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
package com.avanza.hystrix.multiconfig;

import static org.junit.Assert.*;

import org.junit.Test;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

public class MultiConfigIdTest {

	@Test
	public void equalsHashCodeContract() throws Exception {
		assertTrue(MultiConfigId.create("t").equals(MultiConfigId.create("t")));
		assertFalse(MultiConfigId.create("t").equals(MultiConfigId.create("t2")));
		assertEquals(MultiConfigId.create("t").hashCode(), MultiConfigId.create("t").hashCode());
		assertNotEquals(MultiConfigId.create("t").hashCode(), MultiConfigId.create("t2").hashCode());
	}
	
	@Test
	public void readFrom() throws Exception {
		assertEquals(MultiConfigId.create("t-T1"), MultiConfigId.readFrom(MultiConfigId.create("t-T1").createCommandKey("com.avanza.test.TestServiceApi.TestService")));
		assertEquals(MultiConfigId.create("t-T1"), MultiConfigId.readFrom(MultiConfigId.create("t-T1").createThreadPoolKey("com.avanza.test.TestServiceApi.TestService")));
		assertEquals(MultiConfigId.create("t-T1"), MultiConfigId.readFrom(MultiConfigId.create("t-T1").createCollapserKey("com.avanza.test.TestServiceApi.TestService")));
	}
	
	@Test
	public void decode() throws Exception {
		String name = "com.avanza.test.TestServiceApi.TestService";
		assertEquals(HystrixCommandKey.Factory.asKey(name).name(), MultiConfigId.decode(MultiConfigId.create("t-T1").createCommandKey(name)).name());
		assertEquals(HystrixThreadPoolKey.Factory.asKey(name).name(), MultiConfigId.decode(MultiConfigId.create("t-T1").createThreadPoolKey(name)).name());
		assertEquals(HystrixCollapserKey.Factory.asKey(name).name(), MultiConfigId.decode(MultiConfigId.create("t-T1").createCollapserKey(name)).name());
	}
	
}
