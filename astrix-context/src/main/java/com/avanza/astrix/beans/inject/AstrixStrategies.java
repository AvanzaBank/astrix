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

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.core.util.ReflectionUtil;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixStrategies {
	
	private DynamicConfig config;
	
	public AstrixStrategies(DynamicConfig config) {
		this.config = config;
	}

	public <T> Class<? extends T> getProviderClass(Class<T> strategy, Class<?> defaultStrategy) {
		String providerClassName = config.getStringProperty(strategy.getName(), null).get();
		if (providerClassName != null) {
			Class<?> providerClass = ReflectionUtil.classForName(providerClassName);
			if (!strategy.isAssignableFrom(providerClass)) {
				throw new IllegalStateException(String.format("Illegal strategy. strategyType=%s strategyImpl=%s", strategy.getName(), providerClass.getName()));
			}
			return (Class<? extends T>) providerClass;
		}
		if (!strategy.isAssignableFrom(defaultStrategy)) {
			throw new IllegalStateException(String.format("Illegal strategy. strategyType=%s strategyImpl=%s", strategy.getName(), defaultStrategy.getName()));
		}
		
		return (Class<? extends T>) defaultStrategy;
	}

}
