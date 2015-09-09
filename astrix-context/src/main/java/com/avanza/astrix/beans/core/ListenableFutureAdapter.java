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


import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import rx.Observable;

public final class ListenableFutureAdapter<T> implements Future<T> {
	
	private final CountDownLatch done = new CountDownLatch(1);
	private volatile T result;
	private volatile Throwable exception;
	private final Observable<T> obs;
	private final AtomicBoolean subscribed = new AtomicBoolean(false);
	private volatile FutureListenerNotifier futureListener;
	
	private class FutureListenerNotifier {
		
		private final Consumer<FutureResult<T>> futureListener;
		private final AtomicBoolean notified = new AtomicBoolean(false);
		
		public FutureListenerNotifier(Consumer<FutureResult<T>> futureListener) {
			this.futureListener = futureListener;
		}
		
		public void ensureNotified() {
			boolean doNotify = notified.compareAndSet(false, true);
			if (doNotify) {
				futureListener.accept(new FutureResult<T>(result, exception));
			}
		}
		
	}
	
	public ListenableFutureAdapter(Observable<T> obs) {
		this.obs = obs;
	}

	@Override
	public boolean cancel(boolean mayInterruptIfRunning) {
		return false;
	}
	
	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isDone() {
		ensureSubscribed();
		return done.getCount() == 0;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		ensureSubscribed();
		done.await();
		return getResult();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		ensureSubscribed();
		if (!done.await(timeout, unit)) {
			throw new TimeoutException();
		}
		return getResult();
	}
	
	public void setFutureListener(Consumer<FutureResult<T>> futureListener) {
		ensureSubscribed();
		this.futureListener = new FutureListenerNotifier(futureListener);
		if (isDone()) {
			this.futureListener.ensureNotified();
		}
	}
	
	private void ensureSubscribed() {
		boolean doSubscribe = this.subscribed.compareAndSet(false, true);
		if (doSubscribe) {
			obs.subscribe(this::setResult, this::setError);
		}
	}

	private void setError(Throwable t1) {
		exception = t1;
		done.countDown();
		notifyListener();
	}

	private void setResult(T e) {
		result = e;
		done.countDown();
		notifyListener();
	}

	private void notifyListener() {
		if (this.futureListener != null) {
			this.futureListener.ensureNotified();
		}
	}

	private T getResult() throws ExecutionException {
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return result;		
	}

	public Observable<T> asObservable() {
		return this.obs;
	}
	
}