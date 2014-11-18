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
package com.avanza.astrix.service.registry.client;

import java.util.Objects;

import org.openspaces.remoting.Routing;

import com.avanza.astrix.context.AsterixServiceProperties;
import com.avanza.astrix.service.registry.server.AsterixServiceRegistryEntry;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceRegistryClientImpl implements AsterixServiceRegistryClient {
	
	private final AsterixServiceRegistry serviceRegistry;
	private final String subsystem;

	public AsterixServiceRegistryClientImpl(AsterixServiceRegistry serviceRegistry, String subsystem) {
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
		this.subsystem = Objects.requireNonNull(subsystem);
	}

	@Override
	public <T> AsterixServiceProperties lookup(@Routing Class<T> type) {
		return lookup(type, null);
	}

	@Override
	public <T> AsterixServiceProperties lookup(@Routing Class<T> type, String qualifier) {
		AsterixServiceRegistryEntry entry = serviceRegistry.lookup(type.getName(), qualifier);
		if (entry == null) {
			return null;
		}
		return new AsterixServiceProperties(entry.getServiceProperties());
	}

	@Override
	public <T> void register(@Routing Class<T> type, AsterixServiceProperties properties, long lease) {
		properties.setProperty(AsterixServiceProperties.SUBSYSTEM, this.subsystem);
		AsterixServiceRegistryEntry entry = new AsterixServiceRegistryEntry();
		entry.setServiceProperties(properties.getProperties());
		entry.setServiceBeanType(type.getName());
		this.serviceRegistry.register(entry, lease);
	}

}
