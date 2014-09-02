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
package se.avanzabank.asterix.provider.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixBeanRegistryPlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.provider.core.AsterixServiceBusApi;

@MetaInfServices(AsterixBeanRegistryPlugin.class)
public class AsterixServiceBusBeanRegistryPlugin implements AsterixBeanRegistryPlugin, AsterixPluginsAware {

	private AsterixPlugins plugins;
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceBusExporterWorker.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_asterixServiceBusExporterWorker", beanDefinition);
	
		// TODO: how to detect what exporters are required in the given context (depending on serviceDescriptor).
		// Only required exporters should be registered
		List<Class<? extends ServiceBusExporter>> serviceBusExporters = getRequiredExporters();
		for (Class<? extends ServiceBusExporter> exporter : serviceBusExporters) {
			beanDefinition = new AnnotatedGenericBeanDefinition(exporter);
			beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
			registry.registerBeanDefinition("_asterixServiceBusExporter-" + exporter.getName(), beanDefinition);
		}
	}

	private List<Class<? extends ServiceBusExporter>> getRequiredExporters() {
		return new ArrayList<>();
	}

	@Override
	public Class<? extends Annotation> getDescriptorType() {
		return AsterixServiceBusApi.class;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
}
