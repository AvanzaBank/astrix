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
package com.avanza.astrix.remoting.util;

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import rx.Observable;
import rx.functions.Func1;

import com.gigaspaces.async.AsyncFuture;
import com.gigaspaces.async.AsyncFutureListener;
import com.gigaspaces.async.AsyncResult;
import com.gigaspaces.async.internal.DefaultAsyncResult;


public class GsUtilTest {
	
	
	@Test
	public void toObservable_emitsOneEventForSuccesfulResult() throws Exception {
		FakeAsyncFuture<String> future = new FakeAsyncFuture<>();
		Observable<String> observable = GsUtil.toObservable(future);
		future.set("foo");
		List<String> list = observable.toList().toBlocking().first();
		assertEquals(Arrays.asList("foo"), list);
	}
	
	@Test(expected = TestException.class)
	public void toObservable_emitsError() throws Exception {
		FakeAsyncFuture<String> future = new FakeAsyncFuture<>();
		Observable<String> observable = GsUtil.toObservable(future);
		future.setError(new TestException());
		observable.toBlocking().first();
	}
	
	@Test
	public void asyncResultList_ToFlatObservable_EmitsEachResultAsSingleItem() throws Exception {
		Func1<List<AsyncResult<String>>, Observable<String>> listToObservable = GsUtil.asyncResultListToObservable();
		List<AsyncResult<String>> r = Arrays.<AsyncResult<String>>asList(
										new DefaultAsyncResult<String>("foo", null),
										new DefaultAsyncResult<String>("bar", null));
		
		Observable<String> result = Observable.just(r).flatMap(listToObservable);
		assertEquals(Arrays.asList("foo", "bar"), result.toList().toBlocking().first());
	}
	
	@Test	
	public void asyncResultList_ToFlatObservable_Error() throws Exception {
		Func1<List<AsyncResult<String>>, Observable<String>> listToObservable = GsUtil.asyncResultListToObservable();
		List<AsyncResult<String>> r = Arrays.<AsyncResult<String>>asList(
										new DefaultAsyncResult<String>("foo", null),
										new DefaultAsyncResult<String>(null, new TestException()));
		
		Observable<String> result = Observable.just(r).flatMap(listToObservable);
		Iterator<String> resultIterator = result.toBlocking().getIterator();
		resultIterator.next();
		try {
			resultIterator.next();
			fail("Expected TestException to be thrown");
		} catch (TestException e) {
			// expected
		}
	}
	
	public static class TestException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	private static class FakeAsyncFuture<T> implements AsyncFuture<T> {
		private CountDownLatch done = new CountDownLatch(1);
		private volatile T result;
		private volatile AsyncFutureListener<T> listener;
		private volatile Exception executionException;
		
		@Override
		public boolean cancel(boolean mayInterruptIfRunning) {
			return false;
		}

		@Override
		public boolean isCancelled() {
			return false;
		}
		
		public void set(T value) {
			done.countDown();
			result = value;
			notifyListener();
		}

		private void notifyListener() {
			if (listener != null) {
				listener.onResult(new DefaultAsyncResult<T>(result, executionException));
			}
		}
		
		public void setError(Exception value) {
			done.countDown();
			this.executionException = value;
			notifyListener();
		}

		@Override
		public boolean isDone() {
			try {
				return this.done.await(0, TimeUnit.MILLISECONDS);
			} catch (InterruptedException e) {
				return false;
			}
		}

		@Override
		public T get() throws InterruptedException, ExecutionException {
			done.await();
			return getResult();
		}

		@Override
		public T get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
			if (done.await(timeout, unit)) {
				return getResult();
			}
			throw new TimeoutException();
		}
		
		private T getResult() throws ExecutionException {
			if (this.executionException != null) {
				throw new ExecutionException(this.executionException);
			}
			return result;
		}

		@Override
		public void setListener(AsyncFutureListener<T> listener) {
			this.listener = listener;
			if (isDone()) {
				notifyListener();
			}
		}
	}
	
}
