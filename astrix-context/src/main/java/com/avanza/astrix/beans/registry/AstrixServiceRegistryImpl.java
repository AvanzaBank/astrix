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
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.service.ServiceProviderInstanceProperties;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.provider.core.AstrixServiceExport;

@AstrixServiceExport(AstrixServiceRegistry.class)
public class AstrixServiceRegistryImpl implements AstrixServiceRegistry {
	
	private final Logger log = LoggerFactory.getLogger(AstrixServiceRegistryImpl.class);
	private final ServiceRegistryEntryRepository serviceRegistryEntryRepo;
	private final AtomicLong serviceCounter = new AtomicLong();
	
	public AstrixServiceRegistryImpl(ServiceRegistryEntryRepository serviceRegistryEntryRepo) {
		this.serviceRegistryEntryRepo = serviceRegistryEntryRepo;
	}
	
	@Override
	public <T> AstrixServiceRegistryEntry lookup(String type, String qualifier, ServiceConsumerProperties serviceConsumerProperties) {
		List<AstrixServiceRegistryEntry> entries = serviceRegistryEntryRepo.findByServiceKey(new ServiceKey(type, qualifier));
		if (entries.isEmpty()) {
			return null;
		}
		List<AstrixServiceRegistryEntry> activeServices = getServiceProvidersForConsumer(entries, serviceConsumerProperties);
		if (activeServices.isEmpty()) {
			return null;
		}
		return activeServices.get((int) (serviceCounter.incrementAndGet() % activeServices.size()));
	}

	private List<AstrixServiceRegistryEntry> getServiceProvidersForConsumer(List<AstrixServiceRegistryEntry> entries, ServiceConsumerProperties serviceConsumer) {
		List<AstrixServiceRegistryEntry> activeServices = new ArrayList<>(entries.size());
		String consumerZone = serviceConsumer.getProperty(ServiceConsumerProperties.CONSUMER_ZONE);
		for (AstrixServiceRegistryEntry entry : entries) {
			if ("true".equals(entry.getServiceProperties().get(ServiceProviderInstanceProperties.PUBLISHED))) {
				activeServices.add(entry);
				continue;
			}
			if (!Objects.equals(consumerZone, entry.getServiceProperties().get(ServiceProviderInstanceProperties.SERVICE_ZONE))) {
				log.debug("Discarding service-provider={}, consumer={}", entry.getServiceProperties(), serviceConsumer);
				continue;
			}
			activeServices.add(entry);
		}
		return activeServices;
	}

	@Override
	public <T> void register(AstrixServiceRegistryEntry entry, long lease) {
		serviceRegistryEntryRepo.insertOrUpdate(entry, lease);
	}
	
	@Override
	public <T> void deregister(AstrixServiceRegistryEntry properties) {
		serviceRegistryEntryRepo.remove(getServiceProviderKey(properties));
	}
	
	private ServiceProviderKey getServiceProviderKey(AstrixServiceRegistryEntry properties) {
		String appInstanceId = properties.getServiceProperties().get(ServiceProviderInstanceProperties.APPLICATION_INSTANCE_ID);
		String api = properties.getServiceBeanType();
		String qualifier = properties.getServiceProperties().get(ServiceProviderInstanceProperties.QUALIFIER);
		ServiceProviderKey serviceProviderKey = ServiceProviderKey.create(new ServiceKey(api, qualifier), appInstanceId);
		return serviceProviderKey;
	}
	
	

	@Override
	public List<AstrixServiceRegistryEntry> listServices() {
		return serviceRegistryEntryRepo.findAll();
	}
	
	@Override
	public List<AstrixServiceRegistryEntry> listServices(String type, String qualifier) {
		return serviceRegistryEntryRepo.findByServiceKey(new ServiceKey(type, qualifier));
	}

}
