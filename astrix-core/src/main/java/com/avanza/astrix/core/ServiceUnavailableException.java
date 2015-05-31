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
 * Exception thrown when a service is unavailable, i.e we failed to invoce the target service. For example if a call to it times out.
 * 
 * @author Kristoffer Erlandsson (krierl)
 */
public class ServiceUnavailableException extends RuntimeException {

	private static final long serialVersionUID = 1L;

	@Deprecated
	public ServiceUnavailableException() {
	}

	public ServiceUnavailableException(String message) {
		super(message);
	}

	public ServiceUnavailableException(String message, Throwable cause) {
		super(message, cause);
	}

	@Deprecated
	public ServiceUnavailableException(Throwable cause) {
		super(cause);
	}
}