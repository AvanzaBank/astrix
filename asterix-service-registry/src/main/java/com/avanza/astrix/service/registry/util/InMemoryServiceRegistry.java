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
package com.avanza.astrix.service.registry.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import com.avanza.astrix.context.AsterixDirectComponent;
import com.avanza.astrix.context.AsterixServiceProperties;
import com.avanza.astrix.core.AsterixBroadcast;
import com.avanza.astrix.provider.component.AsterixServiceComponentNames;
import com.avanza.astrix.provider.core.AsterixConfigApi;
import com.avanza.astrix.service.registry.app.ServiceKey;
import com.avanza.astrix.service.registry.client.AsterixServiceRegistry;
import com.avanza.astrix.service.registry.client.AsterixServiceRegistryApiDescriptor;
import com.avanza.astrix.service.registry.server.AsterixServiceRegistryEntry;
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
	public <T> AsterixServiceRegistryEntry lookup(String type, String qualifier) {
		return this.servicePropertiesByKey.get(new ServiceKey(type, qualifier));
	}
	@Override
	public <T> void register(AsterixServiceRegistryEntry properties, long lease) {
		ServiceKey key = new ServiceKey(properties.getServiceBeanType(), properties.getServiceProperties().get(AsterixServiceProperties.QUALIFIER));
		this.servicePropertiesByKey.put(key, properties);
	}
	
	public void clear() {
		this.servicePropertiesByKey.clear();
	}
	public String getConfigEntryName() {
		return AsterixServiceRegistryApiDescriptor.class.getAnnotation(AsterixConfigApi.class).entryName();
	}
	
	public String getServiceUri() {
		return AsterixServiceComponentNames.DIRECT + ":" + this.id;
	}

	@Override
	public List<AsterixServiceRegistryEntry> listServices() {
		return new ArrayList<>(servicePropertiesByKey.values());
	}
}