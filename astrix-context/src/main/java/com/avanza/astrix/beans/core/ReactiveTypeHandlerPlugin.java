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

import rx.Observable;

/**
 *	Plugin for converting between {@link Observable} and a reactive type
 *
 * @param <T> a reactive or asynchronous type, such as {@link rx.Single Single} or {@link java.util.concurrent.CompletableFuture CompletableFuture}
 */
public interface ReactiveTypeHandlerPlugin<T> {

	/**
	 *  Convert from reactive type {@code T} to {@link Observable}
	 *  <p>
	 *      The returned {@link Observable} should <b>not</b> be already subscribed by the method.
	 *  </p>
	 */
	Observable<Object> toObservable(T reactiveType);

	/**
	 * Convert from {@link Observable} to reactive type {@code T}
	 * <p>
	 * 		The incoming {@link Observable} <b>should</b> get subscribed by this method.
	 * </p>
	 */
	T toReactiveType(Observable<Object> observable);

	Class<T> reactiveTypeHandled();
}
