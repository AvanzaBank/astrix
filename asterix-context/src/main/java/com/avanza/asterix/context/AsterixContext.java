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
package com.avanza.asterix.context;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import javax.annotation.PreDestroy;
/**
 * An AstrixContext is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AsterixContext providers access
 * to different asterix-plugins at runtime and is used as a factory to create asterix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
public class AsterixContext implements Asterix {
	
	private final AsterixPlugins plugins;
	private final AsterixBeanFactoryRegistry beanFactoryRegistry = new AsterixBeanFactoryRegistry();
	private final AsterixEventBus eventBus = AsterixEventBus.create();
	private final AsterixBeanStates beanStates = new AsterixBeanStates();
	private final AsterixSettingsReader settingsReader;
	private final AsterixSettingsWriter settingsWriter;
	private final AsterixBeanStateWorker beanStateWorker;
	private final String currentSubsystem;
	private AsterixApiProviderPlugins apiProviderPlugins;
	private boolean enforeSubsystemBoundaries;
	private final InstanceCache instanceCache = new InstanceCache(new InstanceCache.ObjectInitializer() {
		@Override
		public void init(Object object) {
			injectDependencies(object);
		}
	});
	
	public AsterixContext(AsterixSettings settings, String currentSubsystem) {
		this.currentSubsystem = currentSubsystem;
		this.eventBus.addEventListener(AsterixBeanStateChangedEvent.class, beanStates);
		this.plugins = new AsterixPlugins(new AsterixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
		this.settingsReader = AsterixSettingsReader.create(plugins, settings);
		this.settingsWriter = AsterixSettingsWriter.create(settings);
		this.enforeSubsystemBoundaries = this.settingsReader.getBoolean(AsterixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
		this.beanStateWorker = new AsterixBeanStateWorker(this.settingsReader, eventBus);
		this.beanStateWorker.start();
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		return plugins.getPlugins(type);
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		plugins.registerPlugin(type, provider);
	}
	
	void registerApiProvider(AsterixApiProvider apiProvider) {
		for (Class<?> beanType : apiProvider.providedApis()) {
			AsterixFactoryBean<?> beanFactory = apiProvider.getFactory(beanType);
			registerBeanFactory(beanFactory);
		}
	}

	<T> void registerBeanFactory(AsterixFactoryBean<T> beanFactory) {
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
		injectAsterixClasses(object);
	}

	private void injectAsterixClasses(Object object) {
		for (Method m : object.getClass().getMethods()) {
			if (!m.isAnnotationPresent(AsterixInject.class)) {
				continue;
			}
			if (m.getParameterTypes().length == 0) {
				throw new IllegalArgumentException(String.format("@AsterixInject annotated methods must accept at least one dependency. Class: %s, method: %s"
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
				throw new RuntimeException("Failed to inject dependencies into asterix managed component: " + object.getClass().getName(), e);
			}
		}
	}

	private void injectAwareDependencies(Object object) {
		if (object instanceof AsterixBeanAware) {
			injectBeanDependencies((AsterixBeanAware)object);
		}
		if (object instanceof AsterixPluginsAware) {
			AsterixPluginsAware.class.cast(object).setPlugins(getPlugins());
		}
		if (object instanceof AsterixDecorator) {
			injectDependencies(AsterixDecorator.class.cast(object).getTarget());
		}
		if (object instanceof AsterixEventBusAware) {
			AsterixEventBusAware.class.cast(object).setEventBus(eventBus);
		}
		if (object instanceof AsterixSettingsAware) {
			AsterixSettingsAware.class.cast(object).setSettings(settingsReader);
		}
		if (object instanceof AsterixBeanStateWorkerAware) {
			AsterixBeanStateWorkerAware.class.cast(object).setBeanStateWorker(beanStateWorker);
		}
	}
	
	private void injectBeanDependencies(final AsterixBeanAware beanDependenciesAware) {
		beanDependenciesAware.setAsterixBeans(new AsterixBeans() {
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
					return AsterixContext.this.getBean(beanType, qualifier);
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
		// "just in time" when an asterix-bean is created.
		getTransitiveBeanDependenciesForBean(beanType); 
		return getFactoryBean(beanType).create(qualifier);
	}

	private <T> AsterixFactoryBean<T> getFactoryBean(Class<T> beanType) {
		if (!hasBeanFactoryFor(beanType)) {
			throw new MissingBeanProviderException(beanType);
		}
		AsterixFactoryBean<T> factoryBean = this.beanFactoryRegistry.getFactoryBean(beanType);
		if (isAllowedToInvokeBean(factoryBean)) {
			return factoryBean;
		}
		throw new IllegalSubsystemException(this.currentSubsystem, factoryBean);
	}
	
	private <T> boolean isAllowedToInvokeBean(AsterixFactoryBean<T> factoryBean) {
		if (factoryBean.isLibrary()) {
			return true; 
		}
		if (factoryBean.isVersioned()) {
			return true;
		}
		if (Objects.equals(this.currentSubsystem, factoryBean.getSubsystem())) {
			return true;
		}
		if (!enforeceSubsystemBoundaries()) {
			return true;
		}
		return false;
	}
	
	private boolean enforeceSubsystemBoundaries() {
		return this.enforeSubsystemBoundaries;
	}

	public AsterixPlugins getPlugins() {
		return this.plugins;
	}

	@Override
	public <T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException {
		return waitForBean(beanType, null, timeoutMillis);
	}
	
	@Override
	public <T> T waitForBean(Class<T> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		T result = getBean(beanType, qualifier); // Trigger creation of bean
		waitForBeanAndTransitiveDependenciesToBeBound(AsterixBeanKey.create(beanType, qualifier), timeoutMillis);
		return result;
	}
	
	private void waitForBeanAndTransitiveDependenciesToBeBound(AsterixBeanKey beanKey, long timeoutMillis) throws InterruptedException {
		for (Class<?> beanType : getTransitiveBeanDependenciesForBean(beanKey.getBeanType())) {
			waitForBeanToBeBound(AsterixBeanKey.create(beanType, null), timeoutMillis);
		}
		waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private void waitForBeanToBeBound(AsterixBeanKey beanKey, long timeoutMillis)
			throws InterruptedException {
		if (!isStatefulBean(beanKey)) {
			return;
		}
		this.beanStates.waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private boolean isStatefulBean(AsterixBeanKey beanKey) {
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

		private AsterixFactoryBeanPlugin<?> rootFactory;
		private AsterixFactoryBean<?> root;
		private Set<Class<?>> transitiveDependencies = new HashSet<>();
		
		public TransitiveBeanDependencyResolver(AsterixFactoryBean<?> rootFactory) {
			this.root = rootFactory;			
			this.rootFactory = (AsterixFactoryBeanPlugin<?>) rootFactory.getTarget();
		}

		public Set<Class<?>> resolve() {
			resolveTransitiveDependencies(rootFactory);
			return transitiveDependencies;
		}

		private void resolveTransitiveDependencies(AsterixFactoryBeanPlugin<?> beanFactory) {
			if (beanFactory instanceof AsterixBeanAware) {
				AsterixBeanAware beanAwareFactory = AsterixBeanAware.class.cast(beanFactory);
				for (Class<?> transitiveDependency : beanAwareFactory.getBeanDependencies()) {
					if (this.rootFactory.getBeanType().equals(transitiveDependency)) {
						throw new AsterixCircularDependency(root.getBeanType(), beanFactory.getBeanType());
					}
					transitiveDependencies.add(transitiveDependency);
					try {
						resolveTransitiveDependencies(getFactoryBean(transitiveDependency));
					} catch (MissingBeanProviderException e) {
						throw new MissingBeanDependencyException(beanAwareFactory, transitiveDependency);
					}
				}
			}
			if (beanFactory instanceof AsterixDecorator) {
				AsterixFactoryBeanPlugin<?> decoratedFactory = (AsterixFactoryBeanPlugin<?>)AsterixDecorator.class.cast(beanFactory).getTarget();
				resolveTransitiveDependencies(decoratedFactory);
			}
		}
		
		private void resolveTransitiveDependencies(AsterixFactoryBean<?> beanFactory) {
			resolveTransitiveDependencies(AsterixFactoryBeanPlugin.class.cast(beanFactory.getTarget()));
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

	public List<Class<?>> getExportedBeans(AsterixApiDescriptor apiDescriptor) {
		return apiProviderPlugins.getExportedBeans(apiDescriptor);
	}

	void setApiProviderPlugins(AsterixApiProviderPlugins apiProviderPlugins) {
		this.apiProviderPlugins = apiProviderPlugins;
	}

	public String getCurrentSubsystem() {
		return this.currentSubsystem;
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
		return this.instanceCache.getInstance(classType);
	}
	
	public AsterixSettingsReader getSettings() {
		return settingsReader;
	}

	void set(String settingName, String settingValue) {
		this.settingsWriter.set(settingName, settingValue);
	}
	
}
