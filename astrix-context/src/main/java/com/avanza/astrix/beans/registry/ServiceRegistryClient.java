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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.beans.service.ServiceProviderInstanceProperties;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceRegistryClient {
	
	private final AstrixServiceRegistry serviceRegistry;
	private final ServiceConsumerProperties consumerProperties;

	public ServiceRegistryClient(AstrixServiceRegistry serviceRegistry, ServiceConsumerProperties serviceConsumerProperties) {
		this.consumerProperties = Objects.requireNonNull(serviceConsumerProperties);
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
	}

	public <T> ServiceProviderInstanceProperties lookup(AstrixBeanKey<T> beanKey) {
		AstrixServiceRegistryEntry entry = serviceRegistry.lookup(beanKey.getBeanType().getName(), beanKey.getQualifier(), consumerProperties);
		if (entry == null) {
			return null;
		}
		return new ServiceProviderInstanceProperties(entry.getServiceProperties());
	}

	public <T> List<ServiceProviderInstanceProperties> list(AstrixBeanKey<T> beanKey) {
		List<AstrixServiceRegistryEntry> registeresServices = serviceRegistry.listServices(beanKey.getBeanType().getName(), beanKey.getQualifier());
		List<ServiceProviderInstanceProperties> result = new ArrayList<>(registeresServices.size());
		for (AstrixServiceRegistryEntry entry : registeresServices) {
			result.add(new ServiceProviderInstanceProperties(entry.getServiceProperties()));
		}
		return result;
	}

}
