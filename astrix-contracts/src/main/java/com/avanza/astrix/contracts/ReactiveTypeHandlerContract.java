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

import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.beans.core.ReactiveTypeConverterImpl;
import com.avanza.astrix.beans.core.ReactiveTypeHandlerPlugin;
import org.junit.Test;
import rx.Observable;
import rx.observers.TestSubscriber;
import rx.subjects.ReplaySubject;

import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;

public abstract class ReactiveTypeHandlerContract<T> {

	private final ReactiveTypeConverter reactiveTypeConverter;
	private final Class<T> reactiveTypeHandled;

	protected ReactiveTypeHandlerContract(ReactiveTypeHandlerPlugin<T> reactiveTypeHandler) {
		this.reactiveTypeConverter = new ReactiveTypeConverterImpl(singletonList(reactiveTypeHandler));
		this.reactiveTypeHandled = reactiveTypeHandler.reactiveTypeHandled();
	}

	@SuppressWarnings("SameParameterValue")
	protected <V> void assertValue(TestSubscriber<V> testSubscriber, V value) {
		testSubscriber.assertValue(value);
	}
	
	@Test(timeout = 2000)
	public final void reactiveTypeListenerIsNotifiedAsynchronouslyWhenReactiveExecutionCompletes() throws Exception {
		ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
		T reactiveType = toCustomReactiveType(subject);
		Observable<Object> observable = toObservable(reactiveType);

		TestSubscriber<Object> resultSpy = TestSubscriber.create();
		observable.subscribe(resultSpy); // Subscribe after execution completes
		resultSpy.assertNotCompleted();
		resultSpy.assertNoErrors();
		resultSpy.assertNoValues();

		subject.onNext("foo");
		subject.onCompleted();  // Complete reactive execution
		resultSpy.assertCompleted();
		assertValue(resultSpy, "foo");
		resultSpy.assertNoErrors();
	}
	
	@Test(timeout=2000)
	public final void reactiveTypeListenerIsNotifiedSynchronouslyIfReactiveExecutionAlreadyCompleted() throws Exception {
		ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
		T reactiveType = toCustomReactiveType(subject);
		Observable<Object> observable = toObservable(reactiveType);

		subject.onNext("foo");
		subject.onCompleted();  // Completes reactive execution

		TestSubscriber<Object> resultSpy = TestSubscriber.create();
		observable.subscribe(resultSpy); // Subscribe after execution completes
		
		resultSpy.assertCompleted();
		assertValue(resultSpy, "foo");
		resultSpy.assertNoErrors();
	}
	
	@Test(timeout=2000)
	public final void reactiveTypeToObservableShouldNotBlock() throws Exception {
		ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
		T reactiveType = toCustomReactiveType(subject);
		Observable<Object> observable = toObservable(reactiveType);

		TestSubscriber<Object> resultSpy = TestSubscriber.create();
		observable.subscribe(resultSpy);
		
		resultSpy.assertNotCompleted();
		
		subject.onNext("foo");
		subject.onCompleted();

		resultSpy.assertCompleted();
		assertValue(resultSpy, "foo");
		resultSpy.assertNoErrors();
	}
	
	@Test(timeout=1000)
	public final void reactiveTypeToObservable_CreatedObserverIsSubscribedInConversion() throws Exception {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.unsafeCreate((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext("foo");
			s.onCompleted();
		});
		
		assertEquals(0, sourceSubscriptionCount.get());
		T reactiveType = toCustomReactiveType(emitsFoo);
		assertEquals(1, sourceSubscriptionCount.get());

		TestSubscriber<Object> resultSpy = TestSubscriber.create();
		toObservable(reactiveType).subscribe(resultSpy);
		
		assertValue(resultSpy, "foo");
	}
	
	@Test
	public final void onlySubscribesOneTimeToSourceObservableIfConvertingBackAndForth() throws Exception {
		AtomicInteger sourceSubscriptionCount = new AtomicInteger(0);
		Observable<Object> emitsFoo = Observable.unsafeCreate((s) -> {
			sourceSubscriptionCount.incrementAndGet();
			s.onNext("foo");
			s.onCompleted();
		});
		
		T reactiveType = toCustomReactiveType(emitsFoo);
		Observable<Object> reconstructedObservable = toObservable(reactiveType);
		reactiveType = toCustomReactiveType(reconstructedObservable);
		
		assertEquals(1, sourceSubscriptionCount.get());
		TestSubscriber<Object> resultSpy = TestSubscriber.create();
		toObservable(reactiveType).subscribe(resultSpy);
		
		assertValue(resultSpy, "foo");
	}
	
	@Test
	public final void notifiesExceptionalResults() throws Exception {
		ReplaySubject<Object> subject = ReplaySubject.createWithSize(1);
		T reactiveType = toCustomReactiveType(subject);
		Observable<Object> observable = toObservable(reactiveType);

		TestSubscriber<Object> resultSpy = TestSubscriber.create();
		observable.subscribe(resultSpy); // Subscribe after execution completes
		resultSpy.assertNotCompleted();
		resultSpy.assertNoErrors();
		resultSpy.assertNoValues();

		RuntimeException error = new RuntimeException("foo");
		subject.onError(error); // Complete reactive execution
		resultSpy.assertTerminalEvent();
		resultSpy.assertNoValues();
		resultSpy.assertError(error);
	}

	private Observable<Object> toObservable(T reactiveType) {
		return reactiveTypeConverter.toObservable(reactiveTypeHandled, reactiveType);
	}

	private T toCustomReactiveType(Observable<Object> emitsFoo) {
		return reactiveTypeConverter.toCustomReactiveType(reactiveTypeHandled, emitsFoo);
	}

	
	
}
