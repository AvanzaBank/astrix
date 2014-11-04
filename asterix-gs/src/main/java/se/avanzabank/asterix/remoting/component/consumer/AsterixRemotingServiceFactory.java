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

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixFactoryBeanPlugin;
import se.avanzabank.asterix.context.AsterixInject;
import se.avanzabank.asterix.context.AsterixServiceComponent;
import se.avanzabank.asterix.context.AsterixServiceComponents;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.ExternalDependency;
import se.avanzabank.asterix.context.ExternalDependencyAware;
import se.avanzabank.asterix.gs.GsBinder;
import se.avanzabank.asterix.provider.remoting.AsterixRemoteApiDescriptor;
import se.avanzabank.asterix.remoting.component.provider.AsterixRemotingServiceRegistryExporter;

public class AsterixRemotingServiceFactory<T> implements AsterixFactoryBeanPlugin<T>, ExternalDependencyAware<AsterixRemotingPluginDependencies> {
	
	private final Class<T> serviceApi;
	private final String targetSpace;
    private final AsterixApiDescriptor descriptor;
	private ExternalDependency<AsterixRemotingPluginDependencies> dependencies;
	private AsterixServiceComponents serviceComponents;
	
	public AsterixRemotingServiceFactory(Class<T> serviceApi,
										String targetSpaceName, 
										AsterixApiDescriptor descriptor) {
		this.serviceApi = serviceApi;
		this.targetSpace = targetSpaceName;
		this.descriptor = descriptor;
	}
	
	@Override
	public T create(String qualifier) {
		AsterixServiceComponent serviceComponent = serviceComponents.getComponent(descriptor);

		String targetSpaceName = descriptor.getAnnotation(AsterixRemoteApiDescriptor.class).targetSpaceName();
		AsterixServiceProperties serviceProperties = new AsterixServiceProperties();
		serviceProperties.setApi(serviceApi);
		serviceProperties.setProperty(AsterixRemotingServiceRegistryExporter.SPACE_NAME_PROPERTY, targetSpaceName);
		serviceProperties.setProperty(GsBinder.SPACE_URL_PROPERTY, dependencies.get().getSpaceUrlBuilder().getSpaceUrl(targetSpace));
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

	@AsterixInject
	public void setServiceComponents(AsterixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
}
