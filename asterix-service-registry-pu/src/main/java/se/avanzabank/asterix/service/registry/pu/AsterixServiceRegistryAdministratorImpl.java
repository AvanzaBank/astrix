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
package se.avanzabank.asterix.service.registry.pu;

import java.util.ArrayList;
import java.util.List;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.provider.core.AsterixServiceExport;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryAdministrator;
import se.avanzabank.asterix.service.registry.server.AsterixServiceRegistryEntry;

@AsterixServiceExport(AsterixServiceRegistryAdministrator.class)
public class AsterixServiceRegistryAdministratorImpl implements AsterixServiceRegistryAdministrator {

	private final GigaSpace gigaSpace;
	
	@Autowired
	public AsterixServiceRegistryAdministratorImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}
	
	@Override
	public List<AsterixServiceRegistryEntry> listServices() {
		SpaceServiceRegistryEntry[] entries = gigaSpace.readMultiple(SpaceServiceRegistryEntry.template());
		List<AsterixServiceRegistryEntry> result = new ArrayList<>();
		for (SpaceServiceRegistryEntry spaceEntry : entries) {
			AsterixServiceRegistryEntry entry = new AsterixServiceRegistryEntry();
			entry.setQualifier(spaceEntry.getServiceKey().getQualifier());
			entry.setServiceBeanType(spaceEntry.getApiType());
			entry.setServiceProperties(spaceEntry.getProperties());
			result.add(entry);
		}
		return result;
	}
}
