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

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
import com.avanza.astrix.provider.core.AstrixQualifier;
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
				AstrixStrategy strategy = beanKey.getBeanType().getAnnotation(AstrixStrategy.class);
				Class<? extends T> providerClass = strategies.getProviderClass(beanKey.getBeanType(), strategy.value());
				return new AstrixClassConstructorFactoryBean<T>(beanKey, providerClass);
			}
			if (beanKey.getBeanType().isAnnotationPresent(AstrixPlugin.class)) {
				return new AstrixPluginFactoryBean<>(beanKey, plugins);
			} 
			return new AstrixClassConstructorFactoryBean<>(beanKey, beanKey.getBeanType());
		}

		public <T> void bind(Class<T> type, Class<? extends T> providerType) {
			this.beanBindings.put(AstrixBeanKey.create(type), AstrixBeanKey.create(providerType));
			this.providerByBeanKey.put(AstrixBeanKey.create(providerType),
									   new AstrixClassConstructorFactoryBean<T>(AstrixBeanKey.create(type), providerType));
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
	
	private static class AlreadyInstantiatedFactoryBean<T> implements StandardFactoryBean<T> {

		private AstrixBeanKey<T> beanKey;
		private T instance;
		
		public AlreadyInstantiatedFactoryBean(AstrixBeanKey<T> beanKey, T instance) {
			this.beanKey = beanKey;
			this.instance = instance;
		}

		@Override
		public T create(AstrixBeans beans) {
			return instance;
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return beanKey;
		}
		
	}
	
	private static class AstrixClassConstructorFactoryBean<T> implements StandardFactoryBean<T> {
		
		private AstrixBeanKey<T> beanKey;
		private Class<? extends T> beanImplClass;
		
		public AstrixClassConstructorFactoryBean(AstrixBeanKey<T> beanKey, Class<? extends T> factory) {
			this.beanKey = beanKey;
			this.beanImplClass = factory;
		}

		@Override
		public T create(AstrixBeans beans) {
			try {
				return doCreate(beans);
			} catch (NoSuchMethodException | SecurityException e) {
				throw new RuntimeException(e);
			}
		}
		
		private T doCreate(AstrixBeans beans) throws NoSuchMethodException, SecurityException {
			Constructor<T> factory = getFactory();
			List<AstrixBeanKey<?>> beanDependencies = new ArrayList<>(factory.getParameterTypes().length);
			for (int argumentIndex = 0; argumentIndex < factory.getParameterTypes().length; argumentIndex++) {
				Class<?> parameterType = factory.getParameterTypes()[argumentIndex];
				String parameterQualifier = getParameterQualifier(factory, argumentIndex);
				beanDependencies.add(AstrixBeanKey.create(parameterType, parameterQualifier));
			}
			
			Object[] args = new Object[factory.getParameterTypes().length];
			for (int argumentIndex = 0; argumentIndex < factory.getParameterTypes().length; argumentIndex++) {
				AstrixBeanKey<?> dep = beanDependencies.get(argumentIndex);
				Class<?> argumentType = dep.getBeanType();
				String parameterQualifier = dep.getQualifier();
				if (argumentType.isAssignableFrom(List.class)) {
					ParameterizedType genericType = (ParameterizedType) factory.getGenericParameterTypes()[argumentIndex];
					Type actualTypeArguments = genericType.getActualTypeArguments()[0];
					if (actualTypeArguments instanceof Class) {
						args[argumentIndex] = new ArrayList<>(getBeansOfType(beans, (Class) actualTypeArguments));
					} else {
						args[argumentIndex] = new ArrayList<>(getBeansOfType(beans, (Class) ParameterizedType.class.cast(actualTypeArguments).getRawType()));
					}
				} else {
					args[argumentIndex] = beans.getBean(AstrixBeanKey.create(argumentType, parameterQualifier));
				}
			}
			try {
				factory.setAccessible(true);
				return factory.newInstance(args);
			} catch (Exception e) {
				throw new RuntimeException("Failed to instantiate bean using constructor: " + factory.getName(), e);
			}
		}

		private <E> Set<E> getBeansOfType(AstrixBeans beans, Class<E> argumentType) {
			Set<AstrixBeanKey<E>> beansOfType = beans.getBeansOfType(argumentType);
			Set<E> allBeansOfType = new HashSet<>(beansOfType.size());
			for (AstrixBeanKey<E> beanKey : beansOfType) {
				allBeansOfType.add(beans.getBean(beanKey));
			}
			return allBeansOfType;
		}

		private Constructor<T> getFactory() throws NoSuchMethodException {
			Constructor<?>[] constructors = beanImplClass.getConstructors();
			if (constructors.length == 0) {
				throw new IllegalStateException("Couldnt find public constructor on: " + beanImplClass.getName());
			}
			if (constructors.length == 1) {
				return (Constructor<T>) constructors[0];
			}
			for (Constructor<?> constructor : constructors) {
				if (isAnnotationPresent(constructor, AstrixInject.class)) {
					return (Constructor<T>) constructor;
				}
			}
			throw new IllegalStateException("Multiple constructors found on class and no @AstrixInject annotated constructor was found on: "  + beanImplClass.getName());
		}

		private boolean isAnnotationPresent(Constructor<?> constructor, Class<? extends Annotation> annotation) {
			for (Annotation a : constructor.getAnnotations()) {
				if (annotation.isAssignableFrom(a.getClass())) {
					return true;
				}
			}
			return false;
		}

		private String getParameterQualifier(Constructor<T> factory, int argumentIndex) {
			for (Annotation parameterAnnotation : factory.getParameterAnnotations()[argumentIndex]) {
				if (parameterAnnotation instanceof AstrixQualifier) {
					return AstrixQualifier.class.cast(parameterAnnotation).value();
				}
			}
			return null;
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
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
