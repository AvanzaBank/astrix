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
package se.avanzabank.service.suite.context;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AstrixContext implements Astrix {
	
	private final AstrixPlugins plugins;
	private final AstrixServiceFactoryRegistry serviceFactoryRegistry = new AstrixServiceFactoryRegistry();
	private ConcurrentMap<Class<?>, ExternalDependencyBean> externalDependencyBean = new ConcurrentHashMap<>();
	private List<Object> externalDependencies = new ArrayList<>();
	
	public AstrixContext() {
		this.plugins = new AstrixPlugins(new AstrixPluginInitializer() {
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
	
	public void registerServiceProvider(AstrixServiceProvider serviceProvider) {
		this.serviceFactoryRegistry.registerProvider(serviceProvider);
	}
	
	private void injectDependencies(Object plugin) {
		if (plugin instanceof ExternalDependencyAware) {
			// TODO: use indirection layer to avoid depending on unused external dependencies?
			// As implemented now, if an unused plugin has an external dependency it is still required
			injectExternalDependencies((ExternalDependencyAware<?>)plugin);
		}
		if (plugin instanceof ServiceDependenciesAware) {
			injectServiceDependencies((ServiceDependenciesAware)plugin);
		}
		if (plugin instanceof AstrixPluginsAware) {
			AstrixPluginsAware.class.cast(plugin).setPlugins(getPlugins());
		}
	}
	
	private void injectServiceDependencies(ServiceDependenciesAware serviceDependenciesAware) {
		serviceDependenciesAware.setServiceDependencies(new ServiceDependencies() {
			@Override
			public <T> T getService(Class<T> service) {
				return AstrixContext.this.getService(service);
			}
		});
	}

	private <D extends ExternalDependencyBean> void injectExternalDependencies(ExternalDependencyAware<D> externalDependencyAware) {
		D dependency = getExternalDependency(externalDependencyAware.getDependencyBeanClass());
		externalDependencyAware.setDependency(dependency);
	}

	/**
	 * Looks up a service in the local service registry. <p>
	 * 
	 * @param type
	 * @return
	 */
	public <T> T getService(Class<T> type) {
		// TODO: synchronize creation of service
		// TODO: fix caching of created services
		AstrixServiceFactory<T> serviceFactory = getServiceFactory(type);
		injectDependencies(serviceFactory);
		return serviceFactory.create();
	}
	
	private <T> AstrixServiceFactory<T> getServiceFactory(Class<T> type) {
		return this.serviceFactoryRegistry.getServiceFactory(type);
	}
	
	/**
	 * Returns the external dependencies (as defined by an ExternalDependencyBean) required 
	 * to create a given service, or null if no external dependencies exists. <p>
	 * 
	 * @param serviceType
	 * @return
	 */
	public Class<? extends ExternalDependencyBean> getExternalDependencyBean(Class<?> serviceType) {
		AstrixServiceFactory<?> serviceFactory = getServiceFactory(serviceType);
		if (serviceFactory instanceof ExternalDependencyAware) {
			return ExternalDependencyAware.class.cast(serviceFactory).getDependencyBeanClass();
		}
		return null;
	}
	
	public <T> AstrixServiceProvider getsServiceProvider(Class<T> type) {
		return this.serviceFactoryRegistry.getServiceProvider(type);
	}

	public AstrixPlugins getPlugins() {
		return this.plugins;
	}

	@Override
	public <T> T waitForService(Class<T> class1, long timeoutMillis) {
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
			params[i] = getDependency(parameterTypes[i]); // TODO: introduce externalDependency as separate concept from service
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
	 * Returns the transitive service dependencies required to create a given service, 
	 * ie the services that are used by, or during creation, of a given service.
	 * @param serviceType
	 * @return
	 */
	public List<Class<?>> getTransitiveServiceDependencies(Class<?> serviceType) {
		AstrixServiceFactory<?> serviceFactory = getServiceFactory(serviceType);
		if (serviceFactory instanceof ServiceDependenciesAware) {
			return ServiceDependenciesAware.class.cast(serviceFactory).getServiceDependencies();
		}
		return Collections.emptyList();
	}
	
}
