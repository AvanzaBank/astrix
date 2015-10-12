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

import java.util.Objects;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public abstract class AstrixRemoteResult<T> {
	
	private static class ServiceInvocationExceptionResult<T> extends AstrixRemoteResult<T> {
		private final ServiceInvocationException exception;
		private final CorrelationId correlationId;
		
		public ServiceInvocationExceptionResult(ServiceInvocationException exception, CorrelationId correlationId) {
			this.exception = exception;
			this.correlationId = correlationId;
		}

		public T getResult() {
			exception.reThrow(correlationId);
			return null;
		}
		
		public boolean hasThrownException() {
			return true;
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
			ServiceInvocationExceptionResult<T> other = (ServiceInvocationExceptionResult<T>) obj;
			return Objects.equals(exception, other.exception);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(exception, correlationId);
		}
		
		@Override
		public Exception getThrownException() {
			return this.exception;
		}
		
	}
	
	private static class SuccessfulResult<T> extends AstrixRemoteResult<T> {
		
		private final T result;
		
		public SuccessfulResult(T result) {
			this.result = result;
		}

		public T getResult() {
			return result;
		}
		
		public boolean hasThrownException() {
			return false;
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
			SuccessfulResult<T> other = (SuccessfulResult<T>) obj;
			return Objects.equals(result, other.result);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(result);
		}
		
		@Override
		public Exception getThrownException() {
			return null;
		}
	}
	
	private static class ServiceUnavailableResult<T> extends AstrixRemoteResult<T> {
		
		private final CorrelationId correlationId;
		private final String msg;
		
		public ServiceUnavailableResult(String msg, CorrelationId correlationId) {
			this.msg = msg;
			this.correlationId = correlationId;
		}

		public T getResult() {
			throw getThrownException();
		}
		
		public boolean hasThrownException() {
			return true;
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
			ServiceUnavailableResult<T> other = (ServiceUnavailableResult<T>) obj;
			return Objects.equals(correlationId, other.correlationId) 
					&& Objects.equals(msg, other.msg);
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(msg, correlationId);
		}
		
		@Override
		public ServiceUnavailableException getThrownException() {
			return new ServiceUnavailableException(msg + " correlationId=" + correlationId);
		}
	}
	
	private AstrixRemoteResult() {
		
	}
	public static <T> AstrixRemoteResult<T> voidResult() {
		return new SuccessfulResult<>(null);
	}
	
	public static <T> AstrixRemoteResult<T> successful(T result) {
		return new SuccessfulResult<>(result);
	}
	
	public static <T> AstrixRemoteResult<T> failure(ServiceInvocationException exception, CorrelationId correlationId) {
		return new ServiceInvocationExceptionResult<>(exception, correlationId);
	}
	
	public static <T> AstrixRemoteResult<T> unavailable(String msg, CorrelationId correlationId) {
		return new ServiceUnavailableResult<T>(msg, correlationId);
	}
	
	/**
	 * Returns the result from the underlying service invocation.
	 * 
	 * @throws Exception - If the underlying service invocation threw an exception. 
	 * 
	 * @return
	 */
	public abstract T getResult();

	/**
	 * 
	 * @return
	 */
	public abstract boolean hasThrownException();
	
	/**
	 * @return The exception if the underlying service invocation threw an exception.   
	 */
	public abstract Exception getThrownException();

}
