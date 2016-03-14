package com.avanza.astrix.beans.rx;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import com.avanza.astrix.beans.core.ReactiveExecutionListener;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import rx.Completable;

/**
 * Created by Daniel Bergholm
 */
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
