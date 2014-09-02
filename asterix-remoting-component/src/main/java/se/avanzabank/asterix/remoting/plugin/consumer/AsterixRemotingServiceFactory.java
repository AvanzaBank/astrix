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

import se.avanzabank.asterix.context.AsterixFactoryBean;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.context.ExternalDependencyAware;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.space.SpaceLocator;

public class AsterixRemotingServiceFactory<T> implements AsterixFactoryBean<T>, ExternalDependencyAware<AsterixRemotingPluginDependencies>, AsterixPluginsAware {
	
	private final Class<T> serviceApi;
	private final String targetSpace;
    private final Class<?> descriptorHolder;
	private AsterixRemotingPluginDependencies dependencies;
	private AsterixPlugins plugins;
	
	public AsterixRemotingServiceFactory(Class<T> serviceApi,
										String targetSpaceName, 
										Class<?> descriptorHolder) {
		this.serviceApi = serviceApi;
		this.targetSpace = targetSpaceName;
		this.descriptorHolder = descriptorHolder;
	}

	@Override
	public T create() {
		AsterixRemotingTransport remotingTransport = createRemotingTransport(); // dependency
		AsterixObjectSerializer objectSerializer = createObjectSerializer(); // plugin
		AsterixFaultTolerancePlugin faultTolerance = createFaultTolerance(); // plugin
		T proxy = AsterixRemotingProxy.create(serviceApi, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(serviceApi, proxy);
		return proxyWithFaultTolerance;
	}

	private AsterixFaultTolerancePlugin createFaultTolerance() {
		return plugins.getPlugin(AsterixFaultTolerancePlugin.class);
	}

	private AsterixObjectSerializer createObjectSerializer() {
		return plugins.getPlugin(AsterixVersioningPlugin.class).create(descriptorHolder);
	}

	private AsterixRemotingTransport createRemotingTransport() {
		SpaceLocator spaceLocator = dependencies.getSpaceLocator();
		 // TODO: caching of created proxies, fault tolerance?
		return AsterixRemotingTransport.remoteSpace(spaceLocator.createClusteredProxy(targetSpace));
	}

	@Override
	public Class<T> getBeanType() {
		return serviceApi;
	}
	
	@Override
	public void setDependency(AsterixRemotingPluginDependencies dependencies) {
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
