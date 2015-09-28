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
package com.avanza.astrix.beans.registry;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.config.BooleanSetting;
import com.avanza.astrix.config.DynamicConfigSource;
import com.avanza.astrix.config.DynamicPropertyListener;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.config.MutableConfigSource;
import com.avanza.astrix.config.Setting;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixServiceExport;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@AstrixServiceExport(AstrixServiceRegistry.class)
public class InMemoryServiceRegistry implements DynamicConfigSource, AstrixServiceRegistry, MutableConfigSource {
	
	private final MapConfigSource configSource = new MapConfigSource();
	private String id;
	private String configSourceId;
	private InMemoryServiceRegistryRepo repo = new InMemoryServiceRegistryRepo();
	private AstrixServiceRegistry serviceRegistry = new AstrixServiceRegistryImpl(repo);
	
	public InMemoryServiceRegistry() {
		this.id = DirectComponent.register(AstrixServiceRegistry.class, this);
		this.configSourceId = GlobalConfigSourceRegistry.register(this);
		this.configSource.set(AstrixSettings.SERVICE_REGISTRY_URI, getServiceUri());
	}
	
	@Override
	public List<AstrixServiceRegistryEntry> listServices() {
		return serviceRegistry.listServices();
	}
	
	@Override
	public <T> AstrixServiceRegistryEntry lookup(String type, String qualifier, ServiceConsumerProperties consumerProperties) {
		return serviceRegistry.lookup(type, qualifier, consumerProperties);
	}
	
	@Override
	public <T> void register(AstrixServiceRegistryEntry properties, long lease) {
		serviceRegistry.register(properties, lease);
	}
	
	@Override
	public <T> void deregister(AstrixServiceRegistryEntry properties) {
		serviceRegistry.deregister(properties);
	}
	
	@Override
	public List<AstrixServiceRegistryEntry> listServices(String type, String qualifier) {
		return this.serviceRegistry.listServices(type, qualifier);
	}
	
	public String getConfigSourceId() {
		return configSourceId;
	}
	
	public void clear() {
		this.repo.clear();
	}

	/**
	 * 
	 * @return
	 * @deprecated replaced by {@link AstrixSettings#SERVICE_REGISTRY_URI}
	 */
	@Deprecated
	public String getConfigEntryName() {
		return AstrixSettings.SERVICE_REGISTRY_URI_PROPERTY_NAME;
	}
	
	public String getServiceUri() {
		return AstrixServiceComponentNames.DIRECT + ":" + this.id;
	}

	public void set(String settingName, String value) {
		this.configSource.set(settingName, value);
	}
	
	public void set(String settingName, long value) {
		this.configSource.set(settingName, Long.toString(value));
	}
	
	@Override
	public <T> void set(Setting<T> setting, T value) {
		this.configSource.set(setting, value);
	}
	
	@Override
	public void set(BooleanSetting setting, boolean value) {
		this.configSource.set(setting, value);
	}
	
	@Override
	public void set(LongSetting setting, long value) {
		this.configSource.set(setting, value);
	}
	
	
	public <T> void registerProvider(Class<T> api, T provider, String subsystem) {
		// TODO: remove this method?
		registerProvider(AstrixBeanKey.create(api), provider, subsystem);
	}
	
	public <T> void registerProvider(Class<T> api, ServiceProperties serviceProperties) {
		// TODO: remove this method?
		registerServiceProvider(AstrixBeanKey.create(api), "default", serviceProperties);
	}
	
	private <T> void registerProvider(AstrixBeanKey<T> beanKey, T provider, String subsystem) {
		ServiceProperties serviceProperties = DirectComponent.registerAndGetProperties(beanKey.getBeanType(), provider);
		registerServiceProvider(beanKey, subsystem, serviceProperties);
	}

	private <T> void registerServiceProvider(AstrixBeanKey<T> beanKey, String subsystem, ServiceProperties serviceProperties) {
		ServiceRegistryExporterClient serviceRegistryClient = new ServiceRegistryExporterClient(this.serviceRegistry, subsystem, beanKey.toString());
		serviceProperties.setQualifier(beanKey.getQualifier());
		serviceRegistryClient.register(beanKey.getBeanType(), serviceProperties, 60_000);
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
	
	public <T> void registerProvider(Class<T> api, String qualifier, T provider) {
		registerProvider(AstrixBeanKey.create(api, qualifier), provider, "default");
	}
	
	@Override
	public String get(String propertyName) {
		return configSource.get(propertyName);
	}
	
	@Override
	public String get(String propertyName, DynamicPropertyListener<String> propertyChangeListener) {
		return configSource.get(propertyName, propertyChangeListener);
	}
	
	private static class InMemoryServiceRegistryRepo implements ServiceRegistryEntryRepository {
		
		private Map<ServiceProviderKey, AstrixServiceRegistryEntry> servicePropertiesByApplicationInstanceId = new ConcurrentHashMap<>();

		@Override
		public List<AstrixServiceRegistryEntry> findAll() {
			return new ArrayList<>(servicePropertiesByApplicationInstanceId.values());
		}
		
		@Override
		public List<AstrixServiceRegistryEntry> findByServiceKey(ServiceKey serviceKey) {
			List<AstrixServiceRegistryEntry> result = new ArrayList<>();
			for (AstrixServiceRegistryEntry entry : this.servicePropertiesByApplicationInstanceId.values()) {
				if (serviceKey.equals(getServiceKey(entry))) {
					result.add(entry);
				}
			}
			return result;
		}
		
		@Override
		public void insertOrUpdate(AstrixServiceRegistryEntry entry, long lease) {
			this.servicePropertiesByApplicationInstanceId.put(getServiceProviderKey(entry), entry);
		}
		
		@Override
		public void remove(ServiceProviderKey serviceProviderKey) {
			this.servicePropertiesByApplicationInstanceId.remove(serviceProviderKey);
		}
		
		private ServiceProviderKey getServiceProviderKey(AstrixServiceRegistryEntry properties) {
			String appInstanceId = properties.getServiceProperties().get(ServiceProperties.APPLICATION_INSTANCE_ID);
			return ServiceProviderKey.create(getServiceKey(properties), appInstanceId);
		}
		
		private ServiceKey getServiceKey(AstrixServiceRegistryEntry properties) {
			String api = properties.getServiceBeanType();
			String qualifier = properties.getServiceProperties().get(ServiceProperties.QUALIFIER);
			return new ServiceKey(api, qualifier);
		}
		
		void clear() {
			this.servicePropertiesByApplicationInstanceId.clear();
		}
		
	}
}