/*
 * Copyright 2014-2015 Avanza Bank AB
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.context.FaultToleranceSpecification;
import com.avanza.astrix.context.IsolationStrategy;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.ft.HystrixAdapter;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.avanza.astrix.ft.service.SimpleService;
import com.avanza.astrix.ft.service.SimpleServiceException;
import com.avanza.astrix.ft.service.SimpleServiceImpl;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.google.common.base.Throwables;

public class FaultToleranceIntegrationTest {

	private static final long SLEEP_FOR_TIMEOUT = 1100l;
	private Class<SimpleService> api = SimpleService.class;
	private SimpleService provider = new SimpleServiceImpl();
	private SimpleService testService;

	@Before
	public void createService() {
		testService = HystrixAdapter.create(specRandomGroup(), settingsRandomCommandKey());
	}
	
	@Test
	public void createWithDefaultSettings() throws Exception {
		SimpleService create = HystrixAdapter.create(specRandomGroup());
		create.echo("");
	}

	@Test
	public void callFtService() {
		assertThat(testService.echo("foo"), is(equalTo("foo")));
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void timeoutThrowsServiceUnavailableException() {
		testService.sleep(SLEEP_FOR_TIMEOUT);
	}
	
	@Test(expected=SimpleServiceException.class)
	public void serviceExceptionIsThrown() throws Exception {
		testService.throwException(SimpleServiceException.class);
	}
	
	@Test
	public void circuitBreakerOpens() throws Exception {
		// Hystrix needs a service to be invoked at least 20 times in a rolling window of one second for the circuit breaker to open
		for (int i = 0; i < 21; i++) {
			callServiceThrowServiceUnavailable(testService);
		}
		assertEventually(serviceException(testService, isA(ServiceUnavailableException.class)));
	}
	
	public void assertEventually(Probe probe) throws Exception {
		new Poller(2000, 100).check(probe);
	}
	
	public Probe serviceException(final SimpleService service, final Matcher<? extends Throwable> matcher) {
		return new Probe() {
			
			private Throwable lastThrown = null;
			
			@Override
			public void sample() {
				try {
					service.echo("foo");
				} catch (Throwable t) {
					lastThrown = t;
				}
			}
			
			@Override
			public boolean isSatisfied() {
				return matcher.matches(lastThrown);
			}
			
			@Override
			public void describeFailureTo(Description description) {
			}
		};
	}
	
	@Test
	public void circuitBreakerDoesNotOpenOnServiceExceptions() throws Exception {
		// Hystrix needs a service to be invoked at least 20 times in the statistical window for the circuit breaker to open
		for (int i = 0; i < 21; i++) {
			try {
				testService.throwException(SimpleServiceException.class);
			} catch (SimpleServiceException e) {
				// Ignore
			}
		}
		// We must sleep for a bit to ensure the window is saved
		Thread.sleep(400);
		try {
			testService.echo("foo");
		} catch (ServiceUnavailableException e) {
			fail("Should not throw ServiceUnavailableException - circuit breaker should be closed");
		}
	}
	
	private void callServiceThrowServiceUnavailable(SimpleService serviceWithFt) {
		try {
			serviceWithFt.throwException(ServiceUnavailableException.class);
		} catch (ServiceUnavailableException e) {
		}
	}
	
	@Test
	public void rejectsWhenPoolIsFull() throws Exception {
		HystrixCommandSettings settings = settingsRandomCommandKey();
		settings.setCoreSize(3);
		settings.setQueueSizeRejectionThreshold(3);
		settings.setExecutionIsolationThreadTimeoutInMilliseconds(5000);
		SimpleService serviceWithFt = HystrixAdapter.create(specRandomGroup(), settings);
		ExecutorService pool = Executors.newCachedThreadPool();
		Collection<R> runners = new ArrayList<R>();
		for (int i = 0; i < 10; i++) {
			R runner = new R(serviceWithFt);
			runners.add(runner);
			// We need a delay between starting the tasks since Hystrix has its own check to see if stuff fits
			// in the queue. This check is done once before tasks are submitted to the executor. If we start a lot
			// of calls in parallel they will all pass that check since none of them has been queued yet.
			Thread.sleep(50);
			pool.execute(runner);
		}
		pool.shutdown();
		pool.awaitTermination(4000, TimeUnit.MILLISECONDS);
		int numRejectionErrors = 0;
		for (R runner : runners) {
			Exception e = runner.getException();
			if (e == null) {
				continue;
			}
			numRejectionErrors++;
		}
		assertThat(numRejectionErrors, is(4));
	}
	
	private static class R implements Runnable {

		private SimpleService service;
		private volatile Exception exception;

		public R(SimpleService service) {
			this.service = service;
		}
		
		@Override
		public void run() {
			try {
				service.sleep(1000);
			} catch (ServiceUnavailableException e) {
				this.exception  = e;
			}
		}

		public Exception getException() {
			return exception;
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceException() throws Exception {
		try {
			testService.throwException(SimpleServiceException.class);
			fail("Expected SimpleServiceException");
		} catch (SimpleServiceException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(getClass().getName() + ".callerStackIsAddedToException"));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceUnavailableException() throws Exception {
		try {
			testService.throwException(ServiceUnavailableException.class);
			fail("Expected SimpleServiceException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(getClass().getName() + ".callerStackIsAddedToException"));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnTimeout() throws Exception {
		try {
			testService.sleep(SLEEP_FOR_TIMEOUT);
			fail("Expected ServiceUnavailableException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(getClass().getName() + ".callerStackIsAddedToException"));
		}
	}
	
	private HystrixCommandSettings settingsRandomCommandKey() {
		HystrixCommandSettings settings = new HystrixCommandSettings();
		settings.setCommandKey(randomString());
		settings.setMetricsRollingStatisticalWindowInMilliseconds(2000);
		return settings;
	}
	
	private FaultToleranceSpecification<SimpleService> specRandomGroup() {
		return FaultToleranceSpecification.builder(api).provider(provider).group(randomString()).isolationStrategy(IsolationStrategy.THREAD).build();
	}
	
	private String randomString() {
		return "" + Math.random();
	}
	
	
}

