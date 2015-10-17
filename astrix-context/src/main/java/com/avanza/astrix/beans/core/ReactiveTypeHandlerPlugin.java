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

public interface ReactiveTypeHandlerPlugin<T> {
	/**
	 * Subscribes a ReactiveExecutionListener to a reactive type. 
	 * 
	 * @param listener
	 * @param reactiveType
	 */
	void subscribe(ReactiveExecutionListener listener, T reactiveType);
	
	/**
	 * Completes a reactive execution created using {@link #newReactiveType()} with an error.
	 * 
	 * This method will only be invoked with instances created using {@link #newReactiveType()},
	 * so its safe to downcast the reactiveType argument to the type returned by
	 * {@link #newReactiveType()}
	 * 
	 * @param error
	 * @param reactiveType
	 */
	void completeExceptionally(Throwable error, T reactiveType);
	
	/**
	 * Successfully completes a reactive execution created using newReactiveType with
	 * a given result.
	 * 
	 * @param result
	 * @param reactiveType
	 */
	void complete(Object result, T reactiveType);
	
	T newReactiveType();
	
	Class<T> reactiveTypeHandled();
}
