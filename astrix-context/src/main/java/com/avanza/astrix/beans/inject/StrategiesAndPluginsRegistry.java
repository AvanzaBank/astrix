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

import java.util.HashSet;
import java.util.Set;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInjector.AstrixPluginFactoryBean;
import com.avanza.astrix.core.AstrixPlugin;
import com.avanza.astrix.core.AstrixStrategy;

/**
 * Replace with ModuleManager instance
 *
 */
@Deprecated
public class StrategiesAndPluginsRegistry implements AstrixFactoryBeanRegistry {
	
	private final AstrixPlugins plugins;
	private final AstrixStrategies strategies;
	
	public StrategiesAndPluginsRegistry(AstrixPlugins plugins, AstrixStrategies strategies) {
		this.plugins = plugins;
		this.strategies = strategies;
	}

	public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
		if (beanKey.getBeanType().isAnnotationPresent(AstrixStrategy.class) && !beanKey.isQualified()) {
			return strategies.getFactory(beanKey.getBeanType());
		}
		if (beanKey.getBeanType().isAnnotationPresent(AstrixPlugin.class)) {
			return new AstrixPluginFactoryBean<>(beanKey, plugins);
		} 
		// TODO: This is not a strategy or plugin
		return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
	}

	public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
		Set<AstrixBeanKey<T>> result = new HashSet<>();
		if (isPlugin(type)) {
			// Plugin
			for (T plugin : plugins.getPlugins(type)) {
				result.add(AstrixBeanKey.create(type, plugin.getClass().getName()));
			}
			return result;
		}
		return result;
	}

	private <T> boolean isPlugin(Class<T> type) {
		return type.isAnnotationPresent(AstrixPlugin.class);
	}

	@Override
	public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
		return beanKey;
	}
}