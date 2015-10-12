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
package com.avanza.astrix.core;

import static org.junit.Assert.*;

import org.junit.Test;

public class AstrixRemoteResultTest {
	
	@Test
	public void successfulResult() throws Exception {
		AstrixRemoteResult<String> result = AstrixRemoteResult.successful("foo");
		
		assertFalse(result.hasThrownException());
		assertEquals("foo", result.getResult());
		assertNull(result.getThrownException());
	}
	
	@Test
	public void serviceUnavailableResult() throws Exception {
		AstrixRemoteResult<String> result = AstrixRemoteResult.unavailable("unavailable", CorrelationId.valueOf("foo"));
		
		assertTrue(result.hasThrownException());
		assertEquals(ServiceUnavailableException.class, result.getThrownException().getClass());
		try {
			result.getResult();
			fail("Expected ServiceUnavailableException");
		} catch (ServiceUnavailableException e) {
		}
	}
	
	@Test
	public void serviceInvocationExceptionResult() throws Exception {
		AstrixRemoteResult<String> result = AstrixRemoteResult.failure(new FakeServiceInvocationException(), CorrelationId.valueOf("foo"));
		
		assertTrue(result.hasThrownException());
		assertEquals(FakeServiceInvocationException.class, result.getThrownException().getClass());
		try {
			result.getResult();
			fail("Expected FakeServiceInvocationException");
		} catch (FakeServiceInvocationException e) {
		}
	}
	
	private static class FakeServiceInvocationException extends ServiceInvocationException {
	}

}
