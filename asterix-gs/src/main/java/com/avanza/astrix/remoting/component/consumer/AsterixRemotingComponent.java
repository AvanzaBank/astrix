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
package com.avanza.astrix.remoting.component.consumer;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import com.avanza.astrix.context.AsterixApiDescriptor;
import com.avanza.astrix.context.AsterixFaultTolerancePlugin;
import com.avanza.astrix.context.AsterixPlugins;
import com.avanza.astrix.context.AsterixPluginsAware;
import com.avanza.astrix.context.AsterixServiceComponent;
import com.avanza.astrix.context.AsterixServiceExporterBean;
import com.avanza.astrix.context.AsterixServiceProperties;
import com.avanza.astrix.context.AsterixServicePropertiesBuilder;
import com.avanza.astrix.context.AsterixVersioningPlugin;
import com.avanza.astrix.core.AsterixObjectSerializer;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AsterixServiceComponentNames;
import com.avanza.astrix.remoting.client.AsterixRemotingProxy;
import com.avanza.astrix.remoting.client.AsterixRemotingTransport;
import com.avanza.astrix.remoting.component.provider.AsterixRemotingServiceRegistryExporter;
import com.avanza.astrix.remoting.server.AsterixRemotingArgumentSerializerFactory;
import com.avanza.astrix.remoting.server.AsterixRemotingServiceExporterBean;
import com.avanza.astrix.remoting.server.AsterixServiceActivator;

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
