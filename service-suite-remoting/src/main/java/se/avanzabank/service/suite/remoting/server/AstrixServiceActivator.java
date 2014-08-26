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
package se.avanzabank.service.suite.remoting.server;


import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.service.suite.core.AstrixObjectSerializer;
import se.avanzabank.service.suite.remoting.client.AstrixMissingServiceException;
import se.avanzabank.service.suite.remoting.client.AstrixMissingServiceMethodException;
import se.avanzabank.service.suite.remoting.client.AstrixServiceInvocationRequest;
import se.avanzabank.service.suite.remoting.client.AstrixServiceInvocationResponse;
import se.avanzabank.service.suite.remoting.util.MethodSignatureBuilder;
/**
 * Server side component used to invoke exported services. <p> 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceActivator {
	
	private static final Logger logger = LoggerFactory.getLogger(AstrixServiceActivator.class);
	
	static class PublishedService<T> {

		private final T service;
		private final Map<String, Method> methodBySignature = new HashMap<>();

		public PublishedService(T service, Class<?>... providedApis) {
			this.service = service;
			for (Class<?> api : providedApis) {
				for (Method m : api.getMethods()) {
					methodBySignature.put(MethodSignatureBuilder.build(m), m);
				}
			}
		}
		
		public T getService() {
			return service;
		}
		
	}
	
	private final ConcurrentMap<String, PublishedService<?>> serviceByType = new ConcurrentHashMap<>();
	private final AstrixObjectSerializer objectSerializer;

	public AstrixServiceActivator(AstrixObjectSerializer objectSerializer) {
		this.objectSerializer = objectSerializer;
	}
	
	@Autowired
	public AstrixServiceActivator(AstrixRemotingArgumentSerializerFactory objectSerializerFactory) {
		this.objectSerializer = objectSerializerFactory.create();
	}

	public void register(Object provider, Class<?>... publishedApis) {
		PublishedService<?> publishedService = new PublishedService<>(provider, publishedApis);
		for (Class<?> publishedApi : publishedApis) {
			this.serviceByType.put(publishedApi.getName(), publishedService);
		}
	}
	
	public AstrixServiceInvocationResponse invokeService(AstrixServiceInvocationRequest request) {
		try {
			return doInvoke(request);
		} catch (Exception e) {
			Throwable exceptionThrownByService = resolveException(e);
			AstrixServiceInvocationResponse invocationResponse = new AstrixServiceInvocationResponse();
			invocationResponse.setExceptionMsg(exceptionThrownByService.getMessage());
			invocationResponse.setCorrelationId(UUID.randomUUID().toString()); // TODO: use header instead?
			invocationResponse.setThrownExceptionType(exceptionThrownByService.getClass().getName());
			logger.info("Service invocation ended with exception, correlationId=" + invocationResponse.getCorrelationId(), e);
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

	private AstrixServiceInvocationResponse doInvoke(AstrixServiceInvocationRequest request) throws Exception {
		int version = Integer.parseInt(request.getHeader("apiVersion")); // TODO: allow version to be null?
		String serviceApi = request.getHeader("serviceApi");
		PublishedService<?> publishedService = this.serviceByType.get(serviceApi);
		if (publishedService == null) {
			throw new AstrixMissingServiceException("No such service: " + serviceApi);
		}
		String serviceMethodSignature = request.getHeader("serviceMethodSignature");
		Method serviceMethod = publishedService.methodBySignature.get(serviceMethodSignature);
		if (serviceMethod == null) {
			throw new AstrixMissingServiceMethodException(String.format("Missing service method: service=%s method=%s", serviceApi, serviceMethodSignature));
		}
		
		Object[] arguments = unmarshal(request.getArguments(), serviceMethod.getParameterTypes(), version);
		Object result = serviceMethod.invoke(publishedService.service, arguments);
		AstrixServiceInvocationResponse invocationResponse = new AstrixServiceInvocationResponse();
		if (!serviceMethod.getReturnType().equals(Void.TYPE)) {
			invocationResponse.setResponseBody(objectSerializer.serialize(result, version));
		}
		return invocationResponse;
	}

	private Object[] unmarshal(Object[] elements, Class<?>[] types, int version) {
		Object[] result = new Object[elements.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = objectSerializer.deserialize(elements[i], types[i], version);
		}
		return result;
	}

}