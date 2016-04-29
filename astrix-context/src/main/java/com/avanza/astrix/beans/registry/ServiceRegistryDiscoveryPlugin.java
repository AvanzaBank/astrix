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

import java.util.Arrays;
import java.util.Collections;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.service.ServiceDiscovery;
import com.avanza.astrix.beans.service.ServiceDiscoveryFactoryPlugin;
import com.avanza.astrix.beans.service.ServiceProviderInstanceProperties;
import com.avanza.astrix.beans.service.ServiceProviders;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceRegistryDiscoveryPlugin implements ServiceDiscoveryFactoryPlugin<ServiceRegistryDiscoveryProperties> {
	
	private final ServiceRegistryClientFactory serviceRegistryClientFactory;
	
	public ServiceRegistryDiscoveryPlugin(ServiceRegistryClientFactory serviceRegistryClientFactory) {
		this.serviceRegistryClientFactory = serviceRegistryClientFactory;
	}

	@Override
	public Class<ServiceRegistryDiscoveryProperties> getDiscoveryPropertiesType() {
		return ServiceRegistryDiscoveryProperties.class;
	}

	@Override
	public ServiceDiscovery create(AstrixBeanKey<?> key, ServiceRegistryDiscoveryProperties lookupAnnotation) {
		return new ServiceRegistryDiscovery(key, serviceRegistryClientFactory.createServiceRegistryClient());
	}
	
	private static class ServiceRegistryDiscovery implements ServiceDiscovery {
		/*
		 * IMPLEMENTATION NOTE:
		 * To avoid that the service-registry it creating an instance of AstrixServiceRegistry against
		 * itself we create the ServiceRegistryClient lazily
		 */
		
		private final AstrixBeanKey<?> beanKey;
		private final ServiceRegistryClient serviceRegistryClient;

		public ServiceRegistryDiscovery(AstrixBeanKey<?> key, ServiceRegistryClient serviceRegistryClient) {
			this.beanKey = key;
			this.serviceRegistryClient = serviceRegistryClient;
		}
		
		@Override
		public String description() {
			return "ServiceRegistry";
		}

		@Override
		public ServiceProviders run() {
			ServiceProviderInstanceProperties lookup = serviceRegistryClient.lookup(beanKey);
			if (lookup != null) {
				return new ServiceProviders(Arrays.asList(lookup));
			}
			return new ServiceProviders(Collections.emptyList());
		}
		
	}

}
