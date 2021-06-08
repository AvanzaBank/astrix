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

import com.avanza.astrix.beans.core.ReactiveExecutionListener;
import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.beans.core.ReactiveTypeConverterImpl;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import rx.Observable;
import rx.Subscriber;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public abstract class ReactiveTypeHandlerContract<T> {
	
	private final ReactiveTypeHandlerPlugin<T> reactiveTypeHandler = newReactiveTypeHandler();
	private final ReactiveTypeConverter reactiveTypeConverter = new ReactiveTypeConverterImpl(singletonList(reactiveTypeHandler));

	protected abstract ReactiveTypeHandlerPlugin<T> newReactiveTypeHandler();

	@Test
	@Timeout(2)
	final void reactiveTypeListenerIsNotifiedAsynchronouslyWhenReactiveExecutionCompletes() {
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
	
	@Test
	@Timeout(2)
	final void reactiveTypeListenerIsNotifiedSynchronouslyIfReactiveExecutionAlreadyCompleted() {
		T reactiveType = reactiveTypeHandler.newReactiveType();

		reactiveTypeHandler.complete("foo", reactiveType); // Completes reactive execution
		
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType); // Subscribe after execution completes
		
		assertTrue(resultSpy.isDone());
		assertEquals("foo", resultSpy.lastElement);
		assertNull(resultSpy.error);
	}
	
	@Test
	@Timeout(2)
	final void reactiveTypeToObservableShouldNotBlock() {
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
	
	@Test
	@Timeout(2)
	final void reactiveTypeToObservable_CreatedObserverIsSubscribedInConversion() {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.unsafeCreate((s) -> {
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
	final void onlySubscribesOneTimeToSourceObservableIfConvertingBackAndForth() {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.unsafeCreate((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext("foo");
			s.onCompleted();
		});
		
		T reactiveType = toCustomReactiveType(emitsFoo);
		Observable<Object> reconstructedObservable = reactiveTypeConverter.toObservable(reactiveTypeHandler.reactiveTypeHandled(), reactiveType);
		reactiveType = toCustomReactiveType(reconstructedObservable);
		
		assertEquals(1, sourceSubscriptionCount.get());
		ReactiveResultSpy resultSpy = new ReactiveResultSpy();
		reactiveTypeHandler.subscribe(resultSpy, reactiveType);
		
		assertEquals("foo", resultSpy.lastElement);
	}
	
	@Test
	final void notifiesExceptionalResults() {
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

	@SuppressWarnings("unchecked")
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
