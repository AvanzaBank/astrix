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

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import com.google.common.base.Throwables;

import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.ft.plugin.HystrixFaultTolerancePlugin;
import se.avanzabank.asterix.ft.service.SimpleService;
import se.avanzabank.asterix.ft.service.SimpleServiceImpl;

public class FaultToleranceIntegrationTest {

	private HystrixFaultTolerancePlugin plugin = new HystrixFaultTolerancePlugin();
	private Class<SimpleService> api = SimpleService.class;
	private SimpleService provider = new SimpleServiceImpl();

	@Test
	public void callFtService() {
		SimpleService serviceWithFt = plugin.addFaultTolerance(api , provider, "test");
		assertThat(serviceWithFt.echo("foo"), is(equalTo("foo")));
	}
	
	@Test(expected=ServiceUnavailableException.class)
	public void timeoutThrowsServiceUnavailableException() {
		SimpleService serviceWithFt = plugin.addFaultTolerance(api , provider, "test");
		serviceWithFt.sleep(5000l);
	}
	
	@Test(expected=TestException.class)
	public void serviceExceptionIsThrown() throws Exception {
		SimpleService serviceWithFt = plugin.addFaultTolerance(api , provider, "test");
		serviceWithFt.throwException(TestException.class);
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnServiceException() throws Exception {
		SimpleService serviceWithFt = plugin.addFaultTolerance(api , provider, "test");
		try {
			serviceWithFt.throwException(TestException.class);
			fail("Expected TestException");
		} catch (TestException e) {
			assertThat(Throwables.getStackTraceAsString(e).contains(getClass().getName() + ".callerStackIsAddedToException"), is(true));
		}
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnTimeout() throws Exception {
		SimpleService serviceWithFt = plugin.addFaultTolerance(api , provider, "test");
		try {
			serviceWithFt.sleep(5000l);
			fail("Expected ServiceUnavailableException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e).contains(getClass().getName() + ".callerStackIsAddedToException"), is(true));
		}
	}

	
	@SuppressWarnings("serial")
	public static class TestException extends RuntimeException {
		
	}
}

