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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.AstrixServiceComponent;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.core.IllegalServiceMetadataException;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixServiceComponent.class)
public class AstrixDirectComponent implements AstrixServiceComponent {
	
	private final static AtomicLong idGen = new AtomicLong();
	private final static Map<String, ServiceProvider<?>> providerById = new ConcurrentHashMap<>();
	
	private AstrixVersioningPlugin versioningPlugin;
	private final List<DirectBoundServiceBeanInstance<?>> nonReleasedInstances = new ArrayList<>();
	private final ConcurrentMap<AstrixBeanKey<?>, String> idByExportedBean = new ConcurrentHashMap<>();
	
	
	@AstrixInject
	public void setVersioningPlugin(AstrixVersioningPlugin versioningPlugin) {
		this.versioningPlugin = versioningPlugin;
	}
	
	public List<? extends BoundServiceBeanInstance<?>> getBoundServices() {
		return this.nonReleasedInstances;
	}
	
	
	@Override
	public <T> BoundServiceBeanInstance<T> bind(ServiceVersioningContext versioningContext, Class<T> type, AstrixServiceProperties serviceProperties) {
		String providerName = serviceProperties.getProperty("providerId");
		ServiceProvider<?> serviceProvider = providerById.get(providerName);
		if (serviceProvider == null) {
			throw new IllegalStateException("Cant find provider for with name="  + providerName + " and type=" + type);
		}
		T provider = type.cast(serviceProvider.getProvider(versioningPlugin, versioningContext));
		DirectBoundServiceBeanInstance<T> directServiceBeanInstance = new DirectBoundServiceBeanInstance<T>(provider);
		this.nonReleasedInstances.add(directServiceBeanInstance);
		return directServiceBeanInstance;
	}
	
	@Override
	public AstrixServiceProperties createServiceProperties(String componentSpecificUri) {
		return getServiceProperties(componentSpecificUri);
	}

	@Override
	public String getName() {
		return AstrixServiceComponentNames.DIRECT;
	}
	
	@Override
	public boolean canBindType(Class<?> type) {
		return true;
	}

	public static <T> String register(Class<T> type, T provider) {
		String id = String.valueOf(idGen.incrementAndGet());
		providerById.put(id, new ServiceProvider<T>(id, type, provider));
		return id;
	}
	
	public static <T> String register(Class<T> type, T provider, String id) {
		providerById.put(id, new ServiceProvider<T>(id, type, provider));
		return id;
	}
	
	public static <T> void unregister(String id) {
		providerById.remove(id);
	}
	
	
	private static <T> String register(Class<T> type, T provider, ServiceVersioningContext serverVersioningContext) {
		String id = String.valueOf(idGen.incrementAndGet());
		providerById.put(id, new VersioningAwareServiceProvider<>(id, type, provider, serverVersioningContext));
		return id;
	}
	
	public static <T> AstrixServiceProperties registerAndGetProperties(Class<T> type, T provider) {
		String id = register(type, provider);
		return getServiceProperties(id);
	}
	
	public static AstrixServiceProperties getServiceProperties(String id) {
		AstrixServiceProperties serviceProperties = new AstrixServiceProperties();
		serviceProperties.setProperty("providerId", id);
		serviceProperties.setComponent(AstrixServiceComponentNames.DIRECT);
		ServiceProvider<?> serviceProvider = providerById.get(id);
		if (serviceProvider == null) {
			return serviceProperties; // TODO: Throw exception when no service-provider found. Requires rewrite of two unit-tests.
		}
		serviceProperties.setApi(serviceProvider.getType());
		serviceProperties.setProperty(AstrixServiceProperties.PUBLISHED, "true"); 
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
		return provider.getProvider(AstrixVersioningPlugin.Default.create(), ServiceVersioningContext.nonVersioned());
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
		
		public T getProvider(AstrixVersioningPlugin astrixVersioningPlugin, ServiceVersioningContext clientVersioningContext) {
			return provider;
		}
	}
	
	static class VersioningAwareServiceProvider<T> extends ServiceProvider<T> {
		
		private Class<T> type;
		private T provider;
		private ServiceVersioningContext serverVersioningContext;
		
		public VersioningAwareServiceProvider(String id, Class<T> type, T provider, ServiceVersioningContext serverVersioningContext) {
			super(id, type, provider);
			this.type = type;
			this.provider = provider;
			this.serverVersioningContext = serverVersioningContext;
		}
		
		@Override
		public T getProvider(AstrixVersioningPlugin astrixVersioningPlugin, ServiceVersioningContext clientVersioningContext) {
			if (serverVersioningContext.isVersioned() || clientVersioningContext.isVersioned()) {
				VersionedServiceProviderProxy serializationHandlingProxy = 
						new VersionedServiceProviderProxy(provider, 
														  clientVersioningContext.version(), 
														  astrixVersioningPlugin.create(clientVersioningContext), 
														  astrixVersioningPlugin.create(serverVersioningContext));
				return (T) Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, serializationHandlingProxy);
			}
			return provider;
		}
	}
	
	static class VersionedServiceProviderProxy implements InvocationHandler {
		
		private Object provider;
		private AstrixObjectSerializer serverSerializer;
		private AstrixObjectSerializer clientSerializer;
		private int clientVersion;
		
		public VersionedServiceProviderProxy(Object provider, int clientVersion, AstrixObjectSerializer clientSerializer, AstrixObjectSerializer serverSerializer) {
			this.serverSerializer = serverSerializer;
			this.clientVersion = clientVersion;
			this.provider = provider;
			this.clientSerializer = clientSerializer;
		}
		
		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			Object[] marshalledAndUnmarshalledArgs = new Object[args.length];
			for (int i = 0; i < args.length; i++) {
				// simulate client serialization before sending request over network
				Object serialized = clientSerializer.serialize(args[i], clientVersion);
				// simulate server deserialization after receiving request from network
				Object deserialized = serverSerializer.deserialize(serialized, method.getParameterTypes()[i], clientVersion);
				if (args[i] != null && !args[i].getClass().equals(deserialized.getClass())) {
					throw new IllegalArgumentException("Deserialization of service arguments failed. clientSerializer=" 
								+ clientSerializer.getClass().getName() + " serverSerializer=" + serverSerializer.getClass().getName());
				}
				marshalledAndUnmarshalledArgs[i] = deserialized;
			}
			Object result = method.invoke(provider, marshalledAndUnmarshalledArgs);
			// simulate server serialization before sending response over network
			Object serialized = serverSerializer.serialize(result, clientVersion);
			// simulate client deserialization after receiving response from server.
			Object deserialized = clientSerializer.deserialize(serialized, method.getReturnType(), clientVersion);
			return deserialized;
		}
		
	}

	public Collection<ServiceProvider<?>> listProviders() {
		return providerById.values();
	}

	public void clear(String id) {
		providerById.remove(id);
	}

	@Override
	public <T> void exportService(Class<T> providedApi, T provider, ServiceVersioningContext versioningContext) {
		String id = register(providedApi, provider);
		this.idByExportedBean.put(AstrixBeanKey.create(providedApi), id);
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return true;
	}
	
	@Override
	public boolean supportsAsyncApis() {
		return false;
	}

	@Override
	public <T> AstrixServiceProperties createServiceProperties(Class<T> exportedService) {
		String id = this.idByExportedBean.get(AstrixBeanKey.create(exportedService));
		return getServiceProperties(id);
	}

	public static <T> String registerAndGetUri(Class<T> api, T provider) {
		String id = register(api, provider);
		return getServiceUri(id);
	}
	
	/**
	 * Registers a a provider for a given api and associates it with the given ServiceVersioningContext. <p>
	 * 
	 * Using this method activates serialization/deserialization of service argument/return types. <p>
	 * 
	 * Example:  
	 * 	pingService.ping("my-arg")
	 * 
	 *
	 * 1. Using the ServiceVersioningContext provided by the api (using @AstrixVersioned) the service arguments will
	 *    be serialized.
	 * 2. All arguments will then be deserialized using the serverVersioningContext passed as to this method during registration of the provider
	 * 3. The service is invoked with the arguments returned from step 2.
	 * 4. The return type will be serialized using the serverVersioningContext
	 * 5. The return type will be deserialized using the ServiceVersioningContext provided by the api.
	 * 6. The value is returned.
	 * 
	 * @param api
	 * @param provider
	 * @param serverVersioningContext
	 * @return
	 */
	public static <T> String registerAndGetUri(Class<T> api, T provider, ServiceVersioningContext serverVersioningContext) {
		String id = register(api, provider, serverVersioningContext);
		return getServiceUri(id);
	}
	
	private class DirectBoundServiceBeanInstance<T> implements BoundServiceBeanInstance<T> {

		private final T instance;
		
		public DirectBoundServiceBeanInstance(T instance) {
			this.instance = instance;
		}

		@Override
		public T get() {
			return instance;
		}

		@Override
		public void release() {
			AstrixDirectComponent.this.nonReleasedInstances.remove(this);
		}
		
	}

}