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
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AstrixServiceRegistry {
	
	private final InstanceCache instanceCache = new InstanceCache();
	private final ConcurrentMap<Class<?>, AstrixServiceProvider> serviceProviderByProvidedService = new ConcurrentHashMap<>();
	private final Logger log = LoggerFactory.getLogger(getClass());

	public <T> T getService(Class<T> type, AstrixContext context) {
		T instance = instanceCache.get(type);
		if (instance != null) {
			return instance;
		}
		// TODO: synchronize creation of service
		AstrixServiceFactory<T> serviceFactory = getServiceFactory(type);
		if (serviceFactory instanceof ExternalDependencyAware) {
			injectDependencies((ExternalDependencyAware)serviceFactory, context);
		}
		if (serviceFactory instanceof ServiceDependenciesAware) {
			injectDependencies((ServiceDependenciesAware)serviceFactory, context);
		}
		instance = serviceFactory.create(context);
		instanceCache.put(type, instance);
		return instance;
	}
	
	private void injectDependencies(ServiceDependenciesAware serviceDependenciesAware, final AstrixContext context) {
		serviceDependenciesAware.setServiceDependencies(new ServiceDependencies() {
			@Override
			public <T> T getService(Class<T> service) {
				return AstrixServiceRegistry.this.getService(service, context);
			}
		});
	}

	private <D extends ExternalDependencyBean> void injectDependencies(ExternalDependencyAware<D> externalDependencyAware, AstrixContext context) {
		D dependency = context.getExternalDependency(externalDependencyAware.getDependencyBeanClass());
		externalDependencyAware.setDependency(dependency);
	}

	public <T> T waitForService(Class<T> type, long timeoutMillis) {
		throw new UnsupportedOperationException();
	}
	
	public <T> AstrixServiceFactory<T> getServiceFactory(Class<T> type) {
		AstrixServiceProvider serviceProvider = getServiceProvider(type);
		return serviceProvider.getServiceFactory(type);
	}
	
	public <T> AstrixServiceProvider getServiceProvider(Class<T> type) {
		AstrixServiceProvider serviceProvider = this.serviceProviderByProvidedService.get(type);
		if (serviceProvider == null) {
			throw new IllegalStateException(String.format("No providers found for service=%s",
			 		  type.getName())); 
		}
		return serviceProvider;
	}

	public void registerProvider(AstrixServiceProvider serviceProvider) {
		for (Class<?> providedService : serviceProvider.providedServices()) {
			AstrixServiceProvider duplicateProvider = serviceProviderByProvidedService.putIfAbsent(providedService, serviceProvider);
			log.debug("Registering provider. service={} provider={}", providedService.getName(), serviceProvider.getDescriptorHolder().getName());
			if (duplicateProvider != null) {
				throw new IllegalStateException(String.format("Multiple providers discovered for service=%s. %s and %s",
													 		  providedService.getClass().getName(), 
													 		  serviceProvider.getClass().getName(), 
													 		  duplicateProvider.getClass().getName()));
			}
		}
	}

}
