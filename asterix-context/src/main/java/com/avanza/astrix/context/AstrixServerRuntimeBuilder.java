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

import com.avanza.astrix.provider.core.AstrixServiceExport;
/**
 * Registers beans associated with service providers, i.e applications that exports one or many
 * services invokable from remote processes. <p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServerRuntimeBuilder {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixServerRuntimeBuilder.class);
	private final AstrixContext AstrixContext;
	private final List<Class<?>> consumedAstrixBeans = new ArrayList<>(1);
	private final Map<Class<?>, AstrixApiDescriptor> apiDescriptorByProvideService;
	private final AstrixServiceDescriptor serviceDescriptor;
	
	public AstrixServerRuntimeBuilder(AstrixContext AstrixContext, AstrixServiceDescriptor serviceDescriptor) {
		this.AstrixContext = AstrixContext;
		this.serviceDescriptor = serviceDescriptor;
		this.apiDescriptorByProvideService = new HashMap<>();
		for (AstrixApiDescriptor apiDescriptor : serviceDescriptor.getApiDescriptors()) {
			for (Class<?> beanType : AstrixContext.getExportedBeans(apiDescriptor)) {
				apiDescriptorByProvideService.put(beanType, apiDescriptor);
			}
		}
	}
	
	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws ClassNotFoundException {
		// Register service-descriptor
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceDescriptor.class);
		beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues(){{
			addIndexedArgumentValue(0, serviceDescriptor);
		}});
		beanDefinition.setFactoryMethodName("create");
		registry.registerBeanDefinition("_AstrixServiceDescriptor", beanDefinition);
		
		// Register beans required by all used service-components
		final Collection<AstrixExportedServiceInfo> exportedServices = getExportedServices(registry, serviceDescriptor);
		validateAllPublishedServicesAreProvided(exportedServices);
		Collection<AstrixServiceComponent> usedServiceComponents = getUsedServiceComponents(exportedServices);
		for (AstrixServiceComponent serviceComponent : usedServiceComponents) {
			serviceComponent.registerBeans(registry); // No exporting of services, registration of component required beans
			Class<? extends AstrixServiceExporterBean> exporterBean = serviceComponent.getExporterBean();
			if (exporterBean != null) {
				beanDefinition = new AnnotatedGenericBeanDefinition(exporterBean);
				registry.registerBeanDefinition("_AstrixServiceExporterBean-" + serviceComponent.getName(), beanDefinition);
			}
		}
		
		Collection<AstrixExportedServiceInfo> publishedOnServiceRegistryServices = new HashSet<>();
		for (AstrixExportedServiceInfo exportedService : exportedServices) {
			if (exportedService.getApiDescriptor().usesServiceRegistry()) {
				publishedOnServiceRegistryServices.add(exportedService);
			}
		}
		if (!publishedOnServiceRegistryServices.isEmpty()) {
			AstrixServiceRegistryPlugin serviceRegistryPlugin = AstrixContext.getPlugin(AstrixServiceRegistryPlugin.class);
			serviceRegistryPlugin.registerBeanDefinitions(registry, publishedOnServiceRegistryServices);
			this.consumedAstrixBeans.addAll(serviceRegistryPlugin.getConsumedBeanTypes());
		}
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceExporter.class);
		beanDefinition.setPropertyValues(new MutablePropertyValues() {{
			addPropertyValue("exportedServices", exportedServices);
		}});
		registry.registerBeanDefinition("_AstrixServiceExporter", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceExporterBeans.class);
		registry.registerBeanDefinition("_AstrixServiceExporterBeans", beanDefinition);
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
					"current application-context defines a bean providing the given service(s) and are annotated with @AstrixServiceExport",
					publishedServices));
		}
	}

	private Set<AstrixServiceComponent> getUsedServiceComponents(Collection<AstrixExportedServiceInfo> exportedServiceInfos) {
		Set<AstrixServiceComponent> result = new HashSet<>();
		for (AstrixExportedServiceInfo serviceInfo : exportedServiceInfos) {
			String componentName = serviceInfo.getComponentName();
			result.add(AstrixContext.getInstance(AstrixServiceComponents.class).getComponent(componentName));
		}
		return result;
	}
	
	private Collection<AstrixExportedServiceInfo> getExportedServices(BeanDefinitionRegistry registry, AstrixServiceDescriptor serviceDescriptor) throws ClassNotFoundException {
		Set<AstrixExportedServiceInfo> result = new HashSet<>();
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (beanDefinition.getBeanClassName() == null) {
				log.debug("Can't find className on bean-definition. Won't use bean as a candiate for AstrixServiceExport. beanName={}", beanName);
				continue;
			}
			Class<?> possibleBeanType = Class.forName(beanDefinition.getBeanClassName());
			if (!possibleBeanType.isAnnotationPresent(AstrixServiceExport.class)){
				continue;
			}
			AstrixServiceExport serviceExport = possibleBeanType.getAnnotation(AstrixServiceExport.class);
			for (Class<?> providedServiceType : serviceExport.value()) {
				if (!publishesService(providedServiceType)) {
					continue;
				}
				AstrixApiDescriptor apiDescriptor = getApiDescriptor(providedServiceType);
				result.add(new AstrixExportedServiceInfo(providedServiceType, apiDescriptor, serviceDescriptor.getComponent(), beanName));
			}
		}
		return result;
	}
	
	private AstrixApiDescriptor getApiDescriptor(Class<?> serviceType) {
		AstrixApiDescriptor result = this.apiDescriptorByProvideService.get(serviceType);
		if (result == null) {
			throw new IllegalArgumentException("Service descriptor does not export service. descriptor: " + serviceDescriptor.getClass().getName() + ", service: " + serviceType.getName());
		}
		return result;
	}
	
	private boolean publishesService(Class<?> providedServiceType) {
		return this.apiDescriptorByProvideService.containsKey(providedServiceType);
	}
	
	/**
	 * Astrix beans consumed by the service framework. For instance: The service-registry components
	 * uses the AstrixServiceRegistry bean to publish services to the service registry. <p>
	 * 
	 * @return
	 */
	public Collection<? extends Class<?>> getConsumedAstrixBeans() {
		return this.consumedAstrixBeans;
	}

}
