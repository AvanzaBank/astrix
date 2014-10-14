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

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixFactoryBeanPlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.context.AsterixServiceComponent;
import se.avanzabank.asterix.context.AsterixServiceComponents;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.ExternalDependency;
import se.avanzabank.asterix.context.ExternalDependencyAware;
import se.avanzabank.asterix.gs.GsBinder;

public class AsterixRemotingServiceFactory<T> implements AsterixFactoryBeanPlugin<T>, ExternalDependencyAware<AsterixRemotingPluginDependencies>, AsterixPluginsAware {
	
	private final Class<T> serviceApi;
	private final String targetSpace;
    private final AsterixApiDescriptor descriptor;
	private ExternalDependency<AsterixRemotingPluginDependencies> dependencies;
	private AsterixPlugins plugins;
	
	public AsterixRemotingServiceFactory(Class<T> serviceApi,
										String targetSpaceName, 
										AsterixApiDescriptor descriptor) {
		this.serviceApi = serviceApi;
		this.targetSpace = targetSpaceName;
		this.descriptor = descriptor; // TODO: inject AsterixApiDescriptor
	}
	
	@Override
	public T create(String qualifier) {
		AsterixServiceComponent serviceComponent = plugins.getPlugin(AsterixServiceComponents.class).getComponent(descriptor);
		AsterixServiceProperties serviceProperties = serviceComponent.getServiceProperties(descriptor, serviceApi);
		serviceProperties.setProperty(GsBinder.SPACE_URL_PROPERTY, dependencies.get().getSpaceLocator().getSpaceUrl(targetSpace));
		return serviceComponent.createService(descriptor, serviceApi, serviceProperties);
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
