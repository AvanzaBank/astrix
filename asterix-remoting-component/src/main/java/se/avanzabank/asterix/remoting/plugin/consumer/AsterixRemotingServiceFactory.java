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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixFactoryBean;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.context.ExternalDependency;
import se.avanzabank.asterix.context.ExternalDependencyAware;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.space.SpaceLocator;

public class AsterixRemotingServiceFactory<T> implements AsterixFactoryBean<T>, ExternalDependencyAware<AsterixRemotingPluginDependencies>, AsterixPluginsAware {
	
	private final Class<T> serviceApi;
	private final String targetSpace;
    private final AsterixApiDescriptor descriptor;
	private ExternalDependency<AsterixRemotingPluginDependencies> dependencies;
	private AsterixPlugins plugins;
	
	private static final Logger log = LoggerFactory.getLogger(AsterixRemotingServiceFactory.class);
	
	public AsterixRemotingServiceFactory(Class<T> serviceApi,
										String targetSpaceName, 
										AsterixApiDescriptor descriptor) {
		this.serviceApi = serviceApi;
		this.targetSpace = targetSpaceName;
		this.descriptor = descriptor; // TODO: inject AsterixApiDescriptor
	}

	@Override
	public T create(String qualifier) {
		log.debug("Creating remote service proxy for {}", serviceApi);
		AsterixRemotingTransport remotingTransport = createRemotingTransport(); // dependency
		AsterixObjectSerializer objectSerializer = createObjectSerializer(); // plugin
		AsterixFaultTolerancePlugin faultTolerance = createFaultTolerance(); // plugin
		T proxy = AsterixRemotingProxy.create(serviceApi, remotingTransport, objectSerializer);
		// TODO really use space name as command group?
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(serviceApi, proxy, targetSpace);
		return proxyWithFaultTolerance;
	}

	private AsterixFaultTolerancePlugin createFaultTolerance() {
		return plugins.getPlugin(AsterixFaultTolerancePlugin.class);
	}

	private AsterixObjectSerializer createObjectSerializer() {
		return plugins.getPlugin(AsterixVersioningPlugin.class).create(descriptor);
	}

	private AsterixRemotingTransport createRemotingTransport() {
		SpaceLocator spaceLocator = dependencies.get().getSpaceLocator();
		 // TODO: caching of created proxies, fault tolerance?
		return AsterixRemotingTransport.remoteSpace(spaceLocator.createClusteredProxy(targetSpace));
	}

	@Override
	public Class<T> getBeanType() {
		return serviceApi;
	}
	
	@Override
	public void setDependency(ExternalDependency<AsterixRemotingPluginDependencies> dependencies) {
		this.dependencies = dependencies;
	}

	@Override
	public Class<AsterixRemotingPluginDependencies> getDependencyBeanClass() {
		return AsterixRemotingPluginDependencies.class;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
}
