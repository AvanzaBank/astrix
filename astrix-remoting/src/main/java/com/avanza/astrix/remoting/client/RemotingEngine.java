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
package com.avanza.astrix.remoting.client;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Optional;

import com.avanza.astrix.core.AstrixRemoteResult;
import com.avanza.astrix.core.CorrelationId;
import com.avanza.astrix.core.RemoteServiceInvocationException;
import com.avanza.astrix.core.ServiceInvocationException;
import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;

import rx.Observable;

public final class RemotingEngine {
	
	// TODO: find suitable name for this abstraction
	
	private final RemotingTransport serviceTransport;
	private final AstrixObjectSerializer objectSerializer;
	private final int apiVersion;
	
	public RemotingEngine(RemotingTransport serviceTransport, AstrixObjectSerializer objectSerializer, int apiVersion) {
		this.serviceTransport = serviceTransport;
		this.objectSerializer = objectSerializer;
		this.apiVersion = apiVersion;
	}

	@SuppressWarnings("unchecked")
	protected final <T> AstrixRemoteResult<T> toRemoteResult(AstrixServiceInvocationResponse response, Type returnType) {
		if (response.isServiceUnavailable()) {
			return AstrixRemoteResult.unavailable(response.getExceptionMsg(), CorrelationId.valueOf(response.getCorrelationId()));
		}
		if (response.hasThrownException()) {
			CorrelationId correlationId = CorrelationId.valueOf(response.getCorrelationId());
			return AstrixRemoteResult.failure(createClientSideException(response, apiVersion), correlationId);
		}
		if (returnType.equals(Void.TYPE) || returnType.equals(Void.class)) {
			return AstrixRemoteResult.voidResult();
		}
		if (isOptionalType(returnType)) {
			return AstrixRemoteResult.successful(restoreOptional(response, returnType));
		}
		T result = unmarshall(response, returnType, apiVersion);
		return AstrixRemoteResult.successful(result);
	}
	
	private <T> T restoreOptional(AstrixServiceInvocationResponse response, Type returnType) {
		if (isNullOptionalReturnValue(response)) {
			return null;
		}
		ParameterizedType optionalType = ParameterizedType.class.cast(returnType);
		Object result = unmarshall(response, optionalType.getActualTypeArguments()[0], apiVersion);

		@SuppressWarnings("unchecked")
		T castedResult = (T) Optional.ofNullable(result);
		return castedResult;
	}

	// Defines whether the service invocation returned the value 'null' (as opposed to Optional.empty()).
	private boolean isNullOptionalReturnValue(AstrixServiceInvocationResponse response) {
		return "true".equals(response.getHeader(AstrixServiceInvocationResponseHeaders.OPTIONAL_RETURN_VALUE_IS_NULL));
	}
	
	private boolean isOptionalType(Type returnType) {
		if (!(returnType instanceof ParameterizedType)) {
			return false;
		}
		ParameterizedType parameterizedType = ParameterizedType.class.cast(returnType);
		Type rawType = parameterizedType.getRawType();
		return rawType.equals(Optional.class);
	}

	protected final Object[] marshall(Object[] elements) {
		if (elements == null) {
			// No argument method
			return new Object[0];
		}
		Object[] result = new Object[elements.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = this.objectSerializer.serialize(elements[i], apiVersion);
		}
		return result;
	}

	private <T> T unmarshall(AstrixServiceInvocationResponse response, Type returnType, int version) {
		return objectSerializer.deserialize(response.getResponseBody(), returnType, version);
	}
	
	protected final ServiceInvocationException createClientSideException(AstrixServiceInvocationResponse response, int version) {
		if (response.getException() != null) {
			ServiceInvocationException exception = objectSerializer.deserialize(response.getException(), 
																		ServiceInvocationException.class, 
																		version);
			return exception;
		} 
		return new RemoteServiceInvocationException(response.getExceptionMsg(), response.getThrownExceptionType());			
	}
	
	final Observable<AstrixServiceInvocationResponse> submitRoutedRequest(AstrixServiceInvocationRequest request, RoutingKey routingKey) {
		return this.serviceTransport.submitRoutedRequest(request, routingKey);
	}
	
	final Observable<List<AstrixServiceInvocationResponse>> submitRoutedRequests(List<RoutedServiceInvocationRequest> requests) {
		return this.serviceTransport.submitRoutedRequests(requests);
	}

	final Observable<List<AstrixServiceInvocationResponse>> submitBroadcastRequest(AstrixServiceInvocationRequest request) {
		return this.serviceTransport.submitBroadcastRequest(request);
	}

	public int partitionCount() {
		return this.serviceTransport.partitionCount();
	}

}
