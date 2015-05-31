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
	
	private final ConcurrentMap<AstrixBeanKey<?>, StandardFactoryBean<?>> factoryByBeanKey = new ConcurrentHashMap<>();
	private final ConcurrentMap<Class<?>, DynamicFactoryBean<?>> dynamicFactoryByBeanType = new ConcurrentHashMap<>();

	
	/* (non-Javadoc)
	 * @see com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry#getFactoryBean(com.avanza.astrix.beans.factory.AstrixBeanKey)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T>  beanKey) {
		StandardFactoryBean<T> factoryBean = (StandardFactoryBean<T>) this.factoryByBeanKey.get(beanKey);
		if (factoryBean != null) {
			return factoryBean;
		}
		DynamicFactoryBean<T> dynamicFactoryBean = (DynamicFactoryBean<T>) this.dynamicFactoryByBeanType.get(beanKey.getBeanType());
		if (dynamicFactoryBean != null) {
			return new SynthesizedFactoryBean<T>(dynamicFactoryBean, beanKey);
		}
		System.out.println(factoryByBeanKey + " " + beanKey);
		throw new MissingBeanProviderException(beanKey);
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
	
	public <T> void registerFactory(FactoryBean<T> factory) {
		if (factory instanceof DynamicFactoryBean) {
			registerFactory((DynamicFactoryBean<T>) factory);
			return;
		}
		if (factory instanceof StandardFactoryBean) {
			registerFactory((StandardFactoryBean<T>) factory);
			return;
		}
		throw new IllegalArgumentException("Unknown FactoryBean type: " + factory);
	}
	
	
	public <T> void registerFactory(StandardFactoryBean<T> factory) {
		StandardFactoryBean<?> duplicateFactory = factoryByBeanKey.putIfAbsent(factory.getBeanKey(), factory);
		if (duplicateFactory != null) {
			throw new MultipleBeanFactoryException(factory.getBeanKey());
		}
	}
	
	public <T> void registerFactory(DynamicFactoryBean<T> dynamicFactoryBean) {
		DynamicFactoryBean<?> duplicateFactory = dynamicFactoryByBeanType.putIfAbsent(dynamicFactoryBean.getType(), dynamicFactoryBean);
		if (duplicateFactory != null) {
			throw new MultipleBeanFactoryException(dynamicFactoryBean.getType());
		}
	}

	@Override
	public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
		return beanKey;
	}
	
	private static class SynthesizedFactoryBean<T> implements StandardFactoryBean<T> {
		private final DynamicFactoryBean<T> factory;
		private final AstrixBeanKey<T> key;
		
		public SynthesizedFactoryBean(DynamicFactoryBean<T> factory,
				AstrixBeanKey<T> key) {
			this.factory = factory;
			this.key = key;
		}
		@Override
		public T create(AstrixBeans beans) {
			return factory.create(key);
		}
		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return key;
		}
	}

	
	
}
