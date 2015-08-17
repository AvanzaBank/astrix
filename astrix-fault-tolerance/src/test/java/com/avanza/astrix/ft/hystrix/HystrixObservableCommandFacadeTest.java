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
package com.avanza.astrix.ft;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;


public class HystrixObservableCommandFacadeTest {
	
	private final String groupKey = UUID.randomUUID().toString();
	private final String commandKey = UUID.randomUUID().toString();
	
	private final Setter commandSettings = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey(groupKey))
			  .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey))
			  .andCommandPropertiesDefaults(com.netflix.hystrix.HystrixCommandProperties.Setter()
					  .withExecutionTimeoutInMilliseconds(25)
					  .withExecutionIsolationSemaphoreMaxConcurrentRequests(1));
	
	
	@Test
	public void underlyingObservableIsWrappedWithFaultTolerance() throws Exception {
		Observable<String> ftFooObs = HystrixObservableCommandFacade.observe(new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				return Observable.just("foo");
			}
		}, commandSettings);

		assertEquals("foo", ftFooObs.toBlocking().first());
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, this.commandKey));
	}
	
	@Test
	public void serviceUnavailableThrownByUnderlyingObservableShouldCountAsFailure() throws Exception {
		Observable<String> ftObservable = HystrixObservableCommandFacade.observe(new Supplier<Observable<String>> () {
			@Override
			public Observable<String> get() {
				return Observable.<String>error(new ServiceUnavailableException(""));
			}
			
		}, commandSettings);
		
		try {
			ftObservable.toBlocking().first();
			fail("Expected service unavailable");
		} catch (ServiceUnavailableException e) {
			// Expcected
		}
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, this.commandKey));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE, this.commandKey));
	}
	
	@Test
	public void normalExceptionsThrownIsTreatedAsPartOfNormalApiFlowAndDoesNotCountAsFailure() throws Exception {
		Observable<String> ftObservable = HystrixObservableCommandFacade.observe(new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				return Observable.error(new MyDomainException());
			}
		}, commandSettings);
		try {
			ftObservable.toBlocking().first();
			fail("All regular exception should be propagated as is from underlying observable");
		} catch (MyDomainException e) {
			// Expcected
		}
		
		// Note that from the perspective of a circuit-breaker an exception thrown
		// by the underlying observable (typically a service call) should not
		// count as failure and therefore not (possibly) trip circuit breaker.
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, this.commandKey));
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE, this.commandKey));
	}
	
	@Test
	public void throwsServiceUnavailableOnTimeouts() throws Exception {
		Supplier<Observable<String>> timeoutCommandSupplier = new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				return Observable.create(new OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> t1) {
						// Simulate timeout by not invoking subscriber
					}
				});
			}
		};
		Observable<String> ftObservable = HystrixObservableCommandFacade.observe(timeoutCommandSupplier, commandSettings);
		try {
			ftObservable.toBlocking().first();
			fail("All ServiceUnavailableException should be thrown on timeout");
		} catch (ServiceUnavailableException e) {
			// Expcected
		}
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, this.commandKey));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.TIMEOUT, this.commandKey));
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SEMAPHORE_REJECTED, this.commandKey));
	}
	@Test
	public void semaphoreRejectedCountsAsFailure() throws Exception {
		Supplier<Observable<String>> timeoutCommandSupplier = new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				return Observable.create(new OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> t1) {
						// Simulate timeout by not invoking subscriber
					}
				});
			}
		};
		Observable<String> ftObservable1 = HystrixObservableCommandFacade.observe(timeoutCommandSupplier, commandSettings);
		Observable<String> ftObservable2 = HystrixObservableCommandFacade.observe(timeoutCommandSupplier, commandSettings);
		
		// No need to subscribe to any of the above Observables. 
		// Execution is started eagerly by HystrixObservableCommandFacade.observe
		
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, this.commandKey));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SEMAPHORE_REJECTED, this.commandKey));
	}
	
	
	@Test
	public void doesNotInvokeSupplierWhenBulkHeadIsFull() throws Exception {
		final AtomicInteger supplierInvocationCount = new AtomicInteger();
		Supplier<Observable<String>> timeoutCommandSupplier = new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				supplierInvocationCount.incrementAndGet();
				return Observable.create(new OnSubscribe<String>() {
					@Override
					public void call(Subscriber<? super String> t1) {
						// Simulate timeout by not invoking subscriber
					}
				});
			}
		};
		Observable<String> ftObservable1 = HystrixObservableCommandFacade.observe(timeoutCommandSupplier, commandSettings);
		final Observable<String> ftObservable2 = HystrixObservableCommandFacade.observe(timeoutCommandSupplier, commandSettings);
		
		// No need to subscribe to any of the above Observables. 
		// Execution is started eagerly by HystrixObservableCommandFacade.observe
		assertEquals(1, supplierInvocationCount.get());
		AstrixTestUtil.serviceInvocationException(new Supplier<String>() {
			@Override
			public String get() {
				return ftObservable2.toBlocking().first();
			}
		}, AstrixTestUtil.isExceptionOfType(ServiceUnavailableException.class));
	}
	
	private int getEventCountForCommand(HystrixRollingNumberEvent hystrixRollingNumberEvent, String commandKey) {
		HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey(commandKey));
		int currentConcurrentExecutionCount = (int) metrics.getCumulativeCount(hystrixRollingNumberEvent);
		return currentConcurrentExecutionCount;
	}
	
	public static class MyDomainException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
}
