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
package se.avanzabank.service.suite.remoting.plugin.consumer;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.service.suite.bus.client.AstrixServiceBusComponent;
import se.avanzabank.service.suite.bus.client.AstrixServiceProperties;
import se.avanzabank.service.suite.context.AstrixFaultTolerancePlugin;
import se.avanzabank.service.suite.context.AstrixPlugins;
import se.avanzabank.service.suite.context.AstrixPluginsAware;
import se.avanzabank.service.suite.context.AstrixVersioningPlugin;
import se.avanzabank.service.suite.context.ServiceDependencies;
import se.avanzabank.service.suite.context.ServiceDependenciesAware;
import se.avanzabank.service.suite.core.AstrixObjectSerializer;
import se.avanzabank.service.suite.gs.GigaSpaceRegistry;
import se.avanzabank.service.suite.provider.remoting.AstrixRemoteApiDescriptor;
import se.avanzabank.service.suite.remoting.client.AstrixRemotingProxy;
import se.avanzabank.service.suite.remoting.client.AstrixRemotingTransport;
import se.avanzabank.service.suite.remoting.plugin.provider.AstrixRemotingServiceBusExporter;

@MetaInfServices(AstrixServiceBusComponent.class)
public class AstrixRemotingServiceBusComponent implements AstrixServiceBusComponent, ServiceDependenciesAware, AstrixPluginsAware {

	// TODO: what if lookup of service-properties fails? whose responsible of making new attempts to discover provider?
	// Multiple cases exists:
	// 1. No service-bus available (i.e. server instance not running)
	// 2. No service-provider registered in bus (i.e. service bus instance running but no provider registered with bus yet).
	//
	
	private AstrixPlugins plugins;
	private ServiceDependencies services;
	
	@Override
	public <T> T createService(Class<?> descriptorHolder, Class<T> api, AstrixServiceProperties serviceProperties) {
		AstrixObjectSerializer objectSerializer = plugins.getPlugin(AstrixVersioningPlugin.class).create(descriptorHolder);
		AstrixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AstrixFaultTolerancePlugin.class);
		
		// TODO: is GigaSpaceRegistry really a service???
		GigaSpaceRegistry registry = services.getService(GigaSpaceRegistry.class); // TODO: behöver den här klassen verkligen känna till space-namnet? Kan inte det abstraheras bort av AstrixServiceContext???
		String targetSpace = serviceProperties.getProperty(AstrixRemotingServiceBusExporter.SPACE_NAME_PROPERTY);
		AstrixRemotingTransport remotingTransport = AstrixRemotingTransport.remoteSpace(registry.lookup(targetSpace)); // TODO: caching of created proxies, fault tolerance?
		
		T proxy = AstrixRemotingProxy.create(api, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(api, proxy);
		return proxyWithFaultTolerance;
	}

	@Override
	public List<Class<?>> getExportedServices(Class<?> possiblyHoldsDescriptor) {
		if (!possiblyHoldsDescriptor.isAnnotationPresent(AstrixRemoteApiDescriptor.class)) {
			return Collections.emptyList();
		}
		AstrixRemoteApiDescriptor remoteApiDescriptor = possiblyHoldsDescriptor.getAnnotation(AstrixRemoteApiDescriptor.class);
		return Arrays.asList(remoteApiDescriptor.exportedApis());
	}

	@Override
	public List<Class<?>> getServiceDependencies() {
		return Arrays.<Class<?>>asList(GigaSpaceRegistry.class);
	}

	@Override
	public void setServiceDependencies(ServiceDependencies services) {
		this.services = services;
	}

	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.plugins = plugins;
	}

}
