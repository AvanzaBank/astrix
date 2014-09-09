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
package se.avanzabank.asterix.ft;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.ft.service.SimpleService;
import se.avanzabank.asterix.ft.service.SimpleServiceImpl;

import com.google.common.base.Throwables;

public class FaultToleranceIntegrationTest {

	private static final long SLEEP_FOR_TIMEOUT = 1100l;
	private Class<SimpleService> api = SimpleService.class;
	private SimpleService provider = new SimpleServiceImpl();

	@Test
	public void callFtService() {
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString(), settingsRandomCommandKey());
		assertThat(serviceWithFt.echo("foo"), is(equalTo("foo")));
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void timeoutThrowsServiceUnavailableException() {
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString(), settingsRandomCommandKey());
		serviceWithFt.sleep(SLEEP_FOR_TIMEOUT);
	}
	
	@Test(expected=TestException.class)
	public void serviceExceptionIsThrown() throws Exception {
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString(), settingsRandomCommandKey());
		serviceWithFt.throwException(TestException.class);
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void circuitBreaker() throws Exception {
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString(), settingsRandomCommandKey());
		// Hystrix needs a service to be invoked at least 20 times in a rolling window of one second for the circuit breaker to open
		for (int i = 0; i < 21; i++) {
			callServiceThrowServiceUnavailable(serviceWithFt);
		}
		Thread.sleep(1200);
		serviceWithFt.echo("foo");
	}
	
	private void callServiceThrowServiceUnavailable(SimpleService serviceWithFt) {
		try {
			serviceWithFt.throwException(ServiceUnavailableException.class);
		} catch (ServiceUnavailableException e) {
		}
	}
	
	// TODO test that service exceptions are not counted towards opening circuit breaker
	
	@Test
	public void rejectsWhenPoolIsFull() throws Exception {
		HystrixCommandSettings settings = settingsRandomCommandKey();
		settings.setCoreSize(3);
		settings.setQueueSizeRejectionThreshold(3);
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString(), settings);
		// For some reason calls are not rejected unless we "warm up" with this call first. Why?
		serviceWithFt.echo("");
		ExecutorService pool = Executors.newCachedThreadPool();
		Collection<R> runners = new ArrayList<R>();
		for (int i = 0; i < 10; i++) {
			R runner = new R(serviceWithFt);
			runners.add(runner);
			pool.execute(runner);
		}
		pool.shutdown();
		pool.awaitTermination(2000, TimeUnit.MILLISECONDS);
		int numRejectionErrors = 0;
		for (R runner : runners) {
			Exception e = runner.getException();
			if (e == null) {
				continue;
			}
			else if (!(e instanceof RejectedExecutionException)) {
				fail("Unexpected exception type: " + e);
			} else {
				numRejectionErrors++;
			}
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
				service.sleep(200);
			} catch (Exception e) {
				e.printStackTrace();
				this.exception  = e;
			}
		}

		public Exception getException() {
			return exception;
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceException() throws Exception {
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString());
		try {
			serviceWithFt.throwException(TestException.class);
			fail("Expected TestException");
		} catch (TestException e) {
			assertThat(Throwables.getStackTraceAsString(e).contains(getClass().getName() + ".callerStackIsAddedToException"), is(true));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnTimeout() throws Exception {
		SimpleService serviceWithFt = HystrixAdapter.create(api , provider, randomString());
		try {
			serviceWithFt.sleep(SLEEP_FOR_TIMEOUT);
			fail("Expected ServiceUnavailableException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e).contains(getClass().getName() + ".callerStackIsAddedToException"), is(true));
		}
	}

	
	@SuppressWarnings("serial")
	public static class TestException extends RuntimeException {
		
	}
	
	private HystrixCommandSettings settingsRandomCommandKey() {
		HystrixCommandSettings settings = new HystrixCommandSettings();
		settings.setCommandKey(randomString());
		return settings;
	}
	
	
	private String randomString() {
		return "" + Math.random();
	}
	
	
}

