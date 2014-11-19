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
/**
 * An AstrixContext is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AstrixContext providers access
 * to different Astrix-plugins at runtime and is used as a factory to create Astrix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
public class AstrixContext implements Astrix {
	
	private final AstrixPlugins plugins;
	private final AstrixBeanFactoryRegistry beanFactoryRegistry = new AstrixBeanFactoryRegistry();
	private final AstrixEventBus eventBus = AstrixEventBus.create();
	private final AstrixBeanStates beanStates = new AstrixBeanStates();
	private final AstrixSettingsReader settingsReader;
	private final AstrixSettingsWriter settingsWriter;
	private final AstrixBeanStateWorker beanStateWorker;
	private AstrixApiProviderPlugins apiProviderPlugins;
	private final InstanceCache instanceCache = new InstanceCache(new InstanceCache.ObjectInitializer() {
		@Override
		public void init(Object object) {
			injectDependencies(object);
		}
	});
	
	public AstrixContext(AstrixSettings settings) {
		this.eventBus.addEventListener(AstrixBeanStateChangedEvent.class, beanStates);
		this.plugins = new AstrixPlugins(new AstrixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
		this.settingsReader = AstrixSettingsReader.create(plugins, settings);
		this.settingsWriter = AstrixSettingsWriter.create(settings);
		this.beanStateWorker = new AstrixBeanStateWorker(this.settingsReader, eventBus);
		this.beanStateWorker.start();
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		return plugins.getPlugins(type);
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		plugins.registerPlugin(type, provider);
	}
	
	void registerApiProvider(AstrixApiProvider apiProvider) {
		for (Class<?> beanType : apiProvider.providedApis()) {
			AstrixFactoryBean<?> beanFactory = apiProvider.getFactory(beanType);
			registerBeanFactory(beanFactory);
		}
	}

	<T> void registerBeanFactory(AstrixFactoryBean<T> beanFactory) {
		injectDependencies(beanFactory);
		this.beanFactoryRegistry.registerFactory(beanFactory);
	}
	
	@PreDestroy
	public void destroy() {
		this.beanStateWorker.interrupt();
		this.eventBus.destroy();
		this.instanceCache.destroy();
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
		if (object instanceof AstrixEventBusAware) {
			AstrixEventBusAware.class.cast(object).setEventBus(eventBus);
		}
		if (object instanceof AstrixSettingsAware) {
			AstrixSettingsAware.class.cast(object).setSettings(settingsReader);
		}
		if (object instanceof AstrixBeanStateWorkerAware) {
			AstrixBeanStateWorkerAware.class.cast(object).setBeanStateWorker(beanStateWorker);
		}
	}
	
	private void injectBeanDependencies(final AstrixBeanAware beanDependenciesAware) {
		beanDependenciesAware.setAstrixBeans(new AstrixBeans() {
			@Override
			public <T> T getBean(Class<T> beanType) {
				return getBean(beanType, null);
			}
			
			@Override
			public <T> T getBean(Class<T> beanType, String qualifier) {
				if (!beanDependenciesAware.getBeanDependencies().contains(beanType)) {
					throw new RuntimeException("Undeclared bean dependency: " + beanType);
				}
				try {
					return AstrixContext.this.getBean(beanType, qualifier);
				} catch (MissingBeanProviderException e) {
					throw new MissingBeanDependencyException(beanDependenciesAware, beanType);
				}
			}
			
		});
	}

	private boolean hasBeanFactoryFor(Class<?> beanType) {
		return this.beanFactoryRegistry.hasBeanFactoryFor(beanType);
	}

	/**
	 * Looks up a bean in the bean registry. <p>
	 * 
	 * @param beanType
	 * @return
	 */
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	public <T> T getBean(Class<T> beanType, String qualifier) {
		// Detect circular dependencies by retrieving transitive bean dependencies
		// "just in time" when an Astrix-bean is created.
		getTransitiveBeanDependenciesForBean(beanType); 
		return getFactoryBean(beanType).create(qualifier);
	}

	private <T> AstrixFactoryBean<T> getFactoryBean(Class<T> beanType) {
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
	
	private void waitForBeanAndTransitiveDependenciesToBeBound(AstrixBeanKey beanKey, long timeoutMillis) throws InterruptedException {
		for (Class<?> beanType : getTransitiveBeanDependenciesForBean(beanKey.getBeanType())) {
			waitForBeanToBeBound(AstrixBeanKey.create(beanType, null), timeoutMillis);
		}
		waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private void waitForBeanToBeBound(AstrixBeanKey beanKey, long timeoutMillis)
			throws InterruptedException {
		if (!isStatefulBean(beanKey)) {
			return;
		}
		this.beanStates.waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private boolean isStatefulBean(AstrixBeanKey beanKey) {
		return !this.beanFactoryRegistry.getFactoryBean(beanKey.getBeanType()).isLibrary();
	}

	/**
	 * Returns the transitive bean dependencies required to create a bean of given type, 
	 * ie the beans that are used by, or during creation, of a given bean.
	 * 
	 * @param beanType
	 * @return
	 */
	public Collection<Class<?>> getTransitiveBeanDependenciesForBean(Class<?> beanType) {
		return new TransitiveBeanDependencyResolver(getFactoryBean(beanType)).resolve();
	}
	
	private class TransitiveBeanDependencyResolver {

		private AstrixFactoryBeanPlugin<?> rootFactory;
		private AstrixFactoryBean<?> root;
		private Set<Class<?>> transitiveDependencies = new HashSet<>();
		
		public TransitiveBeanDependencyResolver(AstrixFactoryBean<?> rootFactory) {
			this.root = rootFactory;			
			this.rootFactory = (AstrixFactoryBeanPlugin<?>) rootFactory.getTarget();
		}

		public Set<Class<?>> resolve() {
			resolveTransitiveDependencies(rootFactory);
			return transitiveDependencies;
		}

		private void resolveTransitiveDependencies(AstrixFactoryBeanPlugin<?> beanFactory) {
			if (beanFactory instanceof AstrixBeanAware) {
				AstrixBeanAware beanAwareFactory = AstrixBeanAware.class.cast(beanFactory);
				for (Class<?> transitiveDependency : beanAwareFactory.getBeanDependencies()) {
					if (this.rootFactory.getBeanType().equals(transitiveDependency)) {
						throw new AstrixCircularDependency(root.getBeanType(), beanFactory.getBeanType());
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

	public <T> T getPluginInstance(Class<T> providerType) {
		for (Class<?> pluginTypeCandidate : providerType.getInterfaces()) {
			for (Object pluginProvider : this.plugins.getPlugins(pluginTypeCandidate)) {
				if (pluginProvider.getClass().equals(providerType)) {
					return providerType.cast(pluginProvider);
				}
			}
		}
		throw new IllegalArgumentException("Plugin provider not found: " + providerType.getName());
	}

	public List<Class<?>> getExportedBeans(AstrixApiDescriptor apiDescriptor) {
		return apiProviderPlugins.getExportedBeans(apiDescriptor);
	}

	void setApiProviderPlugins(AstrixApiProviderPlugins apiProviderPlugins) {
		this.apiProviderPlugins = apiProviderPlugins;
	}

	public <T> T getPlugin(Class<T> pluginType) {
		return getPlugins().getPlugin(pluginType);
	}
	
	public <T> T getInstance(Class<T> classType) {
		// We allow injecting an instance of InstanceCache, hence this
		// check.
		if (classType.equals(InstanceCache.class)) {
			return classType.cast(instanceCache);
		}
		if (classType.equals(AstrixContext.class)) {
			return classType.cast(this);
		}
		return this.instanceCache.getInstance(classType);
	}
	
	public AstrixSettingsReader getSettings() {
		return settingsReader;
	}

	void set(String settingName, String settingValue) {
		this.settingsWriter.set(settingName, settingValue);
	}

	String getCurrentSubsystem() {
		return this.settingsReader.getString(AstrixSettings.SUBSYSTEM_NAME);
	}
	
}
