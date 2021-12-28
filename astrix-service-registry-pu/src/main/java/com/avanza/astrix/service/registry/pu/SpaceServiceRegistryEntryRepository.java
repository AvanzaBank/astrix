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
package com.avanza.astrix.service.registry.pu;

import static com.avanza.astrix.gs.GsBinder.START_TIME;
import static java.lang.Math.max;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryEntry;
import com.avanza.astrix.beans.registry.ServiceKey;
import com.avanza.astrix.beans.registry.ServiceProviderKey;
import com.avanza.astrix.beans.registry.ServiceRegistryEntryRepository;
import com.avanza.astrix.beans.service.ServiceProperties;

public class SpaceServiceRegistryEntryRepository implements ServiceRegistryEntryRepository {
	
	private final GigaSpace gigaSpace;
	
	@Autowired
	public SpaceServiceRegistryEntryRepository(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public void insertOrUpdate(AstrixServiceRegistryEntry entry, long lease) {
		SpaceServiceRegistryEntry spaceEntry = new SpaceServiceRegistryEntry();
		spaceEntry.setApiType(entry.getServiceBeanType());
		ServiceKey serviceKey = new ServiceKey(entry.getServiceBeanType(), entry.getServiceProperties().get(ServiceProperties.QUALIFIER));
		spaceEntry.setServiceKey(serviceKey);
		String applicationInstanceId = entry.getServiceProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID);
		ServiceProviderKey serviceProviderKey = ServiceProviderKey.create(serviceKey, applicationInstanceId);
		spaceEntry.setServiceProviderKey(serviceProviderKey);
		spaceEntry.setProperties(entry.getServiceProperties());
		setLatestStartTime(spaceEntry);
		Map<String, String> metadata = new HashMap<>();
		Date now = new Date();
		metadata.put("lastLeaseRenewalTime", now.toString());
		metadata.put("leaseExpireTime", new Date(now.getTime() + lease).toString());
		spaceEntry.setServiceMetadata(metadata);
		gigaSpace.write(spaceEntry, lease);
	}

	private void setLatestStartTime(SpaceServiceRegistryEntry spaceEntry) {
		final SpaceServiceRegistryEntry existingEntry = gigaSpace.readById(
				SpaceServiceRegistryEntry.class,
				spaceEntry.getServiceProviderKey(),
				spaceEntry.getApiType()
		);
		if (existingEntry == null) {
			return;
		}
		final long startTimeExistingEntry = getStartTime(existingEntry);
		final long startTimeNewEntry = getStartTime(spaceEntry);
		spaceEntry.getProperties().put(
				START_TIME,
				Long.toString(max(startTimeExistingEntry, startTimeNewEntry))
		);
	}

	private long getStartTime(SpaceServiceRegistryEntry entry) {
		return Optional.ofNullable(entry.getProperties())
				.map(p -> p.get(START_TIME))
				.map(Long::parseLong)
				.orElse(0L);
	}

	@Override
	public List<AstrixServiceRegistryEntry> findAll() {
		SpaceServiceRegistryEntry[] entries = gigaSpace.readMultiple(SpaceServiceRegistryEntry.template());
		List<AstrixServiceRegistryEntry> result = new ArrayList<>();
		for (SpaceServiceRegistryEntry spaceEntry : entries) {
			AstrixServiceRegistryEntry entry = new AstrixServiceRegistryEntry();
			entry.setServiceBeanType(spaceEntry.getApiType());
			entry.setServiceProperties(spaceEntry.getProperties());
			entry.setServiceMetadata(spaceEntry.getServiceMetadata());
			result.add(entry);
		}
		return result;
	}
	
	@Override
	public List<AstrixServiceRegistryEntry> findByServiceKey(ServiceKey serviceKey) {
		SpaceServiceRegistryEntry template = SpaceServiceRegistryEntry.template();
		template.setServiceKey(serviceKey);
		SpaceServiceRegistryEntry[] entries = gigaSpace.readMultiple(template);
		List<AstrixServiceRegistryEntry> result = new ArrayList<>(entries.length);
		for (SpaceServiceRegistryEntry spaceEntry : entries) {
			AstrixServiceRegistryEntry entry = new AstrixServiceRegistryEntry();
			entry.setServiceBeanType(spaceEntry.getApiType());
			entry.setServiceProperties(spaceEntry.getProperties());
			entry.setServiceMetadata(spaceEntry.getServiceMetadata());
			result.add(entry);
		}
		return result;
	}

	@Override
	public void remove(ServiceProviderKey serviceProviderKey) {
		gigaSpace.takeById(SpaceServiceRegistryEntry.class, serviceProviderKey);
	}


}
