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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.BasicFuture;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Library;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

import rx.Observable;

public class HystrixFaulttoleranceIntegrationTest {
	
	private static final AtomicInteger counter = new AtomicInteger(0);
	
	private AstrixApplicationContext context;
	private TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
	private Ping ping;
	
	@Before
	public void setup() {
		counter.incrementAndGet();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.registerApiProvider(CorruptPingApiProvider.class);
		astrixConfigurer.enableFaultTolerance(true);
		context = (AstrixApplicationContext) astrixConfigurer.configure();
		ping = context.getBean(Ping.class);
	}
	
	@Test
	public void usesHystrixFaultToleranceProxyProviderPluginToApplyFaultToleranceToLibraries() throws Exception {
		assertEquals(0, getAppliedFaultToleranceCount(Ping.class));

		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(2, getAppliedFaultToleranceCount(Ping.class));
	}
	
	@Test
	public void observable() throws Exception {
		assertEquals(0, getAppliedFaultToleranceCount(Ping.class));
		assertEquals("foo", ping.observePing("foo").toBlocking().first());
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));
	}

	
	/*
	 * The following three tests test core abstractions in com.avanza.astrix.ft but uses the
	 * hystrix-implementation to get a full integration test of the desired behavior.
	 * (The desired behaviour is that the consumer should be protected from poorly designed code
	 *  which might block despite returning Future/Observable types.
	 */
	
	
	@Test(timeout = 2000)
	public void usesThreadIsolationByDefaultForFutureReturnTypes() throws Exception {
		CorruptPing ping = context.getBean(CorruptPing.class);
		for (int i = 0; i < 100; i++) {
			try {
				ping.foreverBlockingQueue("foo");
			} catch (ServiceUnavailableException e) {
			}
		}
	}
	

	
	@Test(timeout = 2000)
	public void excpetionsOfTypesOtherThanServiceUnavailableExceptionDoesNotCountAsFailure() throws Exception {
		CorruptPing ping = context.getBean(CorruptPing.class);
		for (int i = 0; i < 100; i++) {
			try {
				ping.foreverBlockingQueue("foo");
			} catch (ServiceUnavailableException e) {
			}
		}
	}
	
	@DefaultBeanSettings(initialTimeout=10)
	public interface CorruptPing {
		Observable<String> blockingObserve(String foo);
		Future<String> foreverBlockingQueue(String foo);
		Future<String> neverEndingFuture(String foo);
		
	}
	
	public static class CorruptPingImpl implements CorruptPing {
		private final CountDownLatch countDownLatch = new CountDownLatch(1);
		public Observable<String> blockingObserve(final String msg) {
			block();
			return Observable.just(msg);
		}
		private void block() {
			try {
				countDownLatch.await(); // Simulate blocking construction of observable
			} catch (InterruptedException e) {
			}
		}
		@Override
		public Future<String> foreverBlockingQueue(String msg) {
			block();
			return new BasicFuture<String>(msg);
		}
		@Override
		public Future<String> neverEndingFuture(String msg) {
			return new BasicFuture<>(); // Never set result on future
		}
	}
	
	private int getAppliedFaultToleranceCount(Class<?> beanType) {
		HystrixCommandKey commandKey = getFaultTolerance(context).getCommandKey(AstrixBeanKey.create(beanType));
		return getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, commandKey);
	}
	
	private static HystrixFaultTolerance getFaultTolerance(AstrixContext astrixContext) {
		FaultToleranceSpi ftStrategy = AstrixApplicationContext.class.cast(astrixContext).getInstance(FaultToleranceSpi.class);
		assertEquals(HystrixFaultTolerance.class, ftStrategy.getClass());
		return (HystrixFaultTolerance) ftStrategy;
	}
	
	private int getEventCountForCommand(HystrixRollingNumberEvent hystrixRollingNumberEvent, HystrixCommandKey commandKey) {
		HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(commandKey);
		if (metrics == null) {
			return 0;
		}
		int currentConcurrentExecutionCount = (int) metrics.getCumulativeCount(hystrixRollingNumberEvent);
		return currentConcurrentExecutionCount;
	}
	
	public interface Ping {
		String ping(String msg);
		Observable<String> observePing(String msg);
	}
	
	public static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
		@Override
		public Observable<String> observePing(String msg) {
			return Observable.just(msg);
		}
	}
	
	@AstrixApiProvider
	public static class PingApiProvider {
		@AstrixFaultToleranceProxy
		@Library
		public Ping ping() {
			return new PingImpl();
		}
	}
	
	@AstrixApiProvider
	public static class CorruptPingApiProvider {
		@AstrixFaultToleranceProxy
		@Library
		public CorruptPing observablePing() {
			return new CorruptPingImpl();
		}
	}
	
}
