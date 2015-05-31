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
package com.avanza.astrix.core;


/**
 * Thrown on the client side when the server side service invocation ended with an exception.
 *  
 * @author Elias Lindholm (elilin)
 *
 */
public class RemoteServiceInvocationException extends ServiceInvocationException {
	
	private static final long serialVersionUID = 1L;
	
	private final String exceptionType;
	private final String serviceExceptionMessage;

	/**
	 * 
	 * @param msg - 
	 * @param serviceExceptionMessage - the message from the exception thrown 
	 * @param serviceExceptionType - the type of the thrown exception
	 * @param correlationId - a correlation id to identify the exception in server side logs.
	 */
	public RemoteServiceInvocationException(String serviceExceptionMessage, String serviceExceptionType, CorrelationId correlationId) {
		super(correlationId, "Remote service threw exception, see server log for details. [" + serviceExceptionType + ": " + serviceExceptionMessage + "] correlationId=" + correlationId);
		this.serviceExceptionMessage = serviceExceptionMessage;
		this.exceptionType = serviceExceptionType;
	}
	
	public String getExceptionType() {
		return exceptionType;
	}
	
	public String getServiceExceptionMessage() {
		return serviceExceptionMessage;
	}
	
	@Override
	public ServiceInvocationException reCreateOnClientSide(CorrelationId correlationId) {
		return new RemoteServiceInvocationException(getServiceExceptionMessage(), getExceptionType(), correlationId);
	}
	
}
