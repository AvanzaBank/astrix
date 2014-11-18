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
package com.avanza.astrix.remoting.server;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.core.AsterixObjectSerializer;
import com.avanza.astrix.remoting.client.AsterixMissingServiceException;
import com.avanza.astrix.remoting.client.AsterixMissingServiceMethodException;
import com.avanza.astrix.remoting.client.AsterixServiceInvocationRequest;
import com.avanza.astrix.remoting.client.AsterixServiceInvocationResponse;
import com.avanza.astrix.remoting.util.MethodSignatureBuilder;
/**
 * Server side component used to invoke exported services. <p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceActivator {
	
	private static final Logger logger = LoggerFactory.getLogger(AsterixServiceActivator.class);
	
	static class PublishedService<T> {

		private final T service;
		private final Map<String, Method> methodBySignature = new HashMap<>();
		private final AsterixObjectSerializer objectSerializer;

		public PublishedService(T service, AsterixObjectSerializer serializer, Class<?>... providedApis) {
			this.service = service;
			this.objectSerializer = serializer;
			for (Class<?> api : providedApis) {
				for (Method m : api.getMethods()) {
					methodBySignature.put(MethodSignatureBuilder.build(m), m);
				}
			}
		}
		
		public T getService() {
			return service;
		}
		
		public AsterixServiceInvocationResponse doInvoke(AsterixServiceInvocationRequest request, int version, String serviceApi) throws Exception {
			String serviceMethodSignature = request.getHeader("serviceMethodSignature");
			Method serviceMethod = methodBySignature.get(serviceMethodSignature);
			if (serviceMethod == null) {
				throw new AsterixMissingServiceMethodException(String.format("Missing service method: service=%s method=%s", serviceApi, serviceMethodSignature));
			}
			
			Object[] arguments = unmarshal(request.getArguments(), serviceMethod.getGenericParameterTypes(), version);
			Object result = serviceMethod.invoke(service, arguments);
			AsterixServiceInvocationResponse invocationResponse = new AsterixServiceInvocationResponse();
			if (!serviceMethod.getReturnType().equals(Void.TYPE)) {
				invocationResponse.setResponseBody(objectSerializer.serialize(result, version));
			}
			return invocationResponse;
		}

		private Object[] unmarshal(Object[] elements, Type[] types, int version) {
			Object[] result = new Object[elements.length];
			for (int i = 0; i < result.length; i++) {
				result[i] = objectSerializer.deserialize(elements[i], types[i], version);
			}
			return result;
		}
		
	}
	
	private final ConcurrentMap<String, PublishedService<?>> serviceByType = new ConcurrentHashMap<>();
	
	public void register(Object provider, AsterixObjectSerializer objectSerializer, Class<?> publishedApi) {
		if (!publishedApi.isAssignableFrom(provider.getClass())) {
			throw new IllegalArgumentException("Provider: " + provider.getClass() + " does not implement: " + publishedApi);
		}
		PublishedService<?> publishedService = new PublishedService<>(provider, objectSerializer, publishedApi);
		this.serviceByType.put(publishedApi.getName(), publishedService);
	}
	
	/**
	 * @param request
	 * @return
	 */
	public AsterixServiceInvocationResponse invokeService(AsterixServiceInvocationRequest request) {
		try {
			int version = Integer.parseInt(request.getHeader("apiVersion"));
			String serviceApi = request.getHeader("serviceApi");
			PublishedService<?> publishedService = this.serviceByType.get(serviceApi);
			if (publishedService == null) {
				throw new AsterixMissingServiceException("No such service: " + serviceApi);
			}
			return publishedService.doInvoke(request, version, serviceApi);
		} catch (Exception e) {
			Throwable exceptionThrownByService = resolveException(e);
			AsterixServiceInvocationResponse invocationResponse = new AsterixServiceInvocationResponse();
			invocationResponse.setExceptionMsg(exceptionThrownByService.getMessage());
			invocationResponse.setCorrelationId(UUID.randomUUID().toString());
			invocationResponse.setThrownExceptionType(exceptionThrownByService.getClass().getName());
			logger.info(String.format("Service invocation ended with exception. request=%s correlationId=%s", request, invocationResponse.getCorrelationId()), exceptionThrownByService);
			return invocationResponse;
		}
	}

	private Throwable resolveException(Exception e) {
		if (e instanceof InvocationTargetException) {
			// Invoked service threw an exception
			return InvocationTargetException.class.cast(e).getTargetException();
		}
		return e;
	}

}