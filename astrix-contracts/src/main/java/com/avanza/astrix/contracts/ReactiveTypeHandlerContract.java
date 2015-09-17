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
package com.avanza.astrix.contracts;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.context.core.ReactiveExecutionListener;
import com.avanza.astrix.context.core.ReactiveTypeConverter;
import com.avanza.astrix.context.core.ReactiveTypeConverterImpl;
import com.avanza.astrix.context.core.ReactiveTypeHandlerPlugin;

import rx.Observable;
import rx.Subscriber;

public abstract class ReactiveTypeHandlerContract<T> {
	
	private ReactiveTypeConverter reactiveTypeConverter;
	private ReactiveTypeHandlerPlugin<T> reactiveTypeHandler;
	
	@Before
	public void setup() {
		reactiveTypeHandler = newReactiveTypeHandler();
		reactiveTypeConverter = new ReactiveTypeConverterImpl(Arrays.asList(reactiveTypeHandler));
	}
	
	protected abstract ReactiveTypeHandlerPlugin<T> newReactiveTypeHandler();

	@Test(timeout=2000)
	public final void reactiveTypeListenerIsNotifiedAsynchronouslyWhenReactiveExecutionCompletes() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType); // Subscribe after execution completes
		assertFalse(resultSpy.isDone());
		assertNull(resultSpy.error);
		assertNull(resultSpy.lastElement);
		
		reactiveTypeHandler.complete("foo", reactiveType); // Complete reactive execution
		assertTrue(resultSpy.isDone());
		assertEquals("foo", resultSpy.lastElement);
		assertNull(resultSpy.error);
	}
	
	@Test(timeout=2000)
	public final void reactiveTypeListenerIsNotifiedSynchronouslyIfReactiveExecutionAlreadyCompleted() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		reactiveTypeHandler.complete("foo", reactiveType); // Completes reactive execution
		
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType); // Subscribe after execution completes
		
		assertTrue(resultSpy.isDone());
		assertEquals("foo", resultSpy.lastElement);
		assertNull(resultSpy.error);
	}
	
	@Test(timeout=2000)
	public final void reactiveTypeToObservableShouldNotBlock() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		Observable<Object> observable = reactiveTypeConverter.toObservable(reactiveTypeHandler.reactiveTypeHandled(), reactiveType);
		
		ObservableSpy reactiveResultListener = new ObservableSpy();
		observable.subscribe(reactiveResultListener);
		
		assertFalse(reactiveResultListener.isDone());
		
		reactiveTypeHandler.complete("foo", reactiveType);
		
		assertTrue(reactiveResultListener.isDone());
		assertEquals("foo", reactiveResultListener.lastElement);
		assertNull(reactiveResultListener.error);
	}
	
	@Test(timeout=1000)
	public final void reactiveTypeToObservable_CreatedObserverIsSubscribedInConversion() throws Exception {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.create((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext("foo");
			s.onCompleted();
		});
		
		assertEquals(0, sourceSubscriptionCount.get());
		T reactiveType = toCustomReactiveType(emitsFoo);
		assertEquals(1, sourceSubscriptionCount.get());
		
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType);
		
		assertEquals("foo", resultSpy.lastElement);
	}
	
	@Test
	public final void onlySubscribesOneTimeToSourceObservableIfConvertingBackAndForth() throws Exception {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.create((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext("foo");
			s.onCompleted();
		});
		
		T reactiveType = toCustomReactiveType(emitsFoo);
		Observable<Object> reconstructedObservable = (Observable<Object>) reactiveTypeConverter.toObservable(reactiveTypeHandler.reactiveTypeHandled(), reactiveType);
		reactiveType = toCustomReactiveType(reconstructedObservable);
		
		assertEquals(1, sourceSubscriptionCount.get());
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType);
		
		assertEquals("foo", resultSpy.lastElement);
	}
	
	@Test
	public final void notifiesExceptionalResults() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType); // Subscribe after execution completes
		assertFalse(resultSpy.isDone());
		assertNull(resultSpy.error);
		assertNull(resultSpy.lastElement);
		
		reactiveTypeHandler.completeExceptionally(new RuntimeException("foo"), reactiveType); // Complete reactive execution
		assertTrue(resultSpy.isDone());
		assertNull(resultSpy.lastElement);
		assertNotNull(resultSpy.error);
		assertEquals("foo", resultSpy.error.getMessage());
	}

	private T toCustomReactiveType(Observable<Object> emitsFoo) {
		return (T) reactiveTypeConverter.toCustomReactiveType(reactiveType(), emitsFoo);
	}

	private Class<? super T> reactiveType() {
		return this.reactiveTypeHandler.reactiveTypeHandled();
	}
	
	
	private static class ObservableSpy extends Subscriber<Object> {

		private final CountDownLatch done = new CountDownLatch(1);
		private Throwable error;
		private Object lastElement;

		@Override
		public void onCompleted() {
			done.countDown();
		}

		@Override
		public void onError(Throwable e) {
			this.error = e;
		}

		@Override
		public void onNext(Object next) {
			this.lastElement = next;
		}

		public boolean isDone() {
			return done.getCount() == 0;
		}
	}
	
	private static class ReactiveResultSpy implements ReactiveExecutionListener {

		private final CountDownLatch done = new CountDownLatch(1);
		private Throwable error;
		private Object lastElement;

		@Override
		public void onError(Throwable e) {
			this.error = e;
			done.countDown();
		}
		
		@Override
		public void onResult(Object result) {
			this.lastElement = result;
			done.countDown();
		}

		public boolean isDone() {
			return done.getCount() == 0;
		}
	}
	
}
