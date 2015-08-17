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
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.ft.IsolationStrategy;
import com.google.common.base.Throwables;

public class FaultToleranceThreadIsolationIntegrationTest extends FaultToleranceIntegrationTest {

	private static final long SLEEP_FOR_TIMEOUT = 1100l;
	
	
	@Override
	protected IsolationStrategy isolationStrategy() {
		return IsolationStrategy.THREAD;
	}

	@Test(expected=ServiceUnavailableException.class)
	public void timeoutThrowsServiceUnavailableException() {
		testService().sleep(SLEEP_FOR_TIMEOUT);
	}
	
	@Test
	public void callerStackIsAddedToExceptionOnTimeout() throws Exception {
		try {
			testService().sleep(SLEEP_FOR_TIMEOUT);
			fail("Expected ServiceUnavailableException");
		} catch (ServiceUnavailableException e) {
			assertThat(Throwables.getStackTraceAsString(e), containsString(FaultToleranceThreadIsolationIntegrationTest.class.getName() + ".callerStackIsAddedToException"));
		}
	}
	
}
