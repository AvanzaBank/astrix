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
import java.util.concurrent.ConcurrentHashMap;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.config.DynamicConfigSource;
import com.avanza.astrix.config.DynamicPropertyListener;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.service.registry.app.ServiceKey;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistry;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistryServiceProvider;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistryClientImpl;
import com.avanza.astrix.service.registry.server.AstrixServiceRegistryEntry;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class InMemoryServiceRegistry implements AstrixServiceRegistry, DynamicConfigSource {
	
	private final MapConfigSource configSource = new MapConfigSource();
	private Map<ServiceKey, AstrixServiceRegistryEntry> servicePropertiesByKey = new ConcurrentHashMap<>();
	private String id;
	private String configSourceId;
	
	public InMemoryServiceRegistry() {
		this.id = AstrixDirectComponent.register(AstrixServiceRegistry.class, this);
		this.configSourceId = GlobalConfigSourceRegistry.register(this);
		this.configSource.set(AstrixSettings.ASTRIX_SERVICE_REGISTRY_URI, getServiceUri());
	}
	
	@Override
	public <T> AstrixServiceRegistryEntry lookup(String type, String qualifier) {
		return this.servicePropertiesByKey.get(new ServiceKey(type, qualifier));
	}
	
	public String getConfigSourceId() {
		return configSourceId;
	}
	
	@Override
	public <T> void register(AstrixServiceRegistryEntry properties, long lease) {
		ServiceKey key = new ServiceKey(properties.getServiceBeanType(), properties.getServiceProperties().get(AstrixServiceProperties.QUALIFIER));
		this.servicePropertiesByKey.put(key, properties);
	}
	
	public void clear() {
		this.servicePropertiesByKey.clear();
	}
	
	public String getConfigEntryName() {
		return AstrixSettings.ASTRIX_SERVICE_REGISTRY_URI;
	}
	
	public String getServiceUri() {
		return AstrixServiceComponentNames.DIRECT + ":" + this.id;
	}

	@Override
	public List<AstrixServiceRegistryEntry> listServices() {
		return new ArrayList<>(servicePropertiesByKey.values());
	}
	
	public void set(String settingName, String value) {
		this.configSource.set(settingName, value);
	}
	
	public void addConfig(String settingName, long value) {
		this.configSource.set(settingName, Long.toString(value));
	}

	public <T> void registerProvider(Class<T> api, T provider, String subsystem) {
		AstrixServiceRegistryClientImpl serviceRegistryClient = new AstrixServiceRegistryClientImpl(this, subsystem);
		serviceRegistryClient.register(api, AstrixDirectComponent.registerAndGetProperties(api, provider), 60_000);
	}
	
	/**
	 * Registers a provider for a given service that belongs to the 'default' subsystem.
	 * 
	 * @param api
	 * @param provider
	 */
	public <T> void registerProvider(Class<T> api, T provider) {
		registerProvider(api, provider, "default");
	}
	
	@Override
	public String get(String propertyName) {
		return configSource.get(propertyName);
	}

	@Override
	public String get(String propertyName, DynamicPropertyListener<String> propertyChangeListener) {
		return configSource.get(propertyName, propertyChangeListener);
	}
}