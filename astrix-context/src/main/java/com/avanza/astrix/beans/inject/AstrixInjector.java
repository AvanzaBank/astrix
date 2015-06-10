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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanPostProcessor;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBeanRegistry;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.core.AstrixPlugin;
import com.avanza.astrix.core.AstrixStrategy;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixInjector {
	
	private InjectingBeanFactoryRegistry beanFactoryRegistry;
	private AstrixBeanFactory beanFactory;
	
	public AstrixInjector(AstrixPlugins plugins, AstrixStrategies astrixStrategies) {
		this.beanFactoryRegistry = new InjectingBeanFactoryRegistry(plugins, astrixStrategies);
		this.beanFactory = new AstrixBeanFactory(beanFactoryRegistry);
		this.beanFactory.registerBeanPostProcessor(new AstrixBeanDependencyInjectionBeanPostProcessor());
	}
	
	public void registerBeanPostProcessor(AstrixBeanPostProcessor beanPostProcessor) {
		this.beanFactory.registerBeanPostProcessor(beanPostProcessor);
	}
	
	public <T> void bind(Class<T> type, Class<? extends T> providerType) {
		this.beanFactoryRegistry.bind(type, providerType);
	}

	public <T> void bind(Class<T> type, T provider) {
		AstrixBeanKey<T> beanKey = AstrixBeanKey.create(type);
		this.beanFactoryRegistry.providerByBeanKey.put(beanKey, new AlreadyInstantiatedFactoryBean<>(beanKey, provider));
	}
	
	public <T> T getBean(Class<T> type) {
		return beanFactory.getBean(AstrixBeanKey.create(type));
	}

	public class InjectingBeanFactoryRegistry implements AstrixFactoryBeanRegistry {
		
		private final ConcurrentMap<AstrixBeanKey<?>, StandardFactoryBean<?>> providerByBeanKey = new ConcurrentHashMap<>();
		private final ConcurrentMap<AstrixBeanKey<?>, AstrixBeanKey<?>> beanBindings = new ConcurrentHashMap<>();
		private final AstrixPlugins plugins;
		private final AstrixStrategies strategies;
		
		public InjectingBeanFactoryRegistry(AstrixPlugins plugins, AstrixStrategies strategies) {
			this.plugins = plugins;
			this.strategies = strategies;
		}

		@Override
		public <T> StandardFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanKey) {
			if (beanKey.getBeanType().isAssignableFrom(AstrixInjector.class)) {
				return new AlreadyInstantiatedFactoryBean<>(beanKey, beanKey.getBeanType().cast(AstrixInjector.this));
			}
			StandardFactoryBean<T> factory = (StandardFactoryBean<T>) providerByBeanKey.get(beanKey);
			if (factory != null) {
				return factory;
			}
			if (beanKey.getBeanType().isAnnotationPresent(AstrixStrategy.class) && !beanKey.isQualified()) {
				return strategies.getFactory(beanKey.getBeanType());
			}
			if (beanKey.getBeanType().isAnnotationPresent(AstrixPlugin.class)) {
				return new AstrixPluginFactoryBean<>(beanKey, plugins);
			} 
			return new ClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
		}

		public <T> void bind(Class<T> type, Class<? extends T> providerType) {
			this.beanBindings.put(AstrixBeanKey.create(type), AstrixBeanKey.create(providerType));
			this.providerByBeanKey.put(AstrixBeanKey.create(providerType),
									   new ClassConstructorFactoryBean<T>(AstrixBeanKey.create(type), providerType));
		}

		@Override
		public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
			Set<AstrixBeanKey<T>> result = new HashSet<>();
			if (providerByBeanKey.containsKey(AstrixBeanKey.create(type))) {
				result.add(AstrixBeanKey.create(type));
				return result;
			}
			if (isPlugin(type)) {
				// Plugin
				for (T plugin : plugins.getPlugins(type)) {
					result.add(AstrixBeanKey.create(type, plugin.getClass().getName()));
				}
				return result;
			}
			result.add(AstrixBeanKey.create(type));
			return result;
		}

		private <T> boolean isPlugin(Class<T> type) {
			return type.isAnnotationPresent(AstrixPlugin.class);
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
	
	private static class AstrixPluginFactoryBean<T> implements StandardFactoryBean<T> {
		
		private AstrixBeanKey<T> beanKey;
		private AstrixPlugins plugins;
		
		public AstrixPluginFactoryBean(AstrixBeanKey<T> beanKey, AstrixPlugins plugins) {
			this.beanKey = beanKey;
			this.plugins = plugins;
		}

		@Override
		public T create(AstrixBeans beans) {
			if (beanKey.getQualifier() != null) {
				// Get given instance of plugin
				return plugins.getPluginInstance(beanKey.getBeanType(), getPluginProviderType());
			}
			return plugins.getPlugin(beanKey.getBeanType());
		}

		private Class<? extends T> getPluginProviderType() {
			try {
				return (Class<? extends T>) Class.forName(beanKey.getQualifier());
			} catch (ClassNotFoundException e) {
				throw new RuntimeException(e);
			}
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
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
