/*
 * Copyright 2014 Avanza Bank AB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.avanza.astrix.beans.inject;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.core.AstrixStrategy;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixStrategies {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixStrategies.class);
	private final DynamicConfig config;
	private final Map<Class<?>, Object> strategyInstanceByType = new ConcurrentHashMap<>();
	
	public AstrixStrategies(DynamicConfig config, Map<Class<?>, Object> strategyInstanceByStrategyType) {
		this.strategyInstanceByType.putAll(strategyInstanceByStrategyType);
		this.config = config;
	}
	
	public <T> StandardFactoryBean<T> getFactory(Class<T> strategyType) {
		AstrixStrategy strategy = strategyType.getAnnotation(AstrixStrategy.class);
		Object strategyInstance = strategyInstanceByType.get(strategyType);
		if (strategyInstance != null) {
			return new AlreadyInstantiatedFactoryBean<T>(AstrixBeanKey.create(strategyType), strategyType.cast(strategyInstance));
		}
		List<T> providers = AstrixPluginDiscovery.discoverAllPlugins(strategyType);
		if (providers.size() == 1) {
			return new AlreadyInstantiatedFactoryBean<T>(AstrixBeanKey.create(strategyType), providers.get(0));
		}
		if (providers.size() >= 1) {
			Object provider = providers.get(0);
			log.warn(String.format("Multiple strategy providers found. strategyType=%s usedStrategyInstance=%s foundInstances=%s", 
								   strategyType.getName(), provider.getClass().getName(), providers));
			return new AlreadyInstantiatedFactoryBean<T>(AstrixBeanKey.create(strategyType), strategyType.cast(provider));
		}
		Class<? extends T> providerClass = getProviderClass(strategyType, strategy.value());
		return new ClassConstructorFactoryBean<T>(AstrixBeanKey.create(strategyType), providerClass);
	}

	private <T> Class<? extends T> getProviderClass(Class<T> strategy, Class<?> defaultStrategyClass) {
		try {
			String providerClassName = config.getStringProperty(strategy.getName(), null).get();
			if (providerClassName != null) {
				Class<?> providerClass = ReflectionUtil.classForName(providerClassName);
				if (!strategy.isAssignableFrom(providerClass)) {
					throw new IllegalStateException(String.format("Illegal strategy. strategyType=%s strategyImpl=%s", strategy.getName(), providerClass.getName()));
				}
				return (Class<? extends T>) providerClass;
			}
			if (!strategy.isAssignableFrom(defaultStrategyClass)) {
				throw new IllegalStateException(String.format("Illegal strategy. strategyType=%s strategyImpl=%s", strategy.getName(), defaultStrategyClass.getName()));
			}
			
			return (Class<? extends T>) defaultStrategyClass;
		} catch (Exception e) {
			throw new RuntimeException("Failed to load strategy: " + strategy.getName(), e);
		}
	}

}
