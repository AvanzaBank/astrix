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
package com.avanza.astrix.service.registry.server;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixServiceComponent;
import com.avanza.astrix.context.AstrixServiceComponents;
import com.avanza.astrix.context.AstrixServicePropertiesBuilderHolder;
import com.avanza.astrix.context.AstrixServiceRegistryPlugin;

@MetaInfServices(AstrixServiceRegistryPlugin.class)
public class AstrixServiceRegistryPluginImpl implements AstrixServiceRegistryPlugin {
	
	private AstrixServiceComponents serviceComponents;
	private AstrixContext astrixContext;
	
//	@Override
//	public void registerBeanDefinitions(BeanDefinitionRegistry registry, Collection<AstrixExportedServiceInfo> publishedServices) throws BeansException {
//		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceRegistryExporterWorker.class);
//		registry.registerBeanDefinition("_AstrixServiceBusExporterWorker", beanDefinition);
//		
//		Set<AstrixServiceComponent> usedServiceComponents = new HashSet<>();
//		for (final AstrixExportedServiceInfo exportedService : publishedServices) {
//			usedServiceComponents.add(getComponent(exportedService.getComponentName()));
//			
//			beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServicePropertiesBuilderHolder.class);
//			beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues() {{
//				addIndexedArgumentValue(0, new RuntimeBeanReference("_AstrixServiceBuilderBean-" + exportedService.getComponentName()));
//				addIndexedArgumentValue(1, exportedService.getComponentName());
//				addIndexedArgumentValue(2, exportedService.getProvidedService());
//			}});
//			registry.registerBeanDefinition("_AstrixServiceBuilderHolder-" + exportedService.getProvidingBeanName() + "-" + exportedService.getProvidedService().getName(), beanDefinition);
//		}
//		for (AstrixServiceComponent serviceComponent : usedServiceComponents) {
//			beanDefinition = new AnnotatedGenericBeanDefinition(serviceComponent.getServiceBuilder());
//			registry.registerBeanDefinition("_AstrixServiceBuilderBean-" + serviceComponent.getName(), beanDefinition);
//		}
//	}
	
	@Override
	public <T> void addProvider(Class<T> beanType, AstrixServiceComponent serviceComponent) {
		AstrixServiceRegistryExporterWorker exporterWorker = astrixContext.getInstance(AstrixServiceRegistryExporterWorker.class);
		exporterWorker.addServiceBuilder(new AstrixServicePropertiesBuilderHolder(serviceComponent, beanType));
	}
	
//	private AstrixServiceComponent getComponent(String componentName) {
//		return serviceComponents.getComponent(componentName);
//	}
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContext astrixContext) {
		this.astrixContext = astrixContext;
	}

	@Override
	public void startPublishServices() {
		astrixContext.getInstance(AstrixServiceRegistryExporterWorker.class).startServiceExporter();
	}
	
//	@Override
//	public Collection<? extends Class<?>> getConsumedBeanTypes() {
//		return Arrays.asList(AstrixServiceRegistryClient.class);
//	}
	
}
