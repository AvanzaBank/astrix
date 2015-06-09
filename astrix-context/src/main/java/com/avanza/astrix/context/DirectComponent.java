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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.ObjectSerializerDefinition;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.context.versioning.AstrixVersioningPlugin;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(ServiceComponent.class)
public class DirectComponent implements ServiceComponent {
	
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
	public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties) {
		String providerName = serviceProperties.getProperty("providerId");
		ServiceProvider<?> serviceProvider = providerById.get(providerName);
		if (serviceProvider == null) {
			throw new IllegalStateException("Cant find provider for with name="  + providerName + " and type=" + serviceDefinition.getServiceType());
		}
		Object targetProvider = serviceProvider.getProvider(versioningPlugin, serviceDefinition.getObjectSerializerDefinition());
		T provider;
		if (serviceDefinition.getBeanKey().getBeanType().isAssignableFrom(targetProvider.getClass())) {
			provider = serviceDefinition.getServiceType().cast(targetProvider);
		} else {
			provider = createProxy(serviceDefinition.getBeanKey().getBeanType(), targetProvider);
		}
		DirectBoundServiceBeanInstance<T> directServiceBeanInstance = new DirectBoundServiceBeanInstance<T>(provider);
		this.nonReleasedInstances.add(directServiceBeanInstance);
		return directServiceBeanInstance;
	}
	
	private <T> T createProxy(Class<T> proxyApi, final Object targetProvider) {
		return ReflectionUtil.newProxy(proxyApi, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
				try {
					Method targetMethod = targetProvider.getClass().getMethod(method.getName(), method.getParameterTypes());
					if (method.getReturnType().isAssignableFrom(Future.class)) {
						return new DoneFuture(targetMethod.invoke(targetProvider, args));
					}
					return new DoneFuture(targetMethod.invoke(targetProvider, args));
				} catch (NoSuchMethodException e) {
					throw new RuntimeException("Target service does not contain method: " + e.getMessage());
				}
			}
		});
	}
	
	private static final class DoneFuture implements Future<Object>{ 

		private Object result;
		
		public DoneFuture(Object result) {
			this.result = result;
		}

		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}

		@Override
		public boolean isDone() {
			return true;
		}

		@Override
		public Object get() throws InterruptedException, ExecutionException {
			return result;
		}

		@Override
		public Object get(long timeout, TimeUnit unit)
				throws InterruptedException, ExecutionException,
				TimeoutException {
			return result;
		}
	}

	@Override
	public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
		return getServiceProperties(serviceProviderUri);
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
	
	
	private static <T> String register(Class<T> type, T provider, ObjectSerializerDefinition serverSerializerDefinition) {
		String id = String.valueOf(idGen.incrementAndGet());
		providerById.put(id, new VersioningAwareServiceProvider<>(id, type, provider, serverSerializerDefinition));
		return id;
	}
	
	public static <T> ServiceProperties registerAndGetProperties(Class<T> type, T provider) {
		String id = register(type, provider);
		return getServiceProperties(id);
	}
	
	public static ServiceProperties getServiceProperties(String id) {
		ServiceProperties serviceProperties = new ServiceProperties();
		serviceProperties.setProperty("providerId", id);
		serviceProperties.setComponent(AstrixServiceComponentNames.DIRECT);
		ServiceProvider<?> serviceProvider = providerById.get(id);
		if (serviceProvider == null) {
			return serviceProperties; // TODO: Throw exception when no service-provider found. Requires rewrite of two unit-tests.
		}
		serviceProperties.setApi(serviceProvider.getType());
		serviceProperties.setProperty(ServiceProperties.PUBLISHED, "true"); 
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
		return provider.getProvider(AstrixVersioningPlugin.Default.create(), ObjectSerializerDefinition.nonVersioned());
	}
	
	static class ServiceProvider<T> {
		
		private String directId;
		private Class<T> type;
		private T provider;
		
		public ServiceProvider(String directId, Class<T> type, T provider) {
			this.directId = directId;
			this.type = type;
			this.provider = provider;
		}
		
		public String getId() {
			return directId;
		}
		
		public Class<T> getType() {
			return type;
		}
		
		public T getProvider(AstrixVersioningPlugin astrixVersioningPlugin, ObjectSerializerDefinition clientSerializerDefinition) {
			return provider;
		}
	}
	
	static class VersioningAwareServiceProvider<T> extends ServiceProvider<T> {
		
		private Class<T> type;
		private T provider;
		private ObjectSerializerDefinition serverSerializerDefinition;
		
		public VersioningAwareServiceProvider(String id, Class<T> type, T provider, ObjectSerializerDefinition serverSerializerDefinition) {
			super(id, type, provider);
			this.type = type;
			this.provider = provider;
			this.serverSerializerDefinition = serverSerializerDefinition;
		}
		
		@Override
		public T getProvider(AstrixVersioningPlugin astrixVersioningPlugin, ObjectSerializerDefinition serializerDefinition) {
			if (serverSerializerDefinition.isVersioned() || serializerDefinition.isVersioned()) {
				VersionedServiceProviderProxy serializationHandlingProxy = 
						new VersionedServiceProviderProxy(provider, 
														  serializerDefinition.version(), 
														  astrixVersioningPlugin.create(serializerDefinition), 
														  astrixVersioningPlugin.create(serverSerializerDefinition));
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
			Object result = ReflectionUtil.invokeMethod(method, provider, marshalledAndUnmarshalledArgs);
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
	public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition) {
		String id = register(providedApi, provider);
		this.idByExportedBean.put(serviceDefinition.getBeanKey(), id);
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return true;
	}
	
	@Override
	public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> exportedService) {
		String id = this.idByExportedBean.get(exportedService.getBeanKey());
		return getServiceProperties(id);
	}

	public static <T> String registerAndGetUri(Class<T> api, T provider) {
		String id = register(api, provider);
		return getServiceUri(id);
	}
	
	/**
	 * Registers a a provider for a given api and associates it with the given ServiceDefinition. <p>
	 * 
	 * Using this method activates serialization/deserialization of service argument/return types. <p>
	 * 
	 * Example:  
	 * 	pingService.ping("my-arg")
	 * 
	 *
	 * 1. Using the ServiceDefinition provided by the api (using @AstrixVersioned) the service arguments will
	 *    be serialized.
	 * 2. All arguments will then be deserialized using the serverServiceDefinition passed as to this method during registration of the provider
	 * 3. The service is invoked with the arguments returned from step 2.
	 * 4. The return type will be serialized using the serverServiceDefinition
	 * 5. The return type will be deserialized using the ServiceDefinition provided by the api.
	 * 6. The value is returned.
	 * 
	 * @param api
	 * @param provider
	 * @param serverSerializerDefinition
	 * @return
	 */
	public static <T> String registerAndGetUri(Class<T> api, T provider, ObjectSerializerDefinition serverSerializerDefinition) {
		String id = register(api, provider, serverSerializerDefinition);
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
			DirectComponent.this.nonReleasedInstances.remove(this);
		}
		
	}

}