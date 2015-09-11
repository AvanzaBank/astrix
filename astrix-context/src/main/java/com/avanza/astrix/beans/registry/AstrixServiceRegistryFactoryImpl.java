/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.registry;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.configdiscovery.ConfigDiscoveryProperties;
import com.avanza.astrix.beans.configdiscovery.ConfigServiceDiscoveryPlugin;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDefinitionSource;
import com.avanza.astrix.beans.service.ServiceDiscoveryFactory;
import com.avanza.astrix.beans.service.ServiceFactory;
import com.avanza.astrix.beans.service.ServiceMetaFactory;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceRegistryFactoryImpl implements AstrixServiceRegistryFactory {

	private final ServiceMetaFactory serviceMetaFactory;
	private final AstrixConfig config;
	private final ServiceComponentRegistry serviceComponents;
	private volatile AstrixServiceRegistry instance;

	public AstrixServiceRegistryFactoryImpl(ServiceMetaFactory serviceMetaFactory, 
										AstrixConfig config,
										ServiceComponentRegistry serviceComponents) {
		this.serviceMetaFactory = serviceMetaFactory;
		this.config = config;
		this.serviceComponents = serviceComponents;
	}

	@Override
	public synchronized AstrixServiceRegistry createServiceRegistry() {
		if (instance != null) {
			return instance;
		}
		ObjectSerializerDefinition serializer = ObjectSerializerDefinition.versionedService(ServiceRegistryObjectSerializerConfigurer.VERSION, 
																							ServiceRegistryObjectSerializerConfigurer.class);
		ServiceDefinition<AstrixServiceRegistry> serviceRegistryDefinition = 
				new ServiceDefinition<>(
						ServiceDefinitionSource.create("AstrixServiceRegistry"),
						AstrixBeanKey.create(AstrixServiceRegistry.class),
						serializer, 
						false); // Not dynamic qualified
		ServiceDiscoveryFactory<?> serviceDiscoveryFactory = createServiceDiscoveryFactory();
		
		ServiceFactory<AstrixServiceRegistry> serviceFactory = serviceMetaFactory.createServiceFactory(serviceRegistryDefinition,
																									   serviceDiscoveryFactory);
		
		instance = serviceFactory.create(AstrixBeanKey.create(AstrixServiceRegistry.class));
		return instance;
	}

	private ServiceDiscoveryFactory<?> createServiceDiscoveryFactory() {
		return new ServiceDiscoveryFactory<ConfigDiscoveryProperties>(new ConfigServiceDiscoveryPlugin(serviceComponents, config), 
				new ConfigDiscoveryProperties(AstrixSettings.SERVICE_REGISTRY_URI_PROPERTY_NAME), 
				AstrixServiceRegistry.class);
	}

}
