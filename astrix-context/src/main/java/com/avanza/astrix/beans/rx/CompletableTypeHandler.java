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
import rx.Completable;

public class CompletableTypeHandler implements ReactiveTypeHandlerPlugin<Completable> {
	@Override
	public void subscribe(ReactiveExecutionListener listener, Completable reactiveType) {
		reactiveType.subscribe(listener::onError, () -> listener.onResult(null));
	}

	@Override
	public void completeExceptionally(Throwable error, Completable reactiveType) {
		DelayedCompletable.class.cast(reactiveType).completableFuture.completeExceptionally(error);
	}

	@Override
	public void complete(Object result, Completable reactiveType) {
		DelayedCompletable.class.cast(reactiveType).completableFuture.complete(null);
	}

	@Override
	public Completable newReactiveType() {
		return new DelayedCompletable();
	}

	@Override
	public Class<Completable> reactiveTypeHandled() {
		return Completable.class;
	}

	private static class DelayedCompletable extends Completable {
		private final CompletableFuture<Void> completableFuture;

		private DelayedCompletable(CompletableFuture<Void> completableFuture) {
			super(subscriber -> completableFuture.whenComplete((value, throwable) -> {
				if (throwable == null) {
					subscriber.onCompleted();
				} else {
					Throwable exception = throwable instanceof CompletionException ? throwable.getCause() : throwable;
					subscriber.onError(exception);
				}
			}));
			this.completableFuture = completableFuture;
		}

		public DelayedCompletable() {
			this(new CompletableFuture<>());
		}

	}
}
