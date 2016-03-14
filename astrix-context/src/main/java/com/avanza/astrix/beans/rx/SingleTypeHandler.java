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
package com.avanza.astrix.beans.rx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.avanza.astrix.beans.core.ReactiveExecutionListener;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import rx.Single;
import rx.subscriptions.Subscriptions;

public class SingleTypeHandler implements ReactiveTypeHandlerPlugin<Single<Object>> {
	@Override
	public void subscribe(ReactiveExecutionListener listener, Single<Object> reactiveType) {
		reactiveType.subscribe(listener::onResult, listener::onError);
	}

	@Override
	public void completeExceptionally(Throwable error, Single<Object> reactiveType) {
		DelayedSingle.class.cast(reactiveType).completableFuture.completeExceptionally(error);
	}

	@Override
	@SuppressWarnings("unchecked")
	public void complete(Object result, Single<Object> reactiveType) {
		DelayedSingle.class.cast(reactiveType).completableFuture.complete(result);
	}

	@Override
	public Single<Object> newReactiveType() {
		return new DelayedSingle<>();
	}

	@Override
	@SuppressWarnings("unchecked")
	public Class<Single<Object>> reactiveTypeHandled() {
		Class<?> type = Single.class;
		return (Class<Single<Object>>) type;
	}

	private static class DelayedSingle<T> extends Single<T> {
		private final CompletableFuture<T> completableFuture;

		private DelayedSingle(CompletableFuture<T> completableFuture) {
			super(subscriber -> subscriber.add(Subscriptions.from(completableFuture.whenComplete((value, throwable) -> {
				if (throwable == null) {
					subscriber.onSuccess(value);
				} else {
					Throwable exception = throwable instanceof CompletionException ? throwable.getCause() : throwable;
					subscriber.onError(exception);
				}
			}))));
			this.completableFuture = completableFuture;
		}

		public DelayedSingle() {
			this(new CompletableFuture<>());
		}

	}
}
