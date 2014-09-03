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

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixBeanRegistryPlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryComponent;

@MetaInfServices(AsterixBeanRegistryPlugin.class)
public class AsterixServiceRegistryBeanRegistryPlugin implements AsterixBeanRegistryPlugin, AsterixPluginsAware {
	
	/*
	 * När man skapar en komponent i tjänste-registret så måste på något sätt följande utföras av komponenten:
	 * 
	 * För att jacka-in sig i AsterixApiProvider:
	 * 	- Ha en mekanism för hur man givet AsterixServiceProperties "binder" sig mot tjänsten
	 * 
	 * För att jacka-in sig i ServiceRegistryExporterWorker
	 *  - Definiera vilken konkret ServiceRegistryExporter som ska användas för att registrera tjänsten i registret. 
	 * 
	 */

	private AsterixPlugins plugins;
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry, AsterixApiDescriptor descriptor) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceRegistryExporterWorker.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_asterixServiceBusExporterWorker", beanDefinition);
	
		// TODO: how to detect what exporters are required in the given context (depending on serviceDescriptor).
		// Only required exporters should be registered.
		// How to handle dependencies for service-exporters?
		Set<Class<? extends ServiceRegistryExporter>> serviceRegistryExporters = getRequiredExporters(descriptor);
		for (Class<? extends ServiceRegistryExporter> exporter : serviceRegistryExporters) {
			beanDefinition = new AnnotatedGenericBeanDefinition(exporter);
			beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
			registry.registerBeanDefinition("_asterixServiceBusExporter-" + exporter.getName(), beanDefinition);
		}
	}

	private Set<Class<? extends ServiceRegistryExporter>> getRequiredExporters(AsterixApiDescriptor descriptor) {
		List<AsterixServiceRegistryComponent> serverComponents = plugins.getPlugins(AsterixServiceRegistryComponent.class);
		Set<Class<? extends ServiceRegistryExporter>> result = new HashSet<>();
		for (AsterixServiceRegistryComponent serverComponent : serverComponents) {
			if (!serverComponent.isActivatedBy(descriptor)) {
				continue;
			}
			result.addAll(serverComponent.getRequiredExporterClasses());
		}
		return result;
	}

	@Override
	public Class<? extends Annotation> getDescriptorType() {
		return AsterixServiceRegistryApi.class;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
}
