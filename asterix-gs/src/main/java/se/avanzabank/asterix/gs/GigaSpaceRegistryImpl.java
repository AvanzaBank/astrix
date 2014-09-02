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
package se.avanzabank.asterix.gs;

import java.util.Objects;

import org.openspaces.core.GigaSpace;

import se.avanzabank.asterix.bus.client.AsterixServiceRegistry;
import se.avanzabank.asterix.bus.client.AsterixServiceProperties;

public class GigaSpaceRegistryImpl implements GigaSpaceRegistry {
	
	private final AsterixServiceRegistry serviceRegistry;

	public GigaSpaceRegistryImpl(AsterixServiceRegistry serviceRegistry) {
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
	}

	@Override
	public GigaSpace lookup(String spaceName) {
		Objects.requireNonNull(spaceName);
		GsFactory factory = lookupFactory(spaceName); // TODO: cache lookups, manage missing space, etc.
		return factory.create();
	}

	private GsFactory lookupFactory(String spaceName) {
		AsterixServiceProperties lookup = serviceRegistry.lookup(GigaSpace.class, spaceName); // TODO: if lookup fails we want
		if (lookup == null) {
			throw new RuntimeException("Failed to lookup space: " + spaceName); // TODO: handle lookup failure (i.e bus n/a) and lookup 'miss', ie no service registered in bus yet. 
		}
		return GsBinder.createGsFactory(lookup, spaceName);
	}

}
