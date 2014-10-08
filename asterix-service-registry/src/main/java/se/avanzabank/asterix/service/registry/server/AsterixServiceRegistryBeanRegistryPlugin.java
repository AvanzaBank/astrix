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
package se.avanzabank.asterix.service.registry.server;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixExportedServiceInfo;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceBuilderHolder;
import se.avanzabank.asterix.context.AsterixServiceDescriptor;
import se.avanzabank.asterix.context.AsterixServiceRegistryPlugin;
import se.avanzabank.asterix.context.AsterixServiceTransport;
import se.avanzabank.asterix.context.AsterixServiceTransports;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryComponent;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryComponents;

@MetaInfServices(AsterixServiceRegistryPlugin.class)
public class AsterixServiceRegistryBeanRegistryPlugin implements /*AsterixServiceApiPlugin,*/ AsterixServiceRegistryPlugin, AsterixPluginsAware {
	
	// TODO: service registry should implement its own interface rather than AsterixServiceApiPlugin
	
	private final Logger log = LoggerFactory.getLogger(AsterixServiceRegistryBeanRegistryPlugin.class);
	private AsterixPlugins plugins;
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<AsterixExportedServiceInfo> publishedServices) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceRegistryExporterWorker.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_asterixServiceBusExporterWorker", beanDefinition);
		
		Set<AsterixServiceTransport> serviceTransport = new HashSet<>();
		for (final AsterixExportedServiceInfo exportedService : publishedServices) {
			serviceTransport.add(getTransport(exportedService.getTransportName()));
			
			beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceBuilderHolder.class);
			beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues() {{
				addIndexedArgumentValue(0, new RuntimeBeanReference("_asterixServiceBuilderBean-" + exportedService.getTransportName()));
				addIndexedArgumentValue(1, exportedService.getTransportName());
				addIndexedArgumentValue(2, exportedService.getProvidedService());
			}});
			beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
			registry.registerBeanDefinition("_asterixServiceBuilderHolder-" + exportedService.getProvidingBeanName() + "-" + exportedService.getProvidedService().getName(), beanDefinition);
		}
		for (AsterixServiceTransport asterixServiceTransport : serviceTransport) {
			beanDefinition = new AnnotatedGenericBeanDefinition(asterixServiceTransport.getServiceBuilder());
			registry.registerBeanDefinition("_asterixServiceBuilderBean-" + asterixServiceTransport.getName(), beanDefinition);
		}
//		for (final AsterixServiceRegistryComponent component : getActiveComponents(descriptor)) {
			// Register exporter
//			beanDefinition = new AnnotatedGenericBeanDefinition(component.getServiceExporterClass());
//			beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//			registry.registerBeanDefinition("_asterixServiceBusExporter-" + component.getName(), beanDefinition);
//			
//			// Register exporter holder
//			beanDefinition = new AnnotatedGenericBeanDefinition(ServiceRegistryExporterHolder.class);
//			beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues() {{
//				addIndexedArgumentValue(0, new RuntimeBeanReference("_asterixServiceBusExporter-" + component.getName()));
//				addIndexedArgumentValue(1, component.getName());
//			}});
//			registry.registerBeanDefinition("_asterixServiceBusExporterHolder-" + component.getName(), beanDefinition);
//			
//			// TODO: How to ensure beans for component not registered multiple times? Somehow use wrapper that ensure that registerBeans is only processed once
//			// event if component.registerBeans(registry) is invoked multiple times for same component
//			component.registerBeans(registry);
//		}
	}
	
	private AsterixServiceTransport getTransport(String transportName) {
		return plugins.getPlugin(AsterixServiceTransports.class).getTransport(transportName);
	}

//	public void registerServicePropertiesExporter(BeanDefinitionRegistry registry, AsterixServiceTransport serviceTransport) {
//		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(serviceTransport.getServiceBuilder());
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		registry.registerBeanDefinition("_asterixServiceBusExporter-" + component.getName(), beanDefinition);
//		
//		// Register exporter holder
//		beanDefinition = new AnnotatedGenericBeanDefinition(ServiceRegistryExporterHolder.class);
//		beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues() {{
//			addIndexedArgumentValue(0, new RuntimeBeanReference("_asterixServiceBusExporter-" + component.getName()));
//			addIndexedArgumentValue(1, component.getName());
//		}});
//		registry.registerBeanDefinition("_asterixServiceBusExporterHolder-" + component.getName(), beanDefinition);
//	}
	
	
//	@Override
//	public void registerBeanDefinitions(BeanDefinitionRegistry registry) {
//		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceRegistryExporterWorker.class);
//		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
//		registry.registerBeanDefinition("_asterixServiceBusExporterWorker", beanDefinition);
//	}
	
	private Set<AsterixServiceRegistryComponent> getActiveComponents(AsterixServiceDescriptor descriptor) {
		Set<AsterixServiceRegistryComponent> result = new HashSet<>();
		for (AsterixApiDescriptor apiDescriptor : descriptor.getApis(AsterixServiceRegistryApi.class)) {
			for (Class<?> exportedBean : getExportedBeans(apiDescriptor)) {
				AsterixServiceRegistryComponent component = getComponent(exportedBean);
				result.add(component);
				result.addAll(getTransitiveDependencies(component));
			}
		}
		return result;
	}
	
	private AsterixServiceRegistryComponent getComponent(Class<?> exportedBean) {
		/*
		 *  TODO: 
		 *  1. scan bean registry for all AsterixService annotated methods. (once, not in thie method)
		 *  2. find bean that exports given interface (defined by exportedBean)
		 *  3. Check for annotation on that bean that identifies what component to use
		 */
		throw new UnsupportedOperationException("implement me");
	}

	private List<Class<?>> getExportedBeans(AsterixApiDescriptor apiDescriptor) {
		throw new UnsupportedOperationException("implement me");
	}

	private Collection<? extends AsterixServiceRegistryComponent> getTransitiveDependencies(AsterixServiceRegistryComponent component) {
		List<String> componentDepenencies = component.getComponentDepenencies();
		Set<AsterixServiceRegistryComponent> result = new HashSet<>();
		for (String componentDependencyName: componentDepenencies) {
			AsterixServiceRegistryComponent dependency = plugins.getPlugin(AsterixServiceRegistryComponents.class).getComponent(componentDependencyName);
			result.add(dependency);
			result.addAll(getTransitiveDependencies(dependency));
		}
		return result;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
}
