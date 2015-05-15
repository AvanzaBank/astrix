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

import com.avanza.astrix.core.CorrelationId;
import com.avanza.astrix.core.ServiceInvocationException;

public class MissingServiceException extends ServiceInvocationException {

	private static final long serialVersionUID = 1L;
	
	public MissingServiceException(String message) {
		super(UNDEFINED_CORRELATION_ID, message);
	}

	public MissingServiceException(String message, CorrelationId correlationId) {
		super(correlationId, message);
	}

	@Override
	public ServiceInvocationException reCreateOnClientSide(CorrelationId correlationId) {
		return new MissingServiceException(getMessage(), correlationId);
	}

}
