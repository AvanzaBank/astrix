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
package se.avanzabank.service.suite.provider.context;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.service.suite.context.AstrixBeanRegistryPlugin;
import se.avanzabank.service.suite.context.AstrixPlugins;
import se.avanzabank.service.suite.context.AstrixPluginsAware;
import se.avanzabank.service.suite.provider.core.AstrixServiceBusApi;

@MetaInfServices(AstrixBeanRegistryPlugin.class)
public class AstrixServiceBusBeanRegistryPlugin implements AstrixBeanRegistryPlugin, AstrixPluginsAware {

	private AstrixPlugins plugins;
	
	@Override
	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceBusExporterWorker.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_astrixServiceBusExporterWorker", beanDefinition);
	
		// TODO: how to detect what exporters are required in the given context (depending on serviceDescriptor).
		// Only required exporters should be registered
		List<Class<? extends ServiceBusExporter>> serviceBusExporters = getRequiredExporters();
		for (Class<? extends ServiceBusExporter> exporter : serviceBusExporters) {
			beanDefinition = new AnnotatedGenericBeanDefinition(exporter);
			beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
			registry.registerBeanDefinition("_astrixServiceBusExporter-" + exporter.getName(), beanDefinition);
		}
	}

	private List<Class<? extends ServiceBusExporter>> getRequiredExporters() {
		return new ArrayList<>();
	}

	@Override
	public Class<? extends Annotation> getDescriptorType() {
		return AstrixServiceBusApi.class;
	}

	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.plugins = plugins;
	}
	
}
