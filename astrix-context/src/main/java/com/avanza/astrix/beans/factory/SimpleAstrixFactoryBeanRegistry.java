/*
 * Copyright 2014-2015 Avanza Bank AB
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
package com.avanza.astrix.beans.factory;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class SimpleAstrixFactoryBeanRegistry implements AstrixFactoryBeanRegistry {
	
	private final ConcurrentMap<AstrixBeanKey<?>, AstrixFactoryBean<?>> factoryByBeanKey = new ConcurrentHashMap<>();

	/* (non-Javadoc)
	 * @see com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry#getFactoryBean(com.avanza.astrix.beans.factory.AstrixBeanKey)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> AstrixFactoryBean<T> getFactoryBean(AstrixBeanKey<T>  beanKey) {
		if (!hasBeanFactoryFor(beanKey)) {
			throw new MissingBeanProviderException(beanKey);
		}
		return (AstrixFactoryBean<T>) this.factoryByBeanKey.get(beanKey);
	}
	
	@Override
	public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
		Set<AstrixBeanKey<T>> result = new HashSet<>();
		for (AstrixBeanKey<?> key : this.factoryByBeanKey.keySet()) {
			if (key.getBeanType().equals(type)) {
				result.add((AstrixBeanKey<T>) key);
			}
		}
		return result;
	}
	
	public <T> void registerFactory(AstrixFactoryBean<T> factory) {
		AstrixFactoryBean<?> duplicateFactory = factoryByBeanKey.putIfAbsent(factory.getBeanKey(), factory);
		if (duplicateFactory != null) {
			throw new MultipleBeanFactoryException(factory.getBeanKey());
		}
	}

	private boolean hasBeanFactoryFor(AstrixBeanKey<? extends Object> beanKey) {
		return this.factoryByBeanKey.containsKey(beanKey);
	}

	@Override
	public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
		return beanKey;
	}
	
}
