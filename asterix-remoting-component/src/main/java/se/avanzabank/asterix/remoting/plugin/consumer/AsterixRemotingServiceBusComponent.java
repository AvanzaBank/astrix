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
import java.util.Collections;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.bus.client.AsterixServiceBusComponent;
import se.avanzabank.asterix.bus.client.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixBeanAware;
import se.avanzabank.asterix.context.AsterixBeans;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.gs.GigaSpaceRegistry;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteApiDescriptor;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.asterix.remoting.plugin.provider.AsterixRemotingServiceBusExporter;

@MetaInfServices(AsterixServiceBusComponent.class)
public class AsterixRemotingServiceBusComponent implements AsterixServiceBusComponent, AsterixBeanAware, AsterixPluginsAware {

	// TODO: what if lookup of service-properties fails? whose responsible of making new attempts to discover provider?
	// Multiple cases exists:
	// 1. No service-bus available (i.e. server instance not running)
	// 2. No service-provider registered in bus (i.e. service bus instance running but no provider registered with bus yet).
	//
	
	private AsterixPlugins plugins;
	private AsterixBeans beans;
	
	@Override
	public <T> T createService(Class<?> descriptorHolder, Class<T> api, AsterixServiceProperties serviceProperties) {
		AsterixObjectSerializer objectSerializer = plugins.getPlugin(AsterixVersioningPlugin.class).create(descriptorHolder);
		AsterixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AsterixFaultTolerancePlugin.class);
		
		// TODO: is GigaSpaceRegistry really a service???
		GigaSpaceRegistry registry = beans.getBean(GigaSpaceRegistry.class); // TODO: behöver den här klassen verkligen känna till space-namnet? Kan inte det abstraheras bort av AsterixServiceContext???
		String targetSpace = serviceProperties.getProperty(AsterixRemotingServiceBusExporter.SPACE_NAME_PROPERTY);
		AsterixRemotingTransport remotingTransport = AsterixRemotingTransport.remoteSpace(registry.lookup(targetSpace)); // TODO: caching of created proxies, fault tolerance?
		
		T proxy = AsterixRemotingProxy.create(api, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(api, proxy);
		return proxyWithFaultTolerance;
	}

	@Override
	public List<Class<?>> getExportedServices(Class<?> possiblyHoldsDescriptor) {
		if (!possiblyHoldsDescriptor.isAnnotationPresent(AsterixRemoteApiDescriptor.class)) {
			return Collections.emptyList();
		}
		AsterixRemoteApiDescriptor remoteApiDescriptor = possiblyHoldsDescriptor.getAnnotation(AsterixRemoteApiDescriptor.class);
		return Arrays.asList(remoteApiDescriptor.exportedApis());
	}

	@Override
	public List<Class<?>> getBeanDependencies() {
		return Arrays.<Class<?>>asList(GigaSpaceRegistry.class);
	}

	@Override
	public void setAsterixBeans(AsterixBeans beans) {
		this.beans = beans;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

}
