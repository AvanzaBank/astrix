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



/**
 * Thrown on the client side when the server side service invocation ended with an exception.<p>
 * 
 * This is different from a {@link ServiceUnavailableException} which is thrown when the service
 * invocation never returned a response at all, either because timeout was reached or because
 * the fault-tolerance layer rejected the operation.<p>
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
	
	@Deprecated
	public static final CorrelationId UNDEFINED_CORRELATION_ID = CorrelationId.undefined();
	@Deprecated
	public static final CorrelationId UNDEFINED_CORRELACTION_ID = UNDEFINED_CORRELATION_ID;
	
	private static final long serialVersionUID = 1L;
	private volatile CorrelationId correlationId;
	private volatile boolean malformed = false;
	
	public ServiceInvocationException(String msg) {
		super(msg);
		this.correlationId = null;
	}
	
	public ServiceInvocationException() {
		this.correlationId = null;
	}
	
	/**
	 * @param correlationId
	 * @deprecated Use no argument constructor.
	 */
	@Deprecated
	public ServiceInvocationException(CorrelationId correlationId) {
		this.correlationId = null;
	}

	/**
	 * 
	 * @param correlationId
	 * @param msg
	 * @deprecated Use {@link ServiceInvocationException#ServiceInvocationException(String)}
	 */
	@Deprecated
	public ServiceInvocationException(CorrelationId correlationId, String msg) {
		super(msg);
		this.correlationId = null;
	}
	
	@Override
	public String getMessage() {
		/*
		 * Append correlationId to message. Note that correlation
		 * id will only be set when the exception is thrown using
		 * reThrow(CorrelationId)
		 * 
		 */
		String message = super.getMessage();
		if (malformed) {
			return message + " correlationId=" + this.correlationId + " WARNING: THIS EXCEPTION DOES NOT OVERIDE recreateOnClientSide. THEREFORE THE STACK CONTAINING SERVICE INVOCATION CANT BE RESTORED"; 
		}
		if (this.correlationId != null) {
			return message + " correlationId=" + this.correlationId;
		}
		return message;
	}
	
	public final CorrelationId getCorrelationId() {
		return correlationId;
	}
	
	private final void setCorrelationId(CorrelationId correlationId) {
		this.correlationId = correlationId;
	}
	
	/**
	 * Invoked on the client-side to create a new instance of this exception with a proper
	 * stack-trace containing the actual service call.
	 * 
	 * The correlationId should be passed to the returned ServiceInvocationException
	 * 
	 * @param correlationId
	 * @return
	 * @deprecated - Migrate this method by overriding {@link #recreateOnClientSide()} (no argument) and
	 * stop overriding this method. This method will be removed in the future and {@link #recreateOnClientSide()} will 
	 * become abstract.
	 * 
	 * replaced by {@link #recreateOnClientSide()}
	 */
	@Deprecated
	public ServiceInvocationException reCreateOnClientSide(CorrelationId correlationId) {
		return null;
	}
	
	/**
	 * Invoked on the client-side to create a new instance of this exception with a proper
	 * stack-trace containing the actual point of the service call.
	 * 
	 * This method should be overridden by all subclasses. It will become abstract in a future release.
	 * 
	 * @return
	 */
	protected ServiceInvocationException recreateOnClientSide() {
		// TODO: make this method abstract and remove malformed property
		return reCreateOnClientSide(null);
	}

	/*
	 * Recreates exception, sets the correlationId just in time, and throws the exception.
	 */
	void reThrow(CorrelationId correlationId) {
		 ServiceInvocationException serviceInvocationException = recreateOnClientSide();
		 if (serviceInvocationException == null) {
			 serviceInvocationException = this; 
			 serviceInvocationException.malformed = true;
		 }
		 serviceInvocationException.setCorrelationId(correlationId);
		 throw serviceInvocationException;
	}

}

