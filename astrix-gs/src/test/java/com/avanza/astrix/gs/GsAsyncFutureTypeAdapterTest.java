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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.avanza.astrix.context.core.AsyncTypeConverterImpl;
import com.gigaspaces.async.AsyncFuture;

import rx.Observable;

public class GsAsyncFutureTypeAdapterTest {
	
	private AsyncTypeConverterImpl asyncTypeConverterImpl = new AsyncTypeConverterImpl(Arrays.asList(new GsAsyncFutureTypeAdapter()));
	
	@Test
	public void observableToFutureAndBackReturnsSameObservableWithoutSubscribingToUnderlyingObservable() throws Exception {
		AtomicBoolean subscribed = new AtomicBoolean(false);
		Observable<Object> observable = Observable.create((s) -> {
			subscribed.set(true);
			s.onCompleted();
		});
		AsyncFuture<Object> future = (AsyncFuture<Object>) asyncTypeConverterImpl.toAsyncType(AsyncFuture.class, observable);
		Observable<Object> convertedObservable = asyncTypeConverterImpl.toObservable(AsyncFuture.class, future);
		assertSame(observable, convertedObservable);
		assertFalse(subscribed.get());
	}
	
	@Test(timeout=1000)
	public void observableToAsyncFuture() throws Exception {
		Observable<Object> observable = Observable.just("foo");
		AsyncFuture<Object> future = (AsyncFuture<Object>) asyncTypeConverterImpl.toAsyncType(AsyncFuture.class, observable);
		assertEquals("foo", future.get());
	}
	
}
