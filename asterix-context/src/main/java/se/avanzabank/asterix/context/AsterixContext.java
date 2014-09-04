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
import java.util.Collections;
import java.util.List;
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
	
	public AsterixContext() {
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
		this.beanFactoryRegistry.registerProvider(apiProvider);
	}
	
	private void injectDependencies(Object plugin) {
		if (plugin instanceof ExternalDependencyAware) {
			// TODO: use indirection layer to avoid depending on unused external dependencies?
			// As implemented now, if an unused plugin has an external dependency it is still required
			injectExternalDependencies((ExternalDependencyAware<?>)plugin);
		}
		if (plugin instanceof AsterixBeanAware) {
			injectBeanDependencies((AsterixBeanAware)plugin);
		}
		if (plugin instanceof AsterixPluginsAware) {
			AsterixPluginsAware.class.cast(plugin).setPlugins(getPlugins());
		}
	}
	
	private void injectBeanDependencies(final AsterixBeanAware beanDependenciesAware) {
		beanDependenciesAware.setAsterixBeans(new AsterixBeans() {
			@Override
			public <T> T getBean(Class<T> beanType) {
				if (!beanDependenciesAware.getBeanDependencies().contains(beanType)) {
					throw new RuntimeException("Undeclared bean dependency: " + beanType);
				}
				return AsterixContext.this.getBean(beanType);
			}
			
			@Override
			public <T> T getBean(Class<T> beanType, String qualifier) {
				if (!beanDependenciesAware.getBeanDependencies().contains(beanType)) {
					throw new RuntimeException("Undeclared bean dependency: " + beanType);
				}
				return AsterixContext.this.getBean(beanType, qualifier);
			}
		});
	}

	private <D extends ExternalDependencyBean> void injectExternalDependencies(ExternalDependencyAware<D> externalDependencyAware) {
		D dependency = getExternalDependency(externalDependencyAware.getDependencyBeanClass());
		externalDependencyAware.setDependency(dependency);
	}

	/**
	 * Looks up a bean in the bean registry. <p>
	 * 
	 * @param beanType
	 * @return
	 */
	public <T> T getBean(Class<T> beanType) {
		// TODO: synchronize creation of bean
		// TODO: fix caching of created bean
		AsterixFactoryBean<T> factory = getFactoryBean(beanType);
		injectDependencies(factory); // TODO: what the place where it makes most sense to inject dependencies to a AsterixFactory?  
		return factory.create(null);
	}
	
	public <T> T getBean(Class<T> beanType, String qualifier) {
		// TODO: synchronize creation of bean
		// TODO: fix caching of created bean
		AsterixFactoryBean<T> factory = getFactoryBean(beanType);
		injectDependencies(factory); // TODO: what the place where it makes most sense to inject dependencies to a AsterixFactory?  
		return factory.create(qualifier);
	}
	
	private <T> AsterixFactoryBean<T> getFactoryBean(Class<T> beanType) {
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
		if (factoryBean instanceof ExternalDependencyAware) {
			return ExternalDependencyAware.class.cast(factoryBean).getDependencyBeanClass();
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
	public <T> T waitForBean(Class<T> class1, long timeoutMillis) {
		throw new UnsupportedOperationException();
	}

	public void setExternalDependencyBeans(List<ExternalDependencyBean> externalDependencies) {
		for (ExternalDependencyBean dependency : externalDependencies) {
			this.externalDependencyBean.put(dependency.getClass(), dependency);
		}
	}

	public void setExternalDependencies(List<Object> externalDependencies) {
		this.externalDependencies = externalDependencies;
	}
	
	public <T extends ExternalDependencyBean> T getExternalDependency(Class<T> dependencyType) {
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
		throw new RuntimeException("Missing dependency: " + type.getName());
	}

	/**
	 * Returns the transitive bean dependencies required to create a bean of given type, 
	 * ie the beans that are used by, or during creation, of a given bean.
	 * 
	 * @param beanType
	 * @return
	 */
	public List<Class<?>> getTransitiveBeanDependencies(Class<?> beanType) {
		AsterixFactoryBean<?> beanFactory = getFactoryBean(beanType);
		if (beanFactory instanceof AsterixBeanAware) {
			return AsterixBeanAware.class.cast(beanFactory).getBeanDependencies();
		}
		return Collections.emptyList();
	}
	
}
