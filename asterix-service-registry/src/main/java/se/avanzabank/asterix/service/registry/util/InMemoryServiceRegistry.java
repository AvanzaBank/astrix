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
package se.avanzabank.asterix.service.registry.util;

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import se.avanzabank.asterix.context.AsterixDirectComponent;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.provider.core.AsterixConfigApi;
import se.avanzabank.asterix.service.registry.app.ServiceKey;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistry;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryApiDescriptor;
import se.avanzabank.asterix.service.registry.server.AsterixServiceRegistryEntry;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class InMemoryServiceRegistry implements AsterixServiceRegistry {
	
	private Map<ServiceKey, AsterixServiceRegistryEntry> servicePropertiesByKey = new ConcurrentHashMap<>();
	private String id;
	
	public InMemoryServiceRegistry() {
		this.id = AsterixDirectComponent.register(AsterixServiceRegistry.class, this);
	}
	
	@Override
	public <T> AsterixServiceRegistryEntry lookup(String type) {
		return this.servicePropertiesByKey.get(new ServiceKey(type));
	}
	@Override
	public <T> AsterixServiceRegistryEntry lookup(String type, String qualifier) {
		return this.servicePropertiesByKey.get(new ServiceKey(type, qualifier));
	}
	@Override
	public <T> void register(AsterixServiceRegistryEntry properties, long lease) {
		this.servicePropertiesByKey.put(new ServiceKey(properties.getServiceBeanType(), properties.getQualifier()), properties);
	}
	
	public void clear() {
		this.servicePropertiesByKey.clear();
	}
	public String getJndiEntryName() {
		return AsterixServiceRegistryApiDescriptor.class.getAnnotation(AsterixConfigApi.class).entryName();
	}
	public Properties getJndiProperties() {
		AsterixServiceProperties properties = AsterixDirectComponent.getServiceProperties(id);
		Properties result = new Properties();
		result.putAll(properties.getProperties());
		return result;
	}
}