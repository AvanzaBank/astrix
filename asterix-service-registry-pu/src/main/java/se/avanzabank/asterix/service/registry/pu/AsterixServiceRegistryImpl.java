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

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.provider.core.AsterixServiceExport;
import se.avanzabank.asterix.service.registry.app.ServiceKey;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistry;
import se.avanzabank.asterix.service.registry.server.AsterixServiceRegistryEntry;

@AsterixServiceExport(AsterixServiceRegistry.class)
public class AsterixServiceRegistryImpl implements AsterixServiceRegistry {
	
	private final GigaSpace gigaSpace;
	
	@Autowired
	public AsterixServiceRegistryImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public <T> AsterixServiceRegistryEntry lookup(String type) {
		return lookup(type, null);
	}

	@Override
	public <T> AsterixServiceRegistryEntry lookup(String type, String qualifier) {
		SpaceServiceRegistryEntry entry = gigaSpace.readById(SpaceServiceRegistryEntry.class, new ServiceKey(type, qualifier));
		if (entry == null) {
			return null;
		}
		AsterixServiceRegistryEntry result = new AsterixServiceRegistryEntry();
		result.setQualifier(qualifier);
		result.setServiceBeanType(type);
		result.setServiceProperties(entry.getProperties());
		return result;
	}

	@Override
	public <T> void register(AsterixServiceRegistryEntry entry, long lease) {
		SpaceServiceRegistryEntry spaceEntry = new SpaceServiceRegistryEntry();
		spaceEntry.setApiType(entry.getServiceBeanType());
		spaceEntry.setServiceKey(new ServiceKey(entry.getServiceBeanType(), entry.getQualifier()));
		spaceEntry.setProperties(entry.getServiceProperties());
		gigaSpace.write(spaceEntry, lease);
	}

}
