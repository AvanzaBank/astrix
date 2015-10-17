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
package com.avanza.astrix.gs;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.avanza.astrix.beans.core.ReactiveExecutionListener;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.internal.DefaultAsyncResult;
/**
 * 
 * @author Elias Lindholm
 *
 */
public class AsyncFutureTypeHandler implements ReactiveTypeHandlerPlugin<AsyncFuture<Object>> {

	@Override
	public void subscribe(ReactiveExecutionListener listener, AsyncFuture<Object> reactiveType) {
		reactiveType.setListener(result -> {
			if (result.getException() != null) {
				listener.onError(result.getException());
			} else {
				listener.onResult(result.getResult());
			}
		});
	}

	@Override
	public void completeExceptionally(Throwable error, AsyncFuture<Object> reactiveType) {
		AsyncFutureImpl.class.cast(reactiveType).setError(error);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void complete(Object result, AsyncFuture<Object> reactiveType) {
		AsyncFutureImpl.class.cast(reactiveType).setResult(result);
	}

	@Override
	public AsyncFuture<Object> newReactiveType() {
		return new AsyncFutureImpl<>();
	}
	
	@Override
	public Class<AsyncFuture<Object>> reactiveTypeHandled() {
		Class<?> class1 = AsyncFuture.class;
		return (Class<AsyncFuture<Object>>) class1;
	}
	
	public static class AsyncFutureImpl<T> implements AsyncFuture<T> {
		
		private final CountDownLatch done = new CountDownLatch(1);
		private volatile T result;
		private volatile Throwable error;
		private volatile FutureListenerNotifier futureListener;
		
		private class FutureListenerNotifier {
			
			private final AsyncFutureListener<T> futureListener;
			private final AtomicBoolean notified = new AtomicBoolean(false);
			
			public FutureListenerNotifier(AsyncFutureListener<T> futureListener) {
				this.futureListener = futureListener;
			}
			
			public void ensureNotified() {
				boolean doNotify = notified.compareAndSet(false, true);
				if (doNotify) {
					futureListener.onResult(new DefaultAsyncResult<T>(result, asException(error)));
				}
			}

			private Exception asException(Throwable error) {
				if (error == null) {
					return null;
				}
				if (error instanceof Exception) {
					return (Exception) error;
				}
				return new RuntimeException(error);
			}
			
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
		
		
		public void setError(Throwable t1) {
			error = t1;
			done.countDown();
			notifyListener();
		}

		public void setResult(T result) {
			this.result = result;
			done.countDown();
			notifyListener();
		}

		private void notifyListener() {
			if (this.futureListener != null) {
				this.futureListener.ensureNotified();
			}
		}

		private T getResult() throws ExecutionException {
			if (error != null) {
				throw new ExecutionException(error);
			}
			return result;		
		}

		/**
		 * Sets a listener for the result of this future. The listener is guaranteed to be invoked exactly
		 * one time when this future completes.
		 * 
		 * If this Future is already completed then the listener will be invoked on the same thread
		 * that invokes this method. Otherwise the listener will be invoked on the
		 * thread that completes the computation of this future.
		 * 
		 * If this method is invoked multiple times, then only the last listener set is guaranteed
		 * to receive a callback with the result.
		 *  
		 *  
		 * @param futureListener
		 */
		@Override
		public void setListener(AsyncFutureListener<T> futureListener) {
			this.futureListener = new FutureListenerNotifier(futureListener);
			if (isDone()) {
				this.futureListener.ensureNotified();
			}
		}
	}

}
