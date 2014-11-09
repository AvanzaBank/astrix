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
package com.avanza.asterix.service.registry.pu;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.asterix.context.AsterixServiceProperties;
import com.avanza.asterix.provider.core.AsterixServiceExport;
import com.avanza.asterix.service.registry.app.ServiceKey;
import com.avanza.asterix.service.registry.client.AsterixServiceRegistry;
import com.avanza.asterix.service.registry.server.AsterixServiceRegistryEntry;

@AsterixServiceExport(AsterixServiceRegistry.class)
public class AsterixServiceRegistryImpl implements AsterixServiceRegistry {
	
	private final GigaSpace gigaSpace;
	
	@Autowired
	public AsterixServiceRegistryImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public <T> AsterixServiceRegistryEntry lookup(String type, String qualifier) {
		SpaceServiceRegistryEntry entry = gigaSpace.readById(SpaceServiceRegistryEntry.class, new ServiceKey(type, qualifier));
		if (entry == null) {
			return null;
		}
		AsterixServiceRegistryEntry result = new AsterixServiceRegistryEntry();
		result.setServiceBeanType(type);
		result.setServiceProperties(entry.getProperties());
		result.setServiceMetadata(result.getServiceMetadata());
		return result;
	}

	@Override
	public <T> void register(AsterixServiceRegistryEntry entry, long lease) {
		SpaceServiceRegistryEntry spaceEntry = new SpaceServiceRegistryEntry();
		spaceEntry.setApiType(entry.getServiceBeanType());
		spaceEntry.setServiceKey(new ServiceKey(entry.getServiceBeanType(), entry.getServiceProperties().get(AsterixServiceProperties.QUALIFIER)));
		spaceEntry.setProperties(entry.getServiceProperties());
		Map<String, String> metadata = new HashMap<>();
		Date now = new Date();
		metadata.put("lastLeaseRenewalTime", now.toString());
		metadata.put("leaseExpireTime", new Date(now.getTime() + lease).toString());
		spaceEntry.setServiceMetadata(metadata);
		gigaSpace.write(spaceEntry, lease);
	}

	@Override
	public List<AsterixServiceRegistryEntry> listServices() {
		SpaceServiceRegistryEntry[] entries = gigaSpace.readMultiple(SpaceServiceRegistryEntry.template());
		List<AsterixServiceRegistryEntry> result = new ArrayList<>();
		for (SpaceServiceRegistryEntry spaceEntry : entries) {
			AsterixServiceRegistryEntry entry = new AsterixServiceRegistryEntry();
			entry.setServiceBeanType(spaceEntry.getApiType());
			entry.setServiceProperties(spaceEntry.getProperties());
			entry.setServiceMetadata(spaceEntry.getServiceMetadata());
			result.add(entry);
		}
		return result;
	}

}
