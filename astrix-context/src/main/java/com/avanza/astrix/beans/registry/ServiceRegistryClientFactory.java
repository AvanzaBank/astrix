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
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.modules.AstrixInject;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class ServiceRegistryClientFactory {
	
	private final DynamicConfig config;
	private final AstrixServiceRegistryFactory serviceRegistryFactory;

	@AstrixInject
	public ServiceRegistryClientFactory(AstrixConfig config, AstrixServiceRegistryFactory serviceRegistryFactory) {
		this.config = config.getConfig();
		this.serviceRegistryFactory = serviceRegistryFactory;
	}
	
	public ServiceRegistryClientFactory(DynamicConfig config, AstrixServiceRegistryFactory serviceRegistryFactory) {
		this.config = config;
		this.serviceRegistryFactory = serviceRegistryFactory;
	}

	public ServiceRegistryClient createServiceRegistryClient() {
		return new ServiceRegistryClient(serviceRegistryFactory.createServiceRegistry(), getServiceConsumerProps());
	}

	private ServiceConsumerProperties getServiceConsumerProps() {
		ServiceConsumerProperties serviceConsumerProperties = new ServiceConsumerProperties();
		String subsystem = AstrixSettings.SUBSYSTEM_NAME.getFrom(config).get();
		String applicationTag = AstrixSettings.APPLICATION_TAG.getFrom(config).get();
		String zone = subsystem;
		if (applicationTag != null) {
			zone = subsystem + "#" + applicationTag;
		}
		serviceConsumerProperties.setProperty(ServiceConsumerProperties.CONSUMER_ZONE, zone);
		return serviceConsumerProperties;
	}
	
	
	
}
