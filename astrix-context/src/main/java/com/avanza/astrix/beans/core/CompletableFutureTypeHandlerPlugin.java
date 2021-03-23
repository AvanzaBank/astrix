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

import java.util.concurrent.CompletableFuture;

public class CompletableFutureTypeHandlerPlugin implements ReactiveTypeHandlerPlugin<CompletableFuture<Object>> {

	@Override
	public void subscribe(ReactiveExecutionListener listener, CompletableFuture<Object> reactiveType) {
		reactiveType.whenComplete((result, throwable) -> {
			if (throwable != null) {
				listener.onError(throwable);
			} else {
				listener.onResult(result);
			}
		});
	}

	@Override
	public void completeExceptionally(Throwable error, CompletableFuture<Object> reactiveType) {
		reactiveType.completeExceptionally(error);
	}

	@Override
	public void complete(Object result, CompletableFuture<Object> reactiveType) {
		reactiveType.complete(result);
	}
	
	@Override
	public CompletableFuture<Object> newReactiveType() {
		return new CompletableFuture<Object>();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<CompletableFuture<Object>> reactiveTypeHandled() {
		Class<?> result = CompletableFuture.class;
		return (Class<CompletableFuture<Object>>) result;
	}
}