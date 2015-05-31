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
package com.avanza.astrix.remoting.client;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import rx.Observable;
import rx.functions.Action1;

class FutureAdapter<T> implements Future<T> {
	
	private final CountDownLatch done = new CountDownLatch(1);
	private volatile T result;
	private volatile Throwable exception;
	
	public FutureAdapter(Observable<T> obs) {
		obs.subscribe(new Action1<T>() {
			@Override
			public void call(T t1) {
				result = t1;
				done.countDown();
			}
		}, new Action1<Throwable>() {

			@Override
			public void call(Throwable t1) {
				exception = t1;
				done.countDown();
			}
			
		});
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
		return done.getCount() == 0;
	}

	@Override
	public T get() throws InterruptedException, ExecutionException {
		done.await();
		return getResult();
	}

	@Override
	public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
		if (!done.await(timeout, unit)) {
			throw new TimeoutException();
		}
		return getResult();
	}

	private T getResult() throws ExecutionException {
		if (exception != null) {
			throw new ExecutionException(exception);
		}
		return result;		
	}
	
}