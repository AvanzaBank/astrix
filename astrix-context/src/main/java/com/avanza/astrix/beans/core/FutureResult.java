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
package com.avanza.astrix.beans.core;
/**
 * Holds the result from the completion of
 * a ListenableFutureAdapter. <p>
 * 
 * @author Elias Lindholm
 *
 * @param <T>
 */
public final class FutureResult<T> {
	
	private final T result;
	private final Throwable exception;
	
	public FutureResult(T result, Throwable exception) {
		this.result = result;
		this.exception = exception;
	}

	/**
	 * If the underlying computation ended with an error, then
	 * this method returns the given Throwable.
	 * 
	 * @return A Throwable if the underlying computation ended with an error, null otherwise
	 */
	public Throwable getException() {
		return exception;
	}
	
	/**
	 * If the underlying computation ended successfully, then
	 * this method returns the result (possibly null).
	 * 
	 * @return
	 */
	public T getResult() {
		return result;
	}

}
