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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.ft.CommandSettings;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.service.SimpleService;
import com.avanza.astrix.ft.service.SimpleServiceException;
import com.avanza.astrix.ft.service.SimpleServiceImpl;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.google.common.base.Throwables;
import com.netflix.hystrix.Hystrix;

public class FaultToleranceIntegrationTest {

	private Class<SimpleService> api = SimpleService.class;
	private SimpleService provider = new SimpleServiceImpl();
	private SimpleService testService;
	private HystrixFaultTolerance faultTolerance = new HystrixFaultTolerance();
	private String groupName = UUID.randomUUID().toString();
	private String commandName = UUID.randomUUID().toString();
	
	protected SimpleService testService() {
		return testService;
	}

	@Before
	public void createService() {
		testService = createProxy(api, provider, settings());
	}
	
	@After
	public void after() {
		Hystrix.reset();
	}
	
	private <T> T createProxy(Class<T> type, final T provider, final CommandSettings settings) {
		return ReflectionUtil.newProxy(type, new InvocationHandler() {
			@Override
			public Object invoke(Object proxy, final Method method, final Object[] args) throws Throwable {
				return faultTolerance.execute(() -> ReflectionUtil.invokeMethod(method, provider, args), settings);
			}
		});
	}

	@Test
	public void createWithDefaultSettings() throws Exception {
		SimpleService create = createProxy(api, provider, settings());
		create.echo("");
	}

	@Test
	public void callFtService() {
		assertThat(testService.echo("foo"), is(equalTo("foo")));
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
	
	private void callServiceThrowServiceUnavailable(SimpleService serviceWithFt) throws Exception {
		try {
			serviceWithFt.throwException(TestServiceUnavailableException.class);
		} catch (ServiceUnavailableException e) {
		}
	}
	
	@Test
	public void rejectsWhenPoolIsFull() throws Exception {
		CommandSettings settings = settings();
		settings.setCoreSize(3);
		settings.setMaxQueueSize(-1); // sync queue
//		config.set(AstrixBeanSettings.INITIAL_TIMEOUT.nameFor(AstrixBeanKey.create(SimpleService.class)), "5000");
		settings.setSemaphoreMaxConcurrentRequests(3);
		settings.setInitialTimeoutInMilliseconds(5000);
		SimpleService serviceWithFt = createProxy(api, provider, settings);
		ExecutorService pool = Executors.newCachedThreadPool();
		Collection<R> runners = new ArrayList<R>();
		for (int i = 0; i < 5; i++) {
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
		assertThat(numRejectionErrors, is(2));
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
			assertThat(Throwables.getStackTraceAsString(e), containsString(FaultToleranceIntegrationTest.class.getName() + ".callerStackIsAddedToException"));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceUnavailableException() throws Exception {
		try {
			testService.throwException(TestServiceUnavailableException.class);
			fail("Expected SimpleServiceException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(FaultToleranceIntegrationTest.class.getName() + ".callerStackIsAddedToException"));
		}
	}
	
	public static class TestServiceUnavailableException extends ServiceUnavailableException {

		private static final long serialVersionUID = 1L;

		public TestServiceUnavailableException() {
			super("test-msg");
		}
	}
	
	
	private CommandSettings settings() {
		CommandSettings settings = new CommandSettings();
		settings.setMetricsRollingStatisticalWindowInMilliseconds(2000);
		settings.setCommandName(commandName);
		settings.setGroupName(groupName);
		return settings;
	}
	
}

