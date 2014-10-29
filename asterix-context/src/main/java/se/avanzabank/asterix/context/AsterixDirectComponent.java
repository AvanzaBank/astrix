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
package se.avanzabank.asterix.context;

import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.provider.component.AsterixServiceComponentNames;

@MetaInfServices(AsterixServiceComponent.class)
public class AsterixDirectComponent implements AsterixServiceComponent {
	
	private final static AtomicLong idGen = new AtomicLong();
	private final static Map<String, ServiceProvider<?>> providerById = new ConcurrentHashMap<>();
	
	@Override
	public <T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties) {
		String providerName = serviceProperties.getProperty("providerId");
		ServiceProvider<?> result = providerById.get(providerName);
		if (result == null) {
			throw new IllegalStateException("Cant find provider for with name="  + providerName + " and type=" + type);
		}
		return type.cast(result.getProvider());
	}
	
	@Override
	public <T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, String componentSpecificUri) {
		String providerName = componentSpecificUri;
		ServiceProvider<?> result = providerById.get(providerName);
		if (result == null) {
			throw new IllegalStateException("Cant find provider for with name="  + providerName + " and type=" + type);
		}
		return type.cast(result.getProvider());
	}

	@Override
	public String getName() {
		return AsterixServiceComponentNames.DIRECT;
	}

	@Override
	public void registerBeans(BeanDefinitionRegistry registry) {
		 // NOT USED. Client side component only 
	}

	public static <T> String register(Class<T> type, T provider) {
		String id = String.valueOf(idGen.incrementAndGet());
		providerById.put(id, new ServiceProvider<T>(id, type, provider));
		return id;
	}
	
	public static AsterixServiceProperties getServiceProperties(String id) {
		ServiceProvider<?> provider = providerById.get(id);
		if (provider == null) {
			throw new IllegalArgumentException("No provider registered with id: " + id);
		}
		AsterixServiceProperties serviceProperties = new AsterixServiceProperties();
		serviceProperties.setProperty("providerId", id);
		serviceProperties.setComponent(AsterixServiceComponentNames.DIRECT);
		return serviceProperties;
	}
	
	public static <T> T getServiceProvider(Class<T> expectedType, String id) {
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
	public Class<? extends AsterixServiceExporterBean> getExporterBean() {
		// NOT USED. This is a client side component only
		return null;
	}

	@Override
	public Class<? extends AsterixServicePropertiesBuilder> getServiceBuilder() {
		// NOT USED. This is a client side component only
		return null;
	}
	
	@Override
	public Class<? extends Annotation> getApiDescriptorType() {
		return null;
	}
	
}