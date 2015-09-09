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
package com.avanza.astrix.context.core;

import static org.junit.Assert.*;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.Test;

import com.avanza.astrix.beans.core.BasicFuture;
import com.avanza.astrix.context.core.AsyncTypeConverterImpl;

import rx.Observable;

public class AsyncTypeConverterTest {
	
	@Test(timeout=2000)
	public void blockingObservableToAsyncFutureShouldNotBlock() throws Exception {
		AsyncTypeConverterImpl asyncTypeConverterImpl = new AsyncTypeConverterImpl(Collections.emptyList());
		final CountDownLatch latch = new CountDownLatch(1);
		Observable<Object> obs = Observable.create((s) -> {
			try {
				if (latch.await(1, TimeUnit.SECONDS)) {
					s.onNext("SUCCESS");
					s.onCompleted();
				} else {
					s.onError(new TimeoutException("TIMEOUT"));
				}
			} catch (Throwable e) {
				s.onError(new TimeoutException("TIMEOUT"));
			} 
		});
		
		
		Future<String> future = (Future<String>)asyncTypeConverterImpl.toAsyncType(Future.class, obs);
		latch.countDown();
		assertEquals("SUCCESS", future.get());
	}
	
	@Test(timeout=2000)
	public void futureToObservableShouldNotBlock() throws Exception {
		AsyncTypeConverterImpl asyncTypeConverterImpl = new AsyncTypeConverterImpl(Collections.emptyList());
		BasicFuture<Object> future = new BasicFuture<>();
		
		Observable<Object> observable = asyncTypeConverterImpl.toObservable(Future.class, future);
		future.set("foo");
		assertEquals("foo", observable.toBlocking().first());
	}

	@Test(timeout=1000)
	public void futureToObservableAndBackToFuture() throws Exception {
		AsyncTypeConverterImpl asyncTypeConverterImpl = new AsyncTypeConverterImpl(Collections.emptyList());
		BasicFuture<Object> future = new BasicFuture<>();
		
		
		Observable<Object> observable = asyncTypeConverterImpl.toObservable(Future.class, future);
		Future<Object> converted= (Future<Object>) asyncTypeConverterImpl.toAsyncType(Future.class, observable);
		future.set("foo");
		assertEquals("foo", converted.get());
	}
}
