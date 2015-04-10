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
package com.avanza.astrix.beans.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.provider.core.AstrixServiceExport;

@AstrixServiceExport(AstrixServiceRegistry.class)
public class AstrixServiceRegistryImpl implements AstrixServiceRegistry {
	
	private final ServiceRegistryEntryRepository serviceRegistryEntryRepo;
	private final AtomicLong serviceCounter = new AtomicLong();
	
	public AstrixServiceRegistryImpl(ServiceRegistryEntryRepository serviceRegistryEntryRepo) {
		this.serviceRegistryEntryRepo = serviceRegistryEntryRepo;
	}
	
	@Override
	public <T> AstrixServiceRegistryEntry lookup(String type, String qualifier) {
		List<AstrixServiceRegistryEntry> entries = serviceRegistryEntryRepo.findByServiceKey(new ServiceKey(type, qualifier));
		if (entries.isEmpty()) {
			return null;
		}
		List<AstrixServiceRegistryEntry> activeServices = getActiveServices(entries);
		if (activeServices.isEmpty()) {
			return null;
		}
		return activeServices.get((int) (serviceCounter.incrementAndGet() % activeServices.size()));
	}

	private List<AstrixServiceRegistryEntry> getActiveServices(List<AstrixServiceRegistryEntry> entries) {
		List<AstrixServiceRegistryEntry> activeServices = new ArrayList<>(entries.size());
		for (AstrixServiceRegistryEntry entry : entries) {
			if (!ServiceState.INACTIVE.equals(entry.getServiceProperties().get(AstrixServiceProperties.SERVICE_STATE))) {
				activeServices.add(entry);
			}
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
		String appInstanceId = properties.getServiceProperties().get(AstrixServiceProperties.APPLICATION_INSTANCE_ID);
		String api = properties.getServiceBeanType();
		String qualifier = properties.getServiceProperties().get(AstrixServiceProperties.QUALIFIER);
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
