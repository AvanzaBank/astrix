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
package se.avanzabank.asterix.remoting.component.consumer;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceComponent;
import se.avanzabank.asterix.context.AsterixServiceExporterBean;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixServicePropertiesBuilder;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.gs.GsBinder;
import se.avanzabank.asterix.provider.component.AsterixServiceComponentNames;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.asterix.remoting.component.provider.AsterixRemotingServiceRegistryExporter;
import se.avanzabank.asterix.remoting.server.AsterixRemotingArgumentSerializerFactory;
import se.avanzabank.asterix.remoting.server.AsterixRemotingServiceExporterBean;
import se.avanzabank.asterix.remoting.server.AsterixServiceActivator;

@MetaInfServices(AsterixServiceComponent.class)
public class AsterixRemotingComponent implements AsterixPluginsAware, AsterixServiceComponent {
	
	private AsterixPlugins plugins;
	
	@Override
	public <T> T createService(AsterixApiDescriptor descriptor, Class<T> api, AsterixServiceProperties serviceProperties) {
		AsterixObjectSerializer objectSerializer = plugins.getPlugin(AsterixVersioningPlugin.class).create(descriptor);
		AsterixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AsterixFaultTolerancePlugin.class);
		
		String targetSpace = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		GigaSpace space = GsBinder.createGsFactory(serviceProperties).create();
		AsterixRemotingTransport remotingTransport = AsterixRemotingTransport.remoteSpace(space);
		
		T proxy = AsterixRemotingProxy.create(api, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(api, proxy, targetSpace);
		return proxyWithFaultTolerance;
	}
	
	@Override
	public <T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, String serviceUrl) {
		return createService(apiDescriptor, type, GsBinder.createServiceProperties(serviceUrl));
	}
	
	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

	@Override
	public String getName() {
		return AsterixServiceComponentNames.GS_REMOTING;
	}
	
	@Override
	public void registerBeans(BeanDefinitionRegistry registry) {
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixServiceActivator.class);
		registry.registerBeanDefinition("_serviceActivator", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixRemotingArgumentSerializerFactory.class);
		registry.registerBeanDefinition("_asterixRemotingArgumentSerializerFactory", beanDefinition);
	}
	
	@Override
	public Class<? extends AsterixServiceExporterBean> getExporterBean() {
		return AsterixRemotingServiceExporterBean.class;
	}
	
	@Override
	public Class<? extends AsterixServicePropertiesBuilder> getServiceBuilder() {
		return AsterixRemotingServiceRegistryExporter.class;
	}
	
}
