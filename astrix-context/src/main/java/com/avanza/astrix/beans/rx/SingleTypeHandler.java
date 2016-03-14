package com.avanza.astrix.beans.rx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.avanza.astrix.beans.core.ReactiveExecutionListener;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import rx.Single;
import rx.subscriptions.Subscriptions;

/**
 * Created by Daniel Bergholm
 */
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
