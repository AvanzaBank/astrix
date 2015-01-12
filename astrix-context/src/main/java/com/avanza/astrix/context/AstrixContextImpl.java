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
package com.avanza.astrix.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.annotation.PreDestroy;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.ObjectId.AstrixBeanId;
/**
 * An AstrixContextImpl is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AstrixContextImpl providers access
 * to different Astrix-plugins at runtime and is used as a factory to create Astrix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
public class AstrixContextImpl implements Astrix, AstrixContext {
	
	private final AstrixPlugins plugins;
	private final AstrixBeanFactoryRegistry beanFactoryRegistry = new AstrixBeanFactoryRegistry();
	private final AstrixBeanStates beanStates = new AstrixBeanStates();
	private final AstrixSettingsReader settingsReader;
	private AstrixApiProviderPlugins apiProviderPlugins;
	private final ObjectCache objectCache = new ObjectCache(new ObjectCache.ObjectFactory() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T create(ObjectId objectId) throws Exception {
			if (objectId.isAstrixBean()) {
				AstrixBeanId beanId = (AstrixBeanId) objectId;
				// Detect circular dependencies by retrieving transitive bean dependencies
				// "just in time" when an astrix-bean is created.
				getTransitiveBeanDependenciesForBean(beanId.getKey());
				return (T) getFactoryBean(beanId.getKey()).create();
			}
			T result =  (T) objectId.getType().newInstance();
			injectDependencies(result);
			return result;
		}
	});
	
	@Deprecated
	public AstrixContextImpl(AstrixSettings settings) {
		this.plugins = new AstrixPlugins(new AstrixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
		this.settingsReader = AstrixSettingsReader.create(plugins, settings);
		getInstance(EventBus.class).addEventListener(AstrixBeanStateChangedEvent.class, beanStates);
	}
	
	public AstrixContextImpl(DynamicConfig dynamicConfig) {
		this.plugins = new AstrixPlugins(new AstrixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
		this.settingsReader = AstrixSettingsReader.create(dynamicConfig);
		getInstance(EventBus.class).addEventListener(AstrixBeanStateChangedEvent.class, beanStates);
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		return plugins.getPlugins(type);
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		plugins.registerPlugin(type, provider);
	}
	
	void registerApiProvider(AstrixApiProvider apiProvider) {
		for (AstrixBeanKey<?> beanType : apiProvider.providedApis()) {
			AstrixFactoryBean<?> beanFactory = apiProvider.getFactory(beanType);
			registerBeanFactory(beanFactory);
		}
	}

	<T> void registerBeanFactory(AstrixFactoryBean<T> beanFactory) {
		injectDependencies(beanFactory);
		this.beanFactoryRegistry.registerFactory(beanFactory);
	}
	
	@Override
	@PreDestroy
	public void destroy() {
		this.objectCache.destroy();
	}
	
	private void injectDependencies(Object object) {
		injectAwareDependencies(object);
		injectAstrixClasses(object);
	}

	private void injectAstrixClasses(Object object) {
		for (Method m : object.getClass().getMethods()) {
			if (!m.isAnnotationPresent(AstrixInject.class)) {
				continue;
			}
			if (m.getParameterTypes().length == 0) {
				throw new IllegalArgumentException(String.format("@AstrixInject annotated methods must accept at least one dependency. Class: %s, method: %s"
						, object.getClass().getName()
						, m.getName()));
			}
			Object[] deps = new Object[m.getParameterTypes().length];
			for (int argIndex = 0; argIndex < deps.length; argIndex++) {
				Class<?> dep = m.getParameterTypes()[argIndex];
				deps[argIndex] = getInstance(dep);
			}
			try {
				m.invoke(object, deps);
			} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
				throw new RuntimeException("Failed to inject dependencies into Astrix managed component: " + object.getClass().getName(), e);
			}
		}
	}

	private void injectAwareDependencies(Object object) {
		if (object instanceof AstrixBeanAware) {
			injectBeanDependencies((AstrixBeanAware)object);
		}
		if (object instanceof AstrixPluginsAware) {
			AstrixPluginsAware.class.cast(object).setPlugins(getPlugins());
		}
		if (object instanceof AstrixDecorator) {
			injectDependencies(AstrixDecorator.class.cast(object).getTarget());
		}
		if (object instanceof AstrixSettingsAware) {
			AstrixSettingsAware.class.cast(object).setSettings(settingsReader);
		}
	}
	
	private void injectBeanDependencies(final AstrixBeanAware beanDependenciesAware) {
		beanDependenciesAware.setAstrixBeans(new AstrixBeans() {
			@Override
			public <T> T getBean(AstrixBeanKey<T> beanKey) {
				if (!beanDependenciesAware.getBeanDependencies().contains(beanKey)) {
					throw new RuntimeException("Undeclared bean dependency: " + beanKey);
				}
				try {
					return AstrixContextImpl.this.getBean(beanKey);
				} catch (MissingBeanProviderException e) {
					throw new MissingBeanDependencyException(beanDependenciesAware, beanKey);
				}
			}
			
		});
	}

	private boolean hasBeanFactoryFor(AstrixBeanKey<? extends Object> beanKey) {
		return this.beanFactoryRegistry.hasBeanFactoryFor(beanKey);
	}

	@Override
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return objectCache.getInstance(ObjectId.astrixBean(AstrixBeanKey.create(beanType, qualifier)));
	}
	
	public <T> T getBean(AstrixBeanKey<T> beanKey) {
		return objectCache.getInstance(ObjectId.astrixBean(beanKey));
	}

	private <T> AstrixFactoryBean<T> getFactoryBean(AstrixBeanKey<T> beanType) {
		if (!hasBeanFactoryFor(beanType)) {
			throw new MissingBeanProviderException(beanType);
		}
		return this.beanFactoryRegistry.getFactoryBean(beanType);
	}
	
	public AstrixPlugins getPlugins() {
		return this.plugins;
	}

	@Override
	public <T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException {
		return waitForBean(beanType, null, timeoutMillis);
	}
	
	@Override
	public <T> T waitForBean(Class<T> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		T result = getBean(beanType, qualifier); // Trigger creation of bean
		waitForBeanAndTransitiveDependenciesToBeBound(AstrixBeanKey.create(beanType, qualifier), timeoutMillis);
		return result;
	}
	
	private void waitForBeanAndTransitiveDependenciesToBeBound(AstrixBeanKey<?> beanKey, long timeoutMillis) throws InterruptedException {
		for (AstrixBeanKey<?> dependencyKey: getTransitiveBeanDependenciesForBean(beanKey)) {
			waitForBeanToBeBound(dependencyKey, timeoutMillis);
		}
		waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private void waitForBeanToBeBound(AstrixBeanKey<?> beanKey, long timeoutMillis)
			throws InterruptedException {
		if (!isStatefulBean(beanKey)) {
			return;
		}
		this.beanStates.waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private boolean isStatefulBean(AstrixBeanKey<?> beanKey) {
		return this.beanFactoryRegistry.getFactoryBean(beanKey).isStateful();
	}

	/**
	 * Returns the transitive bean dependencies required to create a bean of given type, 
	 * ie the beans that are used by, or during creation, of a given bean.
	 * 
	 * @param beanType
	 * @return
	 */
	public Collection<AstrixBeanKey<?>> getTransitiveBeanDependenciesForBean(AstrixBeanKey<? extends Object> beanType) {
		return new TransitiveBeanDependencyResolver(getFactoryBean(beanType)).resolve();
	}
	
	private class TransitiveBeanDependencyResolver {

		private AstrixFactoryBeanPlugin<?> rootFactory;
		private AstrixFactoryBean<?> root;
		private Set<AstrixBeanKey<?>> transitiveDependencies = new HashSet<>();
		
		public TransitiveBeanDependencyResolver(AstrixFactoryBean<?> rootFactory) {
			this.root = rootFactory;			
			this.rootFactory = (AstrixFactoryBeanPlugin<?>) rootFactory.getTarget();
		}

		public Set<AstrixBeanKey<?>> resolve() {
			resolveTransitiveDependencies(rootFactory);
			return transitiveDependencies;
		}

		private void resolveTransitiveDependencies(AstrixFactoryBeanPlugin<?> beanFactory) {
			if (beanFactory instanceof AstrixBeanAware) {
				AstrixBeanAware beanAwareFactory = AstrixBeanAware.class.cast(beanFactory);
				for (AstrixBeanKey<?> transitiveDependency : beanAwareFactory.getBeanDependencies()) {
					if (this.rootFactory.getBeanKey().equals(transitiveDependency)) {
						throw new AstrixCircularDependency(root.getBeanType(), beanFactory.getBeanKey().getBeanType());
					}
					transitiveDependencies.add(transitiveDependency);
					try {
						resolveTransitiveDependencies(getFactoryBean(transitiveDependency));
					} catch (MissingBeanProviderException e) {
						throw new MissingBeanDependencyException(beanAwareFactory, transitiveDependency);
					}
				}
			}
			if (beanFactory instanceof AstrixDecorator) {
				AstrixFactoryBeanPlugin<?> decoratedFactory = (AstrixFactoryBeanPlugin<?>)AstrixDecorator.class.cast(beanFactory).getTarget();
				resolveTransitiveDependencies(decoratedFactory);
			}
		}
		
		private void resolveTransitiveDependencies(AstrixFactoryBean<?> beanFactory) {
			resolveTransitiveDependencies(AstrixFactoryBeanPlugin.class.cast(beanFactory.getTarget()));
		}
		
	}

	List<AstrixServiceBeanDefinition> getExportedServices(AstrixApiDescriptor apiDescriptor) {
		return apiProviderPlugins.getExportedServices(apiDescriptor);
	}

	void setApiProviderPlugins(AstrixApiProviderPlugins apiProviderPlugins) {
		this.apiProviderPlugins = apiProviderPlugins;
	}

	public <T> T getPlugin(Class<T> pluginType) {
		return getPlugins().getPlugin(pluginType);
	}
	
	/**
	 * Returns an instance of an internal framework class.
	 * @param classType
	 * @return
	 */
	public final <T> T getInstance(Class<T> classType) {
		// We allow injecting an instance of ObjectCache, hence this
		// check.
		if (classType.equals(ObjectCache.class)) {
			return classType.cast(objectCache);
		}
		if (classType.equals(AstrixContextImpl.class)) {
			return classType.cast(this);
		}
		if (classType.equals(AstrixApiProviderPlugins.class)) {
			return classType.cast(this.apiProviderPlugins);
		}
		return this.objectCache.getInstance(ObjectId.internalClass(classType));
	}
	
	public AstrixSettingsReader getSettings() {
		return settingsReader;
	}

	String getCurrentSubsystem() {
		return this.settingsReader.getString(AstrixSettings.SUBSYSTEM_NAME);
	}

}
