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
package com.avanza.astrix.ft.hystrix;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AssertBlockPoller;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.hystrix.multiconfig.MultiConfigId;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;


public class HystrixObservableCommandFacadeTest {
	
	private final String groupKey = UUID.randomUUID().toString();
	private final String commandKey = UUID.randomUUID().toString();
	private final MultiConfigId multiConfigId = MultiConfigId.create("astrix");
	
	private final Setter commandSettings = Setter.withGroupKey(multiConfigId.createCommandGroupKey(groupKey))
			  .andCommandKey(multiConfigId.createCommandKey(commandKey))
			  .andCommandPropertiesDefaults(com.netflix.hystrix.HystrixCommandProperties.Setter()
					  .withExecutionTimeoutInMilliseconds(25)
					  .withExecutionIsolationSemaphoreMaxConcurrentRequests(1));
	
	private AstrixContext context;
	private Ping ping;
	private PingImpl pingServer = new PingImpl();
	
	@Before
	public void before() throws InterruptedException {
		Hystrix.reset();
		context = new TestAstrixConfigurer().enableFaultTolerance(true)
											.registerApiProvider(PingApi.class)
											.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(Ping.class), 25)
											.set(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS, AstrixBeanKey.create(Ping.class), 1)
											.set("pingUri", DirectComponent.registerAndGetUri(Ping.class, pingServer))
											.configure();
		ping = context.getBean(Ping.class);
		initMetrics(ping);
	}
	
	private void initMetrics(Ping ping) throws InterruptedException {
		// Black hystrix magic here :(
		try {
			ping.ping();
		} catch (Exception e) {
		}
		HystrixFaultToleranceFactory faultTolerance = (HystrixFaultToleranceFactory) AstrixApplicationContext.class.cast(this.context).getInstance(BeanFaultToleranceFactorySpi.class);
		HystrixCommandKey key = faultTolerance.getCommandKey(AstrixBeanKey.create(Ping.class));
		
		HystrixCommandMetrics.getInstance(key).getCumulativeCount(HystrixEventType.SUCCESS);
	}
	
	@After
	public void after() {
		context.destroy();
	}
	
	@Test
	public void underlyingObservableIsWrappedWithFaultTolerance() throws Throwable {
		pingServer.setResult(Observable.just("foo"));
		String result = ping.ping().toBlocking().first();

		assertEquals("foo", result);
		eventually(() -> {
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
		});
	}
	
	@Test
	public void serviceUnavailableThrownByUnderlyingObservableShouldCountAsFailure() throws Exception {
		pingServer.setResult(Observable.<String>error(new ServiceUnavailableException("")));
		try {
			ping.ping().toBlocking().first();
			fail("Expected service unavailable");
		} catch (ServiceUnavailableException e) {
			// Expcected
		}
		eventually(() -> {
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE));
		});
	}
	
	@Test
	public void normalExceptionsThrownIsTreatedAsPartOfNormalApiFlowAndDoesNotCountAsFailure() throws Exception {
		pingServer.setResult(Observable.<String>error(new MyDomainException()));
		try {
			ping.ping().toBlocking().first();
			fail("All regular exception should be propagated as is from underlying observable");
		} catch (MyDomainException e) {
			// Expcected
		}
		
		eventually(() -> {
			// Note that from the perspective of a circuit-breaker an exception thrown
			// by the underlying observable (typically a service call) should not
			// count as failure and therefore not (possibly) trip circuit breaker.
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE));
		});
	}
	
	@Test
	public void throwsServiceUnavailableOnTimeouts() throws Exception {
		pingServer.setResult(Observable.create(new OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> t1) {
				// Simulate timeout by not invoking subscriber
			}
		}));
		try {
			ping.ping().toBlocking().first();
			fail("All ServiceUnavailableException should be thrown on timeout");
		} catch (ServiceUnavailableException e) {
			// Expcected
		}
		eventually(() -> {
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.TIMEOUT));
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
		});
	}
	
	@Test
	public void semaphoreRejectedCountsAsFailure() throws Exception {
		pingServer.setResult(Observable.create(new OnSubscribe<String>() {
			@Override
			public void call(Subscriber<? super String> t1) {
				// Simulate timeout by not invoking subscriber
			}
		}));
		Observable<String> ftObservable1 = ping.ping();
		Observable<String> ftObservable2 = ping.ping();

		// Subscribe to observables, ignore emitted items/errors
		ftObservable1.subscribe((item) -> {}, (exception) -> {});
		ftObservable2.subscribe((item) -> {}, (exception) -> {});

		eventually(() -> {
			assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
			assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
		});
	}
	
	@Test
	public void subscribesEagerlyToCreatedObserver() throws Exception {
		AtomicBoolean subscribed = new AtomicBoolean(false);
		Supplier<Observable<String>> timeoutCommandSupplier = new Supplier<Observable<String>>() {
			@Override
			public Observable<String> get() {
				return Observable.create(t1 -> {
					subscribed.set(true);
				});
			}
		};
		HystrixObservableCommandFacade.observe(timeoutCommandSupplier, commandSettings);
		assertTrue(subscribed.get());
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
		
		ftObservable1.subscribe(); // Ignore
		
		assertEquals(1, supplierInvocationCount.get());
		AstrixTestUtil.serviceInvocationException(() -> ftObservable2.toBlocking().first(), AstrixTestUtil.isExceptionOfType(ServiceUnavailableException.class));
		assertEquals(1, supplierInvocationCount.get());
	}
	
	private int getEventCountForCommand(HystrixRollingNumberEvent hystrixRollingNumberEvent) {
		HystrixFaultToleranceFactory faultTolerance = (HystrixFaultToleranceFactory) AstrixApplicationContext.class.cast(this.context).getInstance(BeanFaultToleranceFactorySpi.class);
		HystrixCommandKey commandKey = faultTolerance.getCommandKey(AstrixBeanKey.create(Ping.class));
		HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(commandKey);
		int currentConcurrentExecutionCount = (int) metrics.getCumulativeCount(hystrixRollingNumberEvent);
		return currentConcurrentExecutionCount;
	}
	
	public static class MyDomainException extends RuntimeException {
		private static final long serialVersionUID = 1L;
	}
	
	public static interface Ping {
		Observable<String> ping();
	}
	
	public static class PingImpl implements Ping {
		private Observable<String> result;
		
		public Observable<String> ping() {
			return result;
		}

		public void setResult(Observable<String> result) {
			this.result = result;
		}
	}
	
	@AstrixApiProvider
	public static class PingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		public Ping ping() {
			return new PingImpl();
		}
		
	}
	
	private void eventually(Runnable assertion) throws InterruptedException {
		new AssertBlockPoller(3000, 25).check(assertion);
	}
	
}
