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
package com.avanza.astrix.gs;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.gigaspaces.internal.client.cache.SpaceCacheException;


public class GigaSpaceProxyTest {
	
	@Test
	public void proxiesInvocations() throws Exception {
		GigaSpace gigaSpace = Mockito.mock(GigaSpace.class);
		GigaSpace proxied = GigaSpaceProxy.create(gigaSpace);
		
		Mockito.stub(gigaSpace.count(null)).toReturn(21);
		
		assertEquals(21, proxied.count(null));
	}
	
	@Test
	public void wrappsSpaceCacheExceptionsInServiceUnavailableException() throws Exception {
		GigaSpace gigaSpace = Mockito.mock(GigaSpace.class);
		
		Mockito.stub(gigaSpace.count(null)).toThrow(new SpaceCacheException(""));
		GigaSpace proxied = GigaSpaceProxy.create(gigaSpace);
		
		try {
			proxied.count(null);
			fail("Expected ServiceUnavailableException or subclass to be thrown");
		} catch (ServiceUnavailableException e) {
			// Expected
		}
	}

}
