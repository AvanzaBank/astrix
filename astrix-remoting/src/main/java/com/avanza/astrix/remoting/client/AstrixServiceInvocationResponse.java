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
package com.avanza.astrix.remoting.client;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceInvocationResponse implements Serializable {

	private static final long serialVersionUID = 1L;
	private Object responseBody;
	/*
	 * If the exception thrown on the server side was of type ServiceInvocationException, 
	 * then this field will be populated. Otherwise only thrownExceptionType and
	 * exceptionMsg will be populated
	 */
	private Object thrownException;
	private String thrownExceptionType;
	private String exceptionMsg;
	private String correlationId;

	private final Map<String, String> headers = new HashMap<>();
	
	public void setResponseBody(Object responseBody) {
		this.responseBody = responseBody;
	}
	
	public Object getResponseBody() {
		return responseBody;
	}
	
	public Object getException() {
		return thrownException;
	}
	
	public void setException(Object exception) {
		this.thrownException = exception;
	}
	
	public void setExceptionMsg(String exceptionMsg) {
		this.exceptionMsg = exceptionMsg;
	}
	
	public String getExceptionMsg() {
		return exceptionMsg;
	}
	
	public void setThrownExceptionType(String thrownException) {
		this.thrownExceptionType = thrownException;
	}
	
	public String getThrownExceptionType() {
		return thrownExceptionType;
	}
	
	public boolean hasThrownException() {
		return this.thrownExceptionType != null || this.thrownException != null;
	}
	
	public void setHeader(String name, String value) {
		this.headers.put(name, value);
	}
	
	public String getHeader(String name) {
		return this.headers.get(name);
	}
	
	public void setCorrelationId(String correlationId) {
		this.correlationId = correlationId;
	}
	
	public String getCorrelationId() {
		return correlationId;
	}

	public Object getThrownException() {
		return thrownException;
	}
	
}
