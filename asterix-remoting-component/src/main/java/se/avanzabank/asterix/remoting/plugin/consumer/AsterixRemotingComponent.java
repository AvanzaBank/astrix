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
package se.avanzabank.asterix.remoting.plugin.consumer;

import java.lang.annotation.Annotation;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceBuilder;
import se.avanzabank.asterix.context.AsterixServiceExporterBean;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixServiceTransport;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.gs.GsBinder;
import se.avanzabank.asterix.provider.component.AsterixServiceComponentNames;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteApiDescriptor;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.asterix.remoting.plugin.provider.AsterixRemotingServiceRegistryExporter;
import se.avanzabank.asterix.remoting.server.AsterixRemotingFrameworkBean;
import se.avanzabank.asterix.remoting.server.AsterixRemotingServiceExporterBean;

@MetaInfServices(AsterixServiceTransport.class)
public class AsterixRemotingComponent implements AsterixPluginsAware, AsterixServiceTransport {
	
	private AsterixPlugins plugins;
	
	@Override
	public <T> T createService(AsterixApiDescriptor descriptor, Class<T> api, AsterixServiceProperties serviceProperties) {
		AsterixObjectSerializer objectSerializer = plugins.getPlugin(AsterixVersioningPlugin.class).create(descriptor);
		AsterixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AsterixFaultTolerancePlugin.class);
		
		String targetSpace = serviceProperties.getProperty(AsterixRemotingServiceRegistryExporter.SPACE_NAME_PROPERTY);
		GigaSpace space = GsBinder.createGsFactory(serviceProperties).create();
		AsterixRemotingTransport remotingTransport = AsterixRemotingTransport.remoteSpace(space); // TODO: caching of created proxies, fault tolerance?
		
		T proxy = AsterixRemotingProxy.create(api, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(api, proxy, targetSpace);
		return proxyWithFaultTolerance;
	}
	
	@Override
	public <T> AsterixServiceProperties getServiceProperties(AsterixApiDescriptor apiDescriptor, Class<T> type) {
		String targetSpaceName = apiDescriptor.getAnnotation(AsterixRemoteApiDescriptor.class).targetSpaceName();
		AsterixServiceProperties serviceProperties = new AsterixServiceProperties();
		serviceProperties.setApi(type); // TODO: let invoker set this property?
		serviceProperties.setProperty(AsterixRemotingServiceRegistryExporter.SPACE_NAME_PROPERTY, targetSpaceName);
		return serviceProperties;
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
		new AsterixRemotingFrameworkBean().postProcessBeanDefinitionRegistry(registry);
	}
	
	@Override
	public Class<? extends AsterixServiceExporterBean> getExporterBean() {
		return AsterixRemotingServiceExporterBean.class;
	}
	
	@Override
	public Class<? extends AsterixServiceBuilder> getServiceBuilder() {
		return AsterixRemotingServiceRegistryExporter.class;
	}
	
	@Override
	public Class<? extends Annotation> getServiceDescriptorType() {
		return AsterixRemoteApiDescriptor.class;
	}

}
