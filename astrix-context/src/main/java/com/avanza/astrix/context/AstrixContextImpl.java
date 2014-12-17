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
	private final AstrixSettingsWriter settingsWriter;
	private AstrixApiProviderPlugins apiProviderPlugins;
	private final ObjectCache objectCache = new ObjectCache(new ObjectCache.ObjectFactory() {
		@Override
		@SuppressWarnings("unchecked")
		public <T> T create(ObjectId objectId) throws Exception {
			if (objectId.isAstrixBean()) {
				AstrixBeanId beanId = (AstrixBeanId) objectId;
				// Detect circular dependencies by retrieving transitive bean dependencies
				// "just in time" when an astrix-bean is created.
				getTransitiveBeanDependenciesForBean(beanId.getKey().getBeanType());
				return (T) getFactoryBean(beanId.getKey().getBeanType()).create(beanId.getKey().getQualifier());
			}
			T result =  (T) objectId.getType().newInstance();
			injectDependencies(result);
			return result;
		}
	});
	
	
	public AstrixContextImpl(AstrixSettings settings) {
		this.plugins = new AstrixPlugins(new AstrixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
		this.settingsReader = AstrixSettingsReader.create(plugins, settings);
		this.settingsWriter = AstrixSettingsWriter.create(settings);
		getInstance(EventBus.class).addEventListener(AstrixBeanStateChangedEvent.class, beanStates);
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
			public <T> T getBean(Class<T> beanType) {
				return getBean(beanType, null);
			}
			
			@Override
			public <T> T getBean(Class<T> beanType, String qualifier) {
				if (!beanDependenciesAware.getBeanDependencies().contains(AstrixBeanKey.create(beanType, qualifier))) {
					throw new RuntimeException("Undeclared bean dependency: " + beanType);
				}
				try {
					return AstrixContextImpl.this.getBean(beanType, qualifier);
				} catch (MissingBeanProviderException e) {
					throw new MissingBeanDependencyException(beanDependenciesAware, beanType);
				}
			}
			
		});
	}

	private boolean hasBeanFactoryFor(Class<?> beanType) {
		return this.beanFactoryRegistry.hasBeanFactoryFor(beanType);
	}

	@Override
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return objectCache.getInstance(ObjectId.astrixBean(AstrixBeanKey.create(beanType, qualifier)));
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
		for (AstrixBeanKey dependencyKey: getTransitiveBeanDependenciesForBean(beanKey.getBeanType())) {
			waitForBeanToBeBound(dependencyKey, timeoutMillis);
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
		return this.beanFactoryRegistry.getFactoryBean(beanKey.getBeanType()).isStateful();
	}

	/**
	 * Returns the transitive bean dependencies required to create a bean of given type, 
	 * ie the beans that are used by, or during creation, of a given bean.
	 * 
	 * @param beanType
	 * @return
	 */
	public Collection<AstrixBeanKey> getTransitiveBeanDependenciesForBean(Class<?> beanType) {
		return new TransitiveBeanDependencyResolver(getFactoryBean(beanType)).resolve();
	}
	
	private class TransitiveBeanDependencyResolver {

		private AstrixFactoryBeanPlugin<?> rootFactory;
		private AstrixFactoryBean<?> root;
		private Set<AstrixBeanKey> transitiveDependencies = new HashSet<>();
		
		public TransitiveBeanDependencyResolver(AstrixFactoryBean<?> rootFactory) {
			this.root = rootFactory;			
			this.rootFactory = (AstrixFactoryBeanPlugin<?>) rootFactory.getTarget();
		}

		public Set<AstrixBeanKey> resolve() {
			resolveTransitiveDependencies(rootFactory);
			return transitiveDependencies;
		}

		private void resolveTransitiveDependencies(AstrixFactoryBeanPlugin<?> beanFactory) {
			if (beanFactory instanceof AstrixBeanAware) {
				AstrixBeanAware beanAwareFactory = AstrixBeanAware.class.cast(beanFactory);
				for (AstrixBeanKey transitiveDependency : beanAwareFactory.getBeanDependencies()) {
					if (this.rootFactory.getBeanType().equals(transitiveDependency.getBeanType())) {
						throw new AstrixCircularDependency(root.getBeanType(), beanFactory.getBeanType());
					}
					transitiveDependencies.add(transitiveDependency);
					try {
						resolveTransitiveDependencies(getFactoryBean(transitiveDependency.getBeanType()));
					} catch (MissingBeanProviderException e) {
						throw new MissingBeanDependencyException(beanAwareFactory, transitiveDependency.getBeanType());
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

	void set(String settingName, String settingValue) {
		this.settingsWriter.set(settingName, settingValue);
	}

	String getCurrentSubsystem() {
		return this.settingsReader.getString(AstrixSettings.SUBSYSTEM_NAME);
	}

	public void removeSetting(String settingName) {
		this.settingsWriter.remove(settingName);
	}
	
}
