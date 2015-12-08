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
import static org.junit.Assert.fail;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;


public class HystrixCommandFacadeTest {
	
	// TODO: Rename to sync-service invocation test

	private AstrixContext context;
	private Ping ping;
	private PingImpl pingServer = new PingImpl();
	
	@Before
	public void before() {
		context = new TestAstrixConfigurer().enableFaultTolerance(true)
											.registerApiProvider(PingApi.class)
											.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(Ping.class), 250)
											.set(AstrixBeanSettings.CORE_SIZE, AstrixBeanKey.create(Ping.class), 1)
											.set(AstrixThreadPoolProperties.MAX_QUEUE_SIZE, AstrixBeanKey.create(Ping.class), -1) // NO QUEUE
											.set("pingUri", DirectComponent.registerAndGetUri(Ping.class, pingServer))
											.configure();
		ping = context.getBean(Ping.class);
	}
	
	@After
	public void after() {
		context.destroy();
	}
	
	@Test
	public void underlyingObservableIsWrappedWithFaultTolerance() throws Throwable {
		String result = ping.ping("foo");

		assertEquals("foo", result);
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
	}
	
	@Test
	public void serviceUnavailableThrownByUnderlyingObservableShouldCountAsFailure() throws Exception {
		pingServer.setFault(() -> {
			throw new ServiceUnavailableException("");
		});
		try {
			ping.ping("foo");
			fail("Expected service unavailable");
		} catch (ServiceUnavailableException e) {
			// Expcected
		} catch (Throwable e) {
			fail("Expected exception of type ServiceUnavailableException, got: " + e.getClass().getName());
		}
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE));
	}
	
	@Test
	public void normalExceptionsThrownIsTreatedAsPartOfNormalApiFlowAndDoesNotCountAsFailure() throws Throwable {
		pingServer.setFault(() -> {
			throw new MyDomainException();
		});
		try {
			ping.ping("foo");
			fail("Expected service unavailable");
		} catch (MyDomainException e) {
			// Expcected
		} 
		// Note that from the perspective of a circuit-breaker an exception thrown
		// by the underlying service call (typically a service call) should not
		// count as failure and therefore not (possibly) trip circuit breaker.
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.FAILURE));
	}
	
	@Test
	public void throwsServiceUnavailableOnTimeouts() throws Throwable {
		pingServer.setFault(() -> {
			sleep(10_000); //simulate timeout by sleeping
		});
		try {
			ping.ping("foo");
			fail("A ServiceUnavailableException should be thrown on timeout");
		} catch (ServiceUnavailableException e) {
			// Expcected
		}
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.TIMEOUT));
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SEMAPHORE_REJECTED));
	}
	
	@Test
	public void threadPoolRejectedCountsAsFailure() throws Exception {
		pingServer.setFault(() -> {
			sleep(10_000); //simulate timeout by sleeping
		});
		// One of these invocations should be rejected
		CountDownLatch done = new CountDownLatch(1);
		new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (Exception e) {
			}
			done.countDown();
		}).start();
		new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (Exception e) {
			}
			done.countDown();
		}).start();

		done.await(5, TimeUnit.SECONDS);
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.THREAD_POOL_REJECTED));
	}
	
	@Test
	public void doesNotInvokeServiceWhenBulkHeadIsFull() throws Exception {
		AtomicInteger invocationCount = new AtomicInteger();
		CountDownLatch done = new CountDownLatch(1);
		CountDownLatch serverInvocationCompleted = new CountDownLatch(1);
		pingServer.setFault(() -> {
			invocationCount.incrementAndGet();
			try {
				done.await(10, TimeUnit.SECONDS); //simulate timeout by waiting on latch
			} catch (Exception e) {
			}
			serverInvocationCompleted.countDown();
		});
		Thread t1 = new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (ServiceUnavailableException e) {
			}
			done.countDown();
		});
		t1.start();
		Thread t2 = new Thread(() -> {
			try {
				ping.ping("foo");
			} catch (ServiceUnavailableException e) {
			}
			done.countDown();
		});
		t2.start();
		
		if (!done.await(3, TimeUnit.SECONDS)) {
			fail("Expected one ping invocation to be aborted by fault tolerance layer");
		}
		if (!serverInvocationCompleted.await(3, TimeUnit.SECONDS)) {
			fail("Expected server invocation to complete when second service call is aborted");
		}
		assertEquals(1, invocationCount.get());
	}
	
	private static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
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
		String ping(String msg);
	}
	
	public static class PingImpl implements Ping {
		private Runnable simulatedFault = () -> {};
		
		public String ping(String msg) {
			simulatedFault.run();
			return msg;
		}
		public void setFault(Runnable fault) {
			this.simulatedFault = fault;
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
	
}
