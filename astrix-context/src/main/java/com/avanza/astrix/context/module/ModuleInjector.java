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
package com.avanza.astrix.context.module;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanPostProcessor;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInject;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ModuleInjector {
	
	private InjectingBeanFactoryRegistry beanFactoryRegistry;
	private AstrixBeanFactory beanFactory;
	
	public ModuleInjector(AstrixFactoryBeanRegistry importedBeans) {
		this.beanFactoryRegistry = new InjectingBeanFactoryRegistry(importedBeans);
		this.beanFactory = new AstrixBeanFactory(beanFactoryRegistry);
		this.beanFactory.registerBeanPostProcessor(new AstrixBeanDependencyInjectionBeanPostProcessor());
	}
	
	public void registerBeanPostProcessor(AstrixBeanPostProcessor beanPostProcessor) {
		this.beanFactory.registerBeanPostProcessor(beanPostProcessor);
	}
	
	<T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
		return this.beanFactoryRegistry.getBeansOfType(type);
	}
	
	
	public <T> void bind(AstrixBeanKey<T> beanKey, Class<? extends T> providerType) {
		this.beanFactoryRegistry.bind(beanKey, providerType);
	}

	public <T> void bind(AstrixBeanKey<T> beanKey, T provider) {
		this.beanFactoryRegistry.providerByBeanKey.put(beanKey, new AlreadyInstantiatedFactoryBean<>(beanKey, provider));
	}
	
	public <T> void bind(AstrixBeanKey<T> beanKey, StandardFactoryBean<T> factory) {
		this.beanFactoryRegistry.providerByBeanKey.put(beanKey, factory);
	}
	
	public <T> T getBean(Class<T> type) {
		return beanFactory.getBean(AstrixBeanKey.create(type));
	}
	
	public class InjectingBeanFactoryRegistry implements AstrixFactoryBeanRegistry {
		
		private final ConcurrentMap<AstrixBeanKey<?>, StandardFactoryBean<?>> providerByBeanKey = new ConcurrentHashMap<>();
		private final ConcurrentMap<AstrixBeanKey<?>, AstrixBeanKey<?>> beanBindings = new ConcurrentHashMap<>();
		private final AstrixFactoryBeanRegistry importedBeans;
		
		public InjectingBeanFactoryRegistry(AstrixFactoryBeanRegistry importedBeans) {
			this.importedBeans = importedBeans;
		}

		@Override
		public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
			Class<T> beanType = beanKey.getBeanType();
			if (beanType.isAssignableFrom(ModuleInjector.class)) {
				return new AlreadyInstantiatedFactoryBean<>(beanKey, beanType.cast(ModuleInjector.this));
			}
			StandardFactoryBean<T> factory = (StandardFactoryBean<T>) providerByBeanKey.get(beanKey);
			if (factory != null) {
				return factory;
			}
			return importedBeans.getFactoryBean(beanKey);
			// Check if beanType is abstract
//			throw new MissingBeanProviderException(beanKey);
//			return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
		}

		public <T> void bind(AstrixBeanKey<T> beanKey, Class<? extends T> providerType) {
			this.beanBindings.put(beanKey, AstrixBeanKey.create(providerType));
			this.providerByBeanKey.put(AstrixBeanKey.create(providerType, beanKey.getQualifier()),
									   new ClassConstructorFactoryBean<T>(beanKey, providerType));
		}

		@Override
		public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
			return this.importedBeans.getBeansOfType(type);
//			Set<AstrixBeanKey<T>> result = new HashSet<>();
//			if (providerByBeanKey.containsKey(AstrixBeanKey.create(type))) {
//				result.add(AstrixBeanKey.create(type));
//				return result;
//			}
//			result.add(AstrixBeanKey.create(type));
//			return result;
		}

		@Override
		public <T> AstrixBeanKey<? extends T> resolveBean(AstrixBeanKey<T> beanKey) {
			AstrixBeanKey<?> boundBeanKey = this.beanBindings.get(beanKey);
			if (boundBeanKey != null) {
				return (AstrixBeanKey<? extends T>) boundBeanKey;
			}
			return beanKey;
		}
	}

	private static class AstrixBeanDependencyInjectionBeanPostProcessor implements AstrixBeanPostProcessor {

		@Override
		public void postProcess(Object target, AstrixBeans beans) {
			for (Method m : target.getClass().getMethods()) {
				if (!m.isAnnotationPresent(AstrixInject.class)) {
					continue;
				}
				if (m.getParameterTypes().length == 0) {
					throw new IllegalArgumentException(String.format("@AstrixInject annotated methods must accept at least one dependency. Class: %s, method: %s"
							, target.getClass().getName()
							, m.getName()));
				}
				Object[] deps = new Object[m.getParameterTypes().length];
				for (int argIndex = 0; argIndex < deps.length; argIndex++) {
					Class<?> dep = m.getParameterTypes()[argIndex];
					deps[argIndex] = beans.getBean(AstrixBeanKey.create(dep, null));
				}
				try {
					m.invoke(target, deps);
				} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
					throw new RuntimeException("Failed to inject dependencies into Astrix managed component: " + target.getClass().getName(), e);
				}
			}
		}
	}
	
	public void destroy() {
		this.beanFactory.destroy();
	}

}
