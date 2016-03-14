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

import static org.junit.Assert.*;

import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import com.avanza.astrix.beans.core.ReactiveExecutionListener;
import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.beans.core.ReactiveTypeConverterImpl;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import org.junit.Before;
import org.junit.Test;
import rx.Observable;
import rx.Subscriber;

public abstract class ReactiveTypeHandlerContract<T> {


	private ReactiveTypeConverter reactiveTypeConverter;
	private ReactiveTypeHandlerPlugin<T> reactiveTypeHandler;
	private String value;

	@Before
	public void setup() {
		value = valueToTest();

		reactiveTypeHandler = newReactiveTypeHandler();
		reactiveTypeConverter = new ReactiveTypeConverterImpl(Arrays.asList(reactiveTypeHandler));
	}

	protected String valueToTest() {
		return "foo";
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
		
		reactiveTypeHandler.complete(value, reactiveType); // Complete reactive execution
		assertTrue(resultSpy.isDone());
		assertEquals(value, resultSpy.lastElement);
		assertNull(resultSpy.error);
	}
	
	@Test(timeout=2000)
	public final void reactiveTypeListenerIsNotifiedSynchronouslyIfReactiveExecutionAlreadyCompleted() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		reactiveTypeHandler.complete(value, reactiveType); // Completes reactive execution
		
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType); // Subscribe after execution completes
		
		assertTrue(resultSpy.isDone());
		assertEquals(value, resultSpy.lastElement);
		assertNull(resultSpy.error);
	}
	
	@Test(timeout=2000)
	public final void reactiveTypeToObservableShouldNotBlock() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		Observable<Object> observable = reactiveTypeConverter.toObservable(reactiveTypeHandler.reactiveTypeHandled(), reactiveType);
		
		ObservableSpy reactiveResultListener = new ObservableSpy();
		observable.subscribe(reactiveResultListener);
		
		assertFalse(reactiveResultListener.isDone());
		
		reactiveTypeHandler.complete(value, reactiveType);
		
		assertTrue(reactiveResultListener.isDone());
		assertEquals(value, reactiveResultListener.lastElement);
		assertNull(reactiveResultListener.error);
	}
	
	@Test(timeout=1000)
	public final void reactiveTypeToObservable_CreatedObserverIsSubscribedInConversion() throws Exception {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.create((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext(value);
			s.onCompleted();
		});
		
		assertEquals(0, sourceSubscriptionCount.get());
		T reactiveType = toCustomReactiveType(emitsFoo);
		assertEquals(1, sourceSubscriptionCount.get());
		
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType);
		
		assertEquals(value, resultSpy.lastElement);
	}
	
	@Test
	public final void onlySubscribesOneTimeToSourceObservableIfConvertingBackAndForth() throws Exception {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.create((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext(value);
			s.onCompleted();
		});
		
		T reactiveType = toCustomReactiveType(emitsFoo);
		Observable<Object> reconstructedObservable = (Observable<Object>) reactiveTypeConverter.toObservable(reactiveTypeHandler.reactiveTypeHandled(), reactiveType);
		reactiveType = toCustomReactiveType(reconstructedObservable);
		
		assertEquals(1, sourceSubscriptionCount.get());
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType);
		
		assertEquals(value, resultSpy.lastElement);
	}
	
	@Test
	public final void notifiesExceptionalResults() throws Exception {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType); // Subscribe after execution completes
		assertFalse(resultSpy.isDone());
		assertNull(resultSpy.error);
		assertNull(resultSpy.lastElement);
		
		reactiveTypeHandler.completeExceptionally(new RuntimeException(value), reactiveType); // Complete reactive execution
		assertTrue(resultSpy.isDone());
		assertNull(resultSpy.lastElement);
		assertNotNull(resultSpy.error);
		assertEquals(value, resultSpy.error.getMessage());
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
