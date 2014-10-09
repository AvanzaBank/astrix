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
import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixExportedServiceInfo;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceBuilderHolder;
import se.avanzabank.asterix.context.AsterixServiceRegistryPlugin;
import se.avanzabank.asterix.context.AsterixServiceComponent;
import se.avanzabank.asterix.context.AsterixServiceComponents;

@MetaInfServices(AsterixServiceRegistryPlugin.class)
public class AsterixServiceRegistryBeanRegistryPlugin implements AsterixServiceRegistryPlugin, AsterixPluginsAware {
	
	// TODO: service registry should implement its own interface rather than AsterixServiceApiPlugin
	
	private AsterixPlugins plugins;
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<AsterixExportedServiceInfo> publishedServices) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceRegistryExporterWorker.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_asterixServiceBusExporterWorker", beanDefinition);
		
		Set<AsterixServiceComponent> serviceTransport = new HashSet<>();
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
		for (AsterixServiceComponent asterixServiceTransport : serviceTransport) {
			beanDefinition = new AnnotatedGenericBeanDefinition(asterixServiceTransport.getServiceBuilder());
			registry.registerBeanDefinition("_asterixServiceBuilderBean-" + asterixServiceTransport.getName(), beanDefinition);
		}
	}
	
	private AsterixServiceComponent getTransport(String transportName) {
		return plugins.getPlugin(AsterixServiceComponents.class).getComponent(transportName);
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
}
