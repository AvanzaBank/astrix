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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.provider.core.AsterixServiceExport;

public class AsterixServerRuntimeBuilder {
	
	private AsterixPlugins asterixPlugins;
	
	public AsterixServerRuntimeBuilder(AsterixPlugins asterixPlugins) {
		this.asterixPlugins = asterixPlugins;
	}
	
	public void registerBeanDefinitions(BeanDefinitionRegistry registry,
			final AsterixServiceDescriptor serviceDescriptor) throws ClassNotFoundException {
		// Register service-descriptor
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceDescriptor.class);
		beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues(){{
			addIndexedArgumentValue(0, serviceDescriptor);
		}});
		beanDefinition.setFactoryMethodName("create");
		registry.registerBeanDefinition("_asterixServiceDescriptor", beanDefinition);
		
		// Register beans required by all used service-components
		final Collection<AsterixExportedServiceInfo> exportedServices = getExportedServices(registry, serviceDescriptor);
		Collection<AsterixServiceComponent> usedServiceTransports = getUsedServiceComponents(exportedServices);
		for (AsterixServiceComponent serviceTransport : usedServiceTransports) {
			serviceTransport.registerBeans(registry); // No exporting of services, registration of component required beans
			Class<? extends AsterixServiceExporterBean> exporterBean = serviceTransport.getExporterBean();
			if (exporterBean != null) {
				beanDefinition = new AnnotatedGenericBeanDefinition(exporterBean);
				registry.registerBeanDefinition("_asterixServiceExporterBean-" + serviceTransport.getName(), beanDefinition);
			}
		}
		
		Collection<AsterixExportedServiceInfo> publishedOnServiceRegistryServices = new HashSet<>();
		for (AsterixExportedServiceInfo exportedService : exportedServices) {
			if (exportedService.getApiDescriptor().usesServiceRegistry()) {
				publishedOnServiceRegistryServices.add(exportedService);
			}
		}
		if (!publishedOnServiceRegistryServices.isEmpty()) {
			asterixPlugins.getPlugin(AsterixServiceRegistryPlugin.class).registerBeanDefinitions(registry, publishedOnServiceRegistryServices);
		}
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceExporter.class);
		beanDefinition.setPropertyValues(new MutablePropertyValues() {{
			addPropertyValue("exportedServices", exportedServices);
		}});
		registry.registerBeanDefinition("_asterixServiceExporter", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceExporterBeans.class);
		registry.registerBeanDefinition("_asterixServiceExporterBeans", beanDefinition);
	}

	private Set<AsterixServiceComponent> getUsedServiceComponents(Collection<AsterixExportedServiceInfo> exportedServiceInfos) {
		Set<AsterixServiceComponent> result = new HashSet<>();
		for (AsterixExportedServiceInfo serviceInfo : exportedServiceInfos) {
			String componentName = serviceInfo.getComponentName();
			result.add(asterixPlugins.getPlugin(AsterixServiceComponents.class).getComponent(componentName));
		}
		return result;
	}
	
	private Collection<AsterixExportedServiceInfo> getExportedServices(BeanDefinitionRegistry registry, AsterixServiceDescriptor serviceDescriptor) throws ClassNotFoundException {
		Set<AsterixExportedServiceInfo> result = new HashSet<>();
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			Class<?> possibleBeanType = Class.forName(beanDefinition.getBeanClassName());
			if (!possibleBeanType.isAnnotationPresent(AsterixServiceExport.class)){
				continue;
			}
			AsterixServiceExport serviceExport = possibleBeanType.getAnnotation(AsterixServiceExport.class);
			for (Class<?> providedServiceType : serviceExport.value()) {
				if (!serviceDescriptor.publishesService(providedServiceType)) {
					continue;
				}
				AsterixApiDescriptor apiDescriptor = serviceDescriptor.getApiDescriptor(providedServiceType);
				if (apiDescriptor.usesServiceRegistry()) {
					result.add(new AsterixExportedServiceInfo(providedServiceType, apiDescriptor, serviceDescriptor.getComponent(), beanName));
				} else {
					String componentName = getServiceComponent(apiDescriptor);
					result.add(new AsterixExportedServiceInfo(providedServiceType, apiDescriptor, componentName, beanName));
				}	
			}
		}
		return result;
	}

	private String getServiceComponent(AsterixApiDescriptor apiDescriptor) {
		return this.asterixPlugins.getPlugin(AsterixServiceComponents.class).getComponent(apiDescriptor).getName();
	}

}
