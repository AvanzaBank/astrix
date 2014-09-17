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

import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixBeanAware;
import se.avanzabank.asterix.context.AsterixBeans;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.provider.component.AsterixServiceRegistryComponentNames;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.asterix.remoting.plugin.provider.AsterixRemotingBeanRegistryPlugin;
import se.avanzabank.asterix.remoting.plugin.provider.AsterixRemotingServiceRegistryExporter;
import se.avanzabank.asterix.service.registry.client.AsterixServiceProperties;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryComponent;
import se.avanzabank.asterix.service.registry.server.ServiceRegistryExporter;

@MetaInfServices(AsterixServiceRegistryComponent.class)
public class AsterixRemotingComponent implements AsterixServiceRegistryComponent, AsterixBeanAware, AsterixPluginsAware {
	
	private AsterixPlugins plugins;
	private AsterixBeans beans;
	
	@Override
	public <T> T createService(AsterixApiDescriptor descriptor, Class<T> api, AsterixServiceProperties serviceProperties) {
		AsterixObjectSerializer objectSerializer = plugins.getPlugin(AsterixVersioningPlugin.class).create(descriptor);
		AsterixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AsterixFaultTolerancePlugin.class);
		
		String targetSpace = serviceProperties.getProperty(AsterixRemotingServiceRegistryExporter.SPACE_NAME_PROPERTY);
		GigaSpace space = beans.getBean(GigaSpace.class, targetSpace);
		AsterixRemotingTransport remotingTransport = AsterixRemotingTransport.remoteSpace(space); // TODO: caching of created proxies, fault tolerance?
		
		T proxy = AsterixRemotingProxy.create(api, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(api, proxy, targetSpace);
		return proxyWithFaultTolerance;
	}

	@Override
	public List<Class<?>> getBeanDependencies() {
		return Arrays.<Class<?>>asList(GigaSpace.class);
	}
	
	@Override
	public void setAsterixBeans(AsterixBeans beans) {
		this.beans = beans;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

	@Override
	public Class<? extends ServiceRegistryExporter> getServiceExporterClass() {
		return AsterixRemotingServiceRegistryExporter.class;
	}
	
	@Override
	public List<String> getComponentDepenencies() {
		return Arrays.asList(AsterixServiceRegistryComponentNames.GS);
	}

	@Override
	public String getName() {
		return AsterixServiceRegistryComponentNames.GS_REMOTING;
	}
	
	@Override
	public void registerBeans(BeanDefinitionRegistry registry) {
		new AsterixRemotingBeanRegistryPlugin().registerBeanDefinitions(registry, null);
	}

}
