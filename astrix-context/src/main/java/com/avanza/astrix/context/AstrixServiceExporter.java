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

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

/**
 * Server side component used to export all services provided by a given server as defined by
 * an {@link AstrixApplicationDescriptor}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceExporter {
	
	private AstrixServiceComponents serviceComponents;
	private AstrixApplicationDescriptor serviceDescriptor;
	private final Collection<AstrixServiceBeanDefinition> serviceBeanDefinitions = new CopyOnWriteArrayList<>();
	private AstrixContextImpl astrixContext;
	private final ConcurrentMap<Class<?>, Object> serviceProviderByType = new ConcurrentHashMap<>();
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContextImpl astrixContext) {
		this.astrixContext = astrixContext;
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

	public void setServiceDescriptor(AstrixApplicationDescriptor serviceDescriptor) {
		this.serviceDescriptor = serviceDescriptor;		// TODO: How to inject service descriptor??? 
		for (AstrixApiDescriptor apiDescriptor : serviceDescriptor.getApiDescriptors()) {
			this.serviceBeanDefinitions.addAll(astrixContext.getExportedServices(apiDescriptor));
		}
	}

	public void exportProvidedServices() {
		for (AstrixServiceBeanDefinition serviceBeanDefintion : serviceBeanDefinitions) {
			ServiceVersioningContext versioningContext = serviceBeanDefintion.getVersioningContext();
			AstrixServiceComponent serviceComponent = getServiceComponent(serviceBeanDefintion);
			Object provider = null;
			if (serviceComponent.requiresProviderInstance()) {
				provider = getProvider(serviceBeanDefintion);
			}
			exportService(serviceBeanDefintion.getBeanType(), provider, versioningContext, serviceComponent);
			if (serviceBeanDefintion.usesServiceRegistry()) {
				AstrixServiceRegistryPlugin serviceRegistryPlugin = astrixContext.getPlugin(AstrixServiceRegistryPlugin.class);
				serviceRegistryPlugin.addProvider(serviceBeanDefintion.getBeanKey(), serviceComponent);
			}
		}
	}

	private Object getProvider(AstrixServiceBeanDefinition serviceBeanDefintion) {
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

	private AstrixServiceComponent getServiceComponent(AstrixServiceBeanDefinition serviceBeanDefinition) {
		if (serviceBeanDefinition.getComponentName() != null) {
			return this.serviceComponents.getComponent(serviceBeanDefinition.getComponentName());
		}
		return this.serviceComponents.getComponent(serviceDescriptor.getComponent());
	}

}
