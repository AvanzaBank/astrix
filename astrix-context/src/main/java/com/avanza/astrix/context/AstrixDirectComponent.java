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
package com.avanza.astrix.context;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.provider.component.AstrixServiceComponentNames;

@MetaInfServices(AstrixServiceComponent.class)
public class AstrixDirectComponent implements AstrixServiceComponent {
	
	private final static AtomicLong idGen = new AtomicLong();
	private final static Map<String, ServiceProvider<?>> providerById = new ConcurrentHashMap<>();
	
	@Override
	public <T> T createService(AstrixApiDescriptor apiDescriptor, Class<T> type, AstrixServiceProperties serviceProperties) {
		String providerName = serviceProperties.getProperty("providerId");
		ServiceProvider<?> result = providerById.get(providerName);
		if (result == null) {
			throw new IllegalStateException("Cant find provider for with name="  + providerName + " and type=" + type);
		}
		return type.cast(result.getProvider());
	}
	
	@Override
	public <T> AstrixServiceProperties createServiceProperties(String componentSpecificUri) {
		String providerId = componentSpecificUri;
		return getServiceProperties(providerId);
	}

	@Override
	public String getName() {
		return AstrixServiceComponentNames.DIRECT;
	}

	public static <T> String register(Class<T> type, T provider) {
		String id = String.valueOf(idGen.incrementAndGet());
		providerById.put(id, new ServiceProvider<T>(id, type, provider));
		return id;
	}
	
	public static <T> AstrixServiceProperties registerAndGetProperties(Class<T> type, T provider) {
		String id = register(type, provider);
		return getServiceProperties(id);
	}
	
	public static AstrixServiceProperties getServiceProperties(String id) {
		ServiceProvider<?> provider = providerById.get(id);
		if (provider == null) {
			throw new IllegalArgumentException("No provider registered with id: " + id);
		}
		AstrixServiceProperties serviceProperties = new AstrixServiceProperties();
		serviceProperties.setProperty("providerId", id);
		serviceProperties.setComponent(AstrixServiceComponentNames.DIRECT);
		return serviceProperties;
	}
	
	public static String getServiceUri(String id) {
		ServiceProvider<?> provider = providerById.get(id);
		if (provider == null) {
			throw new IllegalArgumentException("No provider registered with id: " + id);
		}
		return AstrixServiceComponentNames.DIRECT + ":" + id; 
	}
	
	public static <T> T getServiceProvider(Class<T> expectedType, String id) {
		ServiceProvider<T> provider = (ServiceProvider<T>) providerById.get(id);
		if (provider == null) {
			throw new IllegalArgumentException("No provider registered with id: " + id);
		}
		return provider.getProvider();
	}
	
	public static <T> T getServiceProvider(String id) {
		ServiceProvider<T> provider = (ServiceProvider<T>) providerById.get(id);
		if (provider == null) {
			throw new IllegalArgumentException("No provider registered with id: " + id);
		}
		return provider.getProvider();
	}
	
	static class ServiceProvider<T> {
		private String id;
		private Class<T> type;
		private T provider;
		
		public ServiceProvider(String id, Class<T> type, T provider) {
			this.id = id;
			this.type = type;
			this.provider = provider;
		}
		
		public String getId() {
			return id;
		}
		
		public Class<T> getType() {
			return type;
		}
		
		public T getProvider() {
			return provider;
		}
	}

	public Collection<ServiceProvider<?>> listProviders() {
		return providerById.values();
	}

	public void clear(String id) {
		providerById.remove(id);
	}

	@Override
	public <T> void exportService(Class<T> providedApi, T provider, AstrixApiDescriptor apiDescriptor) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public List<AstrixExportedServiceInfo> getImplicitExportedServices() {
		return Collections.emptyList();
	}
	
	@Override
	public boolean supportsAsyncApis() {
		return false;
	}

	@Override
	public <T> AstrixServiceProperties createServiceProperties(Class<T> exportedService) {
		throw new UnsupportedOperationException();
	}

	public static <T> String registerAndGetUri(Class<T> api, T provider) {
		String id = register(api, provider);
		return getServiceUri(id);
	}
	
}