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
package se.avanzabank.asterix.context;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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
	private ConcurrentMap<Class<?>, ExternalDependencyBean> externalDependencyBean = new ConcurrentHashMap<>();
	private List<Object> externalDependencies = new ArrayList<>();
	private final AsterixEventBus eventBus = new AsterixEventBus();
	private final AsterixBeanStates beanStates = new AsterixBeanStates();
	
	public AsterixContext() {
		this.eventBus.addEventListener(AsterixBeanStateChangedEvent.class, beanStates);
		this.plugins = new AsterixPlugins(new AsterixPluginInitializer() {
			@Override
			public void init(Object plugin) {
				injectDependencies(plugin);
			}
		});
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		return plugins.getPlugins(type);
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		plugins.registerPlugin(type, provider);
	}
	
	public void registerApiProvider(AsterixApiProvider apiProvider) {
		for (Class<?> beanType : apiProvider.providedApis()) {
			injectDependencies(apiProvider.getFactory(beanType));
		}
		this.beanFactoryRegistry.registerProvider(apiProvider);
	}
	
	private void injectDependencies(Object object) {
		if (object instanceof ExternalDependencyAware) {
			// TODO: use indirection layer to avoid depending on unused external dependencies?
			// As implemented now, if an unused plugin has an external dependency it is still required
			injectExternalDependencies((ExternalDependencyAware<?>)object);
		}
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
	}
	
	private void injectBeanDependencies(final AsterixBeanAware beanDependenciesAware) {
		beanDependenciesAware.setAsterixBeans(new AsterixBeans() {
			@Override
			public <T> T getBean(Class<T> beanType) {
				return getBean(AsterixBeanKey.create(beanType, null));
			}
			
			@Override
			public <T> T getBean(Class<T> beanType, String qualifier) {
				return getBean(AsterixBeanKey.create(beanType, qualifier));
			}
			
			private <T> T getBean(AsterixBeanKey<T> beanKey) {
				if (!beanDependenciesAware.getBeanDependencies().contains(beanKey.getBeanType())) {
					throw new RuntimeException("Undeclared bean dependency: " + beanKey.getBeanType());
				}
				try {
					return AsterixContext.this.getBean(beanKey);
				} catch (MissingBeanException e) {
					throw new MissingBeanDependencyException(beanDependenciesAware, beanKey.getBeanType());
				}
			}
		});
	}

	private boolean hasBeanFactoryFor(Class<?> beanType) {
		return this.beanFactoryRegistry.hasBeanFactoryFor(beanType);
	}

	private <D extends ExternalDependencyBean> void injectExternalDependencies(final ExternalDependencyAware<D> externalDependencyAware) {
		externalDependencyAware.setDependency(new ExternalDependency<D>() {
			@Override
			public D get() {
				return getExternalDependency(externalDependencyAware.getDependencyBeanClass());
			}
		});
	}

	/**
	 * Looks up a bean in the bean registry. <p>
	 * 
	 * @param beanType
	 * @return
	 */
	public <T> T getBean(Class<T> beanType) {
		return getBean(AsterixBeanKey.create(beanType, null));
	}
	
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return getBean(AsterixBeanKey.create(beanType, qualifier));
	}
	
	private <T> T getBean(AsterixBeanKey<T> asterixBeanKey) {
		getTransitiveBeanDependenciesForBean(asterixBeanKey.getBeanType()); // TODO: find cleaner way to detect circular dependencies
		return getFactoryBean(asterixBeanKey.getBeanType()).create(asterixBeanKey.getQualifier());
	}

	private <T> AsterixFactoryBean<T> getFactoryBean(Class<T> beanType) {
		if (!hasBeanFactoryFor(beanType)) {
			throw new MissingBeanException(beanType);
		}
		return this.beanFactoryRegistry.getFactoryBean(beanType);
	}
	
	/**
	 * Returns the external dependencies (as defined by an ExternalDependencyBean) required 
	 * to create a given bean, or null if no external dependencies exists. <p>
	 * 
	 * @param beanType
	 * @return
	 */
	public Class<? extends ExternalDependencyBean> getExternalDependencyBean(Class<?> beanType) {
		AsterixFactoryBean<?> factoryBean = getFactoryBean(beanType);
		return deepGetExternalDependencyBean(factoryBean);
	}
	
	private Class<? extends ExternalDependencyBean> deepGetExternalDependencyBean(Object asterixObject) {
		if (asterixObject instanceof ExternalDependencyAware) {
			return ExternalDependencyAware.class.cast(asterixObject).getDependencyBeanClass();
		}
		if (asterixObject instanceof AsterixDecorator) {
			return deepGetExternalDependencyBean(AsterixDecorator.class.cast(asterixObject).getTarget());
		}
		return null;
	}
	
	public <T> AsterixApiProvider getsApiProvider(Class<T> beanType) {
		return this.beanFactoryRegistry.getApiProvider(beanType);
	}

	public AsterixPlugins getPlugins() {
		return this.plugins;
	}

	@Override
	public void waitForBean(Class<?> beanType, long timeoutMillis) throws InterruptedException {
		waitForBeanToBeBound(AsterixBeanKey.create(beanType, null), timeoutMillis);
	}
	
	@Override
	public void waitForBean(Class<?> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		waitForBeanToBeBound(AsterixBeanKey.create(beanType, qualifier), timeoutMillis);
	}
	
	private void waitForBeanToBeBound(AsterixBeanKey beanKey, long timeoutMillis) throws InterruptedException {
		if (!isStatefulBean(beanKey)) {
			return;
		}
		this.beanStates.waitForBeanToBeBound(beanKey, timeoutMillis);
	}

	private boolean isStatefulBean(AsterixBeanKey beanKey) {
		return this.beanFactoryRegistry.getApiProvider(beanKey.getBeanType()).hasStatefulBeans();
	}

	public void setExternalDependencyBeans(List<ExternalDependencyBean> externalDependencies) {
		for (ExternalDependencyBean dependency : externalDependencies) {
			this.externalDependencyBean.put(dependency.getClass(), dependency);
		}
	}

	public void setExternalDependencies(List<Object> externalDependencies) {
		this.externalDependencies = externalDependencies;
	}
	
	private <T extends ExternalDependencyBean> T getExternalDependency(Class<T> dependencyType) {
		ExternalDependencyBean externalDependencyBean = this.externalDependencyBean.get(dependencyType);
		if (externalDependencyBean == null) {
			return createExternalDependencyBean(dependencyType);
		}
		return dependencyType.cast(externalDependencyBean);
	}
	
	private <D extends ExternalDependencyBean> D createExternalDependencyBean(Class<D> dependencyBeanClass) {
		if (dependencyBeanClass.getConstructors().length != 1) {
			throw new IllegalArgumentException(
					"Dependency bean class must have exactly one constructor. " + dependencyBeanClass);
		}
		Constructor<?> constructor = dependencyBeanClass.getConstructors()[0];
		Class<?>[] parameterTypes = constructor.getParameterTypes();
		Object[] params = new Object[parameterTypes.length];
		for (int i = 0; i < parameterTypes.length; i++) {
			params[i] = getDependency(parameterTypes[i]);
		}
		try {
			D result = dependencyBeanClass.cast(constructor.newInstance(params));
			this.externalDependencyBean.put(dependencyBeanClass, result);
			return result;
		} catch (Exception e) {
			throw new RuntimeException("Failed to create dependencyBean: " + dependencyBeanClass, e);
		}
	}

	private <T> T getDependency(Class<T> type) {
		for (Object dep : this.externalDependencies) {
			if (type.isAssignableFrom(dep.getClass())) {
				return type.cast(dep);
			}
		}
		throw new MissingExternalDependencyException(type);
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

		private AsterixFactoryBean<?> rootFactory;
		private Set<Class<?>> transitiveDependencies = new HashSet<>();
		
		public TransitiveBeanDependencyResolver(AsterixFactoryBean<?> rootFactory) {
			this.rootFactory = rootFactory;
		}

		public Set<Class<?>> resolve() {
			resolveTransitiveDependencies(rootFactory);
			return transitiveDependencies;
		}

		private void resolveTransitiveDependencies(AsterixFactoryBean<?> beanFactory) {
			if (beanFactory instanceof AsterixBeanAware) {
				AsterixBeanAware beanAwareFactory = AsterixBeanAware.class.cast(beanFactory);
				for (Class<?> transitiveDependency : beanAwareFactory.getBeanDependencies()) {
					if (this.rootFactory.getBeanType().equals(transitiveDependency)) {
						throw new AsterixCircularDependency(rootFactory, beanFactory);
					}
					transitiveDependencies.add(transitiveDependency);
					try {
						resolveTransitiveDependencies(getFactoryBean(transitiveDependency));
					} catch (MissingBeanException e) {
						throw new MissingBeanDependencyException(beanAwareFactory, transitiveDependency);
					}
				}
			}
			if (beanFactory instanceof AsterixDecorator) {
				AsterixFactoryBean<?> decoratedFactory = (AsterixFactoryBean<?>)AsterixDecorator.class.cast(beanFactory).getTarget();
				resolveTransitiveDependencies(decoratedFactory);
			}
		}
		
	}
	
}
