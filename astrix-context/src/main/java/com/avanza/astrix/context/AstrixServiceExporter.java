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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import com.avanza.astrix.provider.core.AstrixServiceExport;

/**
 * Server side component used to export all services provided by a given server as defined by
 * an {@link AstrixServiceDescriptor}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceExporter {
	
	private AstrixServiceComponents serviceComponents;
	private AstrixServiceDescriptor serviceDescriptor;
	private final Map<Class<?>, AstrixApiDescriptor> apiDescriptorByProvideService = new ConcurrentHashMap<Class<?>, AstrixApiDescriptor>();
	private AstrixContextImpl astrixContext;
	private final List<Object> serviceProviders = new CopyOnWriteArrayList<>();
	
	public void exportProvidedServices() {
		Collection<AstrixExportedServiceInfo> exportedServices = getExportedServices();
		validateAllPublishedServicesAreProvided(exportedServices);
		for (AstrixExportedServiceInfo exportedService : exportedServices) {
			exportService(exportedService);
		}
	}
	
	private void exportService(AstrixExportedServiceInfo exportedService) {
		AstrixServiceComponent serviceComponent = serviceComponents.getComponent(exportedService.getComponentName());
		exportService(exportedService, serviceComponent, exportedService.getProvidedService());
		if (astrixContext.getInstance(AstrixServiceLookupFactory.class).usesServiceRegistry(exportedService.getApiDescriptor())) {
			AstrixServiceRegistryPlugin serviceRegistryPlugin = astrixContext.getPlugin(AstrixServiceRegistryPlugin.class);
			serviceRegistryPlugin.addProvider(exportedService.getProvidedService(), serviceComponent);
		}
	}
	
	private <T> void exportService(AstrixExportedServiceInfo exportedService, AstrixServiceComponent serviceComponent, Class<T> providedApi) {
		serviceComponent.exportService(providedApi, providedApi.cast(exportedService.getProvider()), exportedService.getApiDescriptor());
	}

	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContextImpl astrixContext) {
		this.astrixContext = astrixContext;
	}

	public void addServiceProvider(Object bean) {
		this.serviceProviders.add(bean);
	}
	
	private AstrixApiDescriptor getApiDescriptor(Class<?> serviceType) {
		AstrixApiDescriptor result = this.apiDescriptorByProvideService.get(serviceType);
		if (result == null) {
			throw new IllegalArgumentException("Service descriptor does not export service. descriptor: " + serviceDescriptor.getClass().getName() + ", service: " + serviceType.getName());
		}
		return result;
	}

	public void setSericeDescriptor(AstrixServiceDescriptor serviceDescriptor) {
		this.serviceDescriptor = serviceDescriptor;		// TODO: How to inject service descriptor??? 
		for (AstrixApiDescriptor apiDescriptor : serviceDescriptor.getApiDescriptors()) {
			for (Class<?> beanType : astrixContext.getExportedBeans(apiDescriptor)) {
				apiDescriptorByProvideService.put(beanType, apiDescriptor);
			}
		}
	}
	
	private void validateAllPublishedServicesAreProvided(Collection<AstrixExportedServiceInfo> exportedServices) {
		Set<Class<?>> providedServices = new HashSet<>();
		for (AstrixExportedServiceInfo exportedServiceInfo : exportedServices) {
			providedServices.add(exportedServiceInfo.getProvidedService());
		}
		Set<Class<?>> publishedServices = new HashSet<>(this.apiDescriptorByProvideService.keySet());
		publishedServices.removeAll(providedServices);
		if (!publishedServices.isEmpty()) {
			throw new IllegalStateException(String.format(
					"Couldn't find service provider(s) (@AstrixServiceExport annotated bean) for all " +
					"services exported by service descriptor. Missing provider(s) for: %s. Verify that " +
					"current application-context defines a bean providing the given service(s) that are annotated with @AstrixServiceExport",
					publishedServices));
		}
	}

	private Collection<AstrixExportedServiceInfo> getExportedServices() {
		Set<AstrixExportedServiceInfo> result = new HashSet<>();
		for (AstrixServiceComponent serviceComponent : this.serviceComponents.getAll()) {
			result.addAll(serviceComponent.getImplicitExportedServices());
		}
		for (Object provider : this.serviceProviders) {
			AstrixServiceExport serviceExport = provider.getClass().getAnnotation(AstrixServiceExport.class);
			for (Class<?> providedServiceType : serviceExport.value()) {
				if (!publishesService(providedServiceType)) {
					continue;
				}
				AstrixApiDescriptor apiDescriptor = getApiDescriptor(providedServiceType);
				result.add(new AstrixExportedServiceInfo(providedServiceType, apiDescriptor, serviceDescriptor.getComponent(), provider));
			}
		}
		return result;
	}
	
	private boolean publishesService(Class<?> providedServiceType) {
		return this.apiDescriptorByProvideService.containsKey(providedServiceType);
	}

}
