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
package se.avanzabank.asterix.service.registry.client;

import org.openspaces.remoting.Routing;

import se.avanzabank.asterix.context.AsterixServiceProperties;

public class AsterixServiceRegistryClientImpl implements AsterixServiceRegistryClient {
	
	private AsterixServiceRegistry serviceRegistry;

	public AsterixServiceRegistryClientImpl(AsterixServiceRegistry serviceRegistry) {
		this.serviceRegistry = serviceRegistry;
	}

	@Override
	public <T> AsterixServiceProperties lookup(@Routing Class<T> type) {
		return serviceRegistry.lookup(type.getName());
	}

	@Override
	public <T> AsterixServiceProperties lookup(@Routing Class<T> type, String qualifier) {
		return serviceRegistry.lookup(type.getName(), qualifier);
	}

	@Override
	public <T> void register(@Routing Class<T> type, AsterixServiceProperties properties) {
		this.serviceRegistry.register(type.getName(), properties);
	}

}
