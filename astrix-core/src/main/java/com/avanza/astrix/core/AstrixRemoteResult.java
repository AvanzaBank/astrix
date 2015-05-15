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


public final class AstrixRemoteResult<T> {
	
	private final T result;
	private final ServiceInvocationException exception;
	private final CorrelationId correlationId;

	private AstrixRemoteResult(T result, ServiceInvocationException exception, CorrelationId correlationId) {
		this.result = result;
		this.exception = exception;
		this.correlationId = correlationId;
	}
	
	public static <T> AstrixRemoteResult<T> successful(T result) {
		return new AstrixRemoteResult<>(result, null, null);
	}
	
	public static <T> AstrixRemoteResult<T> failure(ServiceInvocationException exception, CorrelationId correlationId) {
		return new AstrixRemoteResult<>(null, exception, correlationId);
	}
	
	public T getResult() {
		if (hasThrownException()) {
			exception.reThrow(correlationId);
		}
		return result;
	}

	public boolean hasThrownException() {
		return exception != null;
	}

	public static <T> AstrixRemoteResult<T> voidResult() {
		return new AstrixRemoteResult<>(null, null, null);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		@SuppressWarnings("unchecked")
		AstrixRemoteResult<T> other = (AstrixRemoteResult<T>) obj;
		return Objects.equals(result, other.result) && Objects.equals(exception, other.exception);
	}
	
	@Override
	public int hashCode() {
		return Objects.hash(result, exception);
	}

}
