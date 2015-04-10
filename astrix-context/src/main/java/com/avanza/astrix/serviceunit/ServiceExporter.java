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
package com.avanza.astrix.serviceunit;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.avanza.astrix.beans.publish.AstrixApiProviderClass;
import com.avanza.astrix.beans.service.AstrixServiceComponent;
import com.avanza.astrix.beans.service.AstrixServiceComponents;
import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

/**
 * Server side component used to export all services provided by a given server as defined by
 * an {@link AstrixApplicationDescriptor}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceExporter {
	
	private AstrixServiceComponents serviceComponents;
	private AstrixApplicationDescriptor applicationDescriptor;
	private final Collection<ServiceBeanDefinition> serviceBeanDefinitions = new CopyOnWriteArrayList<>();
	private final ServiceRegistryExporter serviceRegistryExporter;
	private final ConcurrentMap<Class<?>, Object> serviceProviderByType = new ConcurrentHashMap<>();
	private final ServiceProviderPlugins serviceProviderPlugins;
	
	
	
	public ServiceExporter(AstrixServiceComponents serviceComponents, ServiceRegistryExporter serviceRegistryExporter, ServiceProviderPlugins serviceProviderPlugins) {
		this.serviceComponents = serviceComponents;
		this.serviceRegistryExporter = serviceRegistryExporter;
		this.serviceProviderPlugins = serviceProviderPlugins;
	}

	public void addServiceProvider(Object bean) {
		if (!bean.getClass().isAnnotationPresent(AstrixServiceExport.class)) {
			throw new IllegalArgumentException("Service provider beans must be annotated with @AstrixServiceExport. bean: " + bean.getClass().getName());
		}
		AstrixServiceExport serviceExport = bean.getClass().getAnnotation(AstrixServiceExport.class);
		for (Class<?> providedServiceType : serviceExport.value()) {
			Object previousProvider = serviceProviderByType.putIfAbsent(providedServiceType, bean);
			if (previousProvider != null) {
				throw new IllegalStateException(String.format("Multiple providers for same serviceApi detected. serviceApi=%s beanA=%s beanB=%s",
															  providedServiceType.getName(),
															  previousProvider.getClass().getName(),
															  bean.getClass().getName()));
			}
		}
	}

	public void setServiceDescriptor(AstrixApplicationDescriptor applicationDescriptor) {
		this.applicationDescriptor = applicationDescriptor;		// TODO: How to inject service descriptor??? 
		for (AstrixApiProviderClass api : applicationDescriptor.exportsRemoteServicesFor()) {
			this.serviceBeanDefinitions.addAll(serviceProviderPlugins.getExportedServices(api));
		}
	}

	public void exportProvidedServices() {
		for (ServiceBeanDefinition serviceBeanDefintion : serviceBeanDefinitions) {
			ServiceVersioningContext versioningContext = serviceBeanDefintion.getVersioningContext();
			AstrixServiceComponent serviceComponent = getServiceComponent(serviceBeanDefintion);
			Object provider = null;
			if (serviceComponent.requiresProviderInstance()) {
				provider = getProvider(serviceBeanDefintion);
			}
			exportService(serviceBeanDefintion.getBeanType(), provider, versioningContext, serviceComponent);
			if (serviceBeanDefintion.usesServiceRegistry()) {
				serviceRegistryExporter.addExportedService(serviceBeanDefintion.getBeanKey(), serviceComponent);
			}
		}
	}

	private Object getProvider(ServiceBeanDefinition serviceBeanDefintion) {
		Object provider = serviceProviderByType.get(serviceBeanDefintion.getBeanKey().getBeanType());
		if (provider == null) {
			throw new IllegalStateException(String.format(
					"Couldn't find service provider (@AstrixServiceExport annotated bean) for " +
					"services exported by service descriptor. Missing provider for: %s. Verify that " +
					"current application-context defines a bean providing the given service that is annotated with @AstrixServiceExport",
					serviceBeanDefintion.getBeanType().getName()));
		}
		return provider;
	}
	
	private <T> void exportService(Class<T> providedApi, Object provider, ServiceVersioningContext versioningContext, AstrixServiceComponent serviceComponent) {
		serviceComponent.exportService(providedApi, providedApi.cast(provider), versioningContext);
	}

	private AstrixServiceComponent getServiceComponent(ServiceBeanDefinition serviceBeanDefinition) {
		if (serviceBeanDefinition.getComponentName() != null) {
			return this.serviceComponents.getComponent(serviceBeanDefinition.getComponentName());
		}
		return this.serviceComponents.getComponent(applicationDescriptor.getDefaultServiceComponent());
	}

}
