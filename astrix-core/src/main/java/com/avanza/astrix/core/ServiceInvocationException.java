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
package com.avanza.astrix.core;

import java.util.Objects;

/**
 * Thrown on the client side when the server side service invocation ended with an exception.
 * 
 * This is different from a {@link ServiceUnavailableException} which is thrown when the service
 * invocation never returned a response at all, either because timeout was reached or because
 * the fault-tolerance layer rejected the operation.
 * 
 * If the service-api's throws exceptions as part of an api, then such exceptions should subclass
 * this exception to propagate correctly to the client. All exceptions thrown of other types than
 * ServiceInvocationException will be wrapped by a RemoteServiceInvocationException by a service-implementation
 *  
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public abstract class ServiceInvocationException extends RuntimeException {

	
	public static final CorrelationId UNDEFINED_CORRELACTION_ID = CorrelationId.undefined();
	private static final long serialVersionUID = 1L;
	private final CorrelationId correlationId;
	
	public ServiceInvocationException(CorrelationId correlationId) {
		this.correlationId = Objects.requireNonNull(correlationId);
	}

	public ServiceInvocationException(CorrelationId correlationId, String msg) {
		super(msg);
		this.correlationId = Objects.requireNonNull(correlationId);
	}
	
	public final CorrelationId getCorrelationId() {
		return correlationId;
	}
	
	
	/**
	 * Invoked on the client-side to create a new instance of this exception with a proper
	 * stack-trace containing the actual service call.
	 * 
	 * The correlationId should be passed to the returned ServiceInvocationException
	 * 
	 * @param correlationId
	 * @return
	 */
	public abstract ServiceInvocationException reCreateOnClientSide(CorrelationId correlationId);

	/*
	 * Recreates exception and throws it.
	 */
	void reThrow() {
		throw reCreateOnClientSide(correlationId);
	}

}

