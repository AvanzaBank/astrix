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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import com.avanza.astrix.provider.core.AsterixServiceExport;
/**
 * Registers beans associated with service providers, i.e applications that exports one or many
 * services invokable from remote processes. <p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServerRuntimeBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(AsterixServerRuntimeBuilder.class);
	private final AsterixContext asterixContext;
	private final List<Class<?>> consumedAsterixBeans = new ArrayList<>(1);
	private final Map<Class<?>, AsterixApiDescriptor> apiDescriptorByProvideService;
	private final AsterixServiceDescriptor serviceDescriptor;
	
	public AsterixServerRuntimeBuilder(AsterixContext asterixContext, AsterixServiceDescriptor serviceDescriptor) {
		this.asterixContext = asterixContext;
		this.serviceDescriptor = serviceDescriptor;
		this.apiDescriptorByProvideService = new HashMap<>();
		for (AsterixApiDescriptor apiDescriptor : serviceDescriptor.getApiDescriptors()) {
			for (Class<?> beanType : asterixContext.getExportedBeans(apiDescriptor)) {
				apiDescriptorByProvideService.put(beanType, apiDescriptor);
			}
		}
	}
	
	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws ClassNotFoundException {
		// Register service-descriptor
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceDescriptor.class);
		beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues(){{
			addIndexedArgumentValue(0, serviceDescriptor);
		}});
		beanDefinition.setFactoryMethodName("create");
		registry.registerBeanDefinition("_asterixServiceDescriptor", beanDefinition);
		
		// Register beans required by all used service-components
		final Collection<AsterixExportedServiceInfo> exportedServices = getExportedServices(registry, serviceDescriptor);
		validateAllPublishedServicesAreProvided(exportedServices);
		Collection<AsterixServiceComponent> usedServiceComponents = getUsedServiceComponents(exportedServices);
		for (AsterixServiceComponent serviceComponent : usedServiceComponents) {
			serviceComponent.registerBeans(registry); // No exporting of services, registration of component required beans
			Class<? extends AsterixServiceExporterBean> exporterBean = serviceComponent.getExporterBean();
			if (exporterBean != null) {
				beanDefinition = new AnnotatedGenericBeanDefinition(exporterBean);
				registry.registerBeanDefinition("_asterixServiceExporterBean-" + serviceComponent.getName(), beanDefinition);
			}
		}
		
		Collection<AsterixExportedServiceInfo> publishedOnServiceRegistryServices = new HashSet<>();
		for (AsterixExportedServiceInfo exportedService : exportedServices) {
			if (exportedService.getApiDescriptor().usesServiceRegistry()) {
				publishedOnServiceRegistryServices.add(exportedService);
			}
		}
		if (!publishedOnServiceRegistryServices.isEmpty()) {
			AsterixServiceRegistryPlugin serviceRegistryPlugin = asterixContext.getPlugin(AsterixServiceRegistryPlugin.class);
			serviceRegistryPlugin.registerBeanDefinitions(registry, publishedOnServiceRegistryServices);
			this.consumedAsterixBeans.addAll(serviceRegistryPlugin.getConsumedBeanTypes());
		}
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceExporter.class);
		beanDefinition.setPropertyValues(new MutablePropertyValues() {{
			addPropertyValue("exportedServices", exportedServices);
		}});
		registry.registerBeanDefinition("_asterixServiceExporter", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceExporterBeans.class);
		registry.registerBeanDefinition("_asterixServiceExporterBeans", beanDefinition);
	}

	private void validateAllPublishedServicesAreProvided(Collection<AsterixExportedServiceInfo> exportedServices) {
		Set<Class<?>> providedServices = new HashSet<>();
		for (AsterixExportedServiceInfo exportedServiceInfo : exportedServices) {
			providedServices.add(exportedServiceInfo.getProvidedService());
		}
		Set<Class<?>> publishedServices = new HashSet<>(this.apiDescriptorByProvideService.keySet());
		publishedServices.removeAll(providedServices);
		if (!publishedServices.isEmpty()) {
			throw new IllegalStateException(String.format(
					"Couldn't find service provider(s) (@AsterixServiceExport annotated bean) for all " +
					"services exported by service descriptor. Missing provider(s) for: %s. Verify that " +
					"current application-context defines a bean providing the given service(s) and are annotated with @AsterixServiceExport",
					publishedServices));
		}
	}

	private Set<AsterixServiceComponent> getUsedServiceComponents(Collection<AsterixExportedServiceInfo> exportedServiceInfos) {
		Set<AsterixServiceComponent> result = new HashSet<>();
		for (AsterixExportedServiceInfo serviceInfo : exportedServiceInfos) {
			String componentName = serviceInfo.getComponentName();
			result.add(asterixContext.getInstance(AsterixServiceComponents.class).getComponent(componentName));
		}
		return result;
	}
	
	private Collection<AsterixExportedServiceInfo> getExportedServices(BeanDefinitionRegistry registry, AsterixServiceDescriptor serviceDescriptor) throws ClassNotFoundException {
		Set<AsterixExportedServiceInfo> result = new HashSet<>();
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (beanDefinition.getBeanClassName() == null) {
				log.debug("Can't find className on bean-definition. Won't use bean as a candiate for AsterixServiceExport. beanName={}", beanName);
				continue;
			}
			Class<?> possibleBeanType = Class.forName(beanDefinition.getBeanClassName());
			if (!possibleBeanType.isAnnotationPresent(AsterixServiceExport.class)){
				continue;
			}
			AsterixServiceExport serviceExport = possibleBeanType.getAnnotation(AsterixServiceExport.class);
			for (Class<?> providedServiceType : serviceExport.value()) {
				if (!publishesService(providedServiceType)) {
					continue;
				}
				AsterixApiDescriptor apiDescriptor = getApiDescriptor(providedServiceType);
				result.add(new AsterixExportedServiceInfo(providedServiceType, apiDescriptor, serviceDescriptor.getComponent(), beanName));
			}
		}
		return result;
	}
	
	private AsterixApiDescriptor getApiDescriptor(Class<?> serviceType) {
		AsterixApiDescriptor result = this.apiDescriptorByProvideService.get(serviceType);
		if (result == null) {
			throw new IllegalArgumentException("Service descriptor does not export service. descriptor: " + serviceDescriptor.getClass().getName() + ", service: " + serviceType.getName());
		}
		return result;
	}
	
	private boolean publishesService(Class<?> providedServiceType) {
		return this.apiDescriptorByProvideService.containsKey(providedServiceType);
	}
	
	/**
	 * Asterix beans consumed by the service framework. For instance: The service-registry components
	 * uses the AsterixServiceRegistry bean to publish services to the service registry. <p>
	 * 
	 * @return
	 */
	public Collection<? extends Class<?>> getConsumedAsterixBeans() {
		return this.consumedAsterixBeans;
	}

}
