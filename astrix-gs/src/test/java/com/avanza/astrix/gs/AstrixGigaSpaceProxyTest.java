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

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.beans.publish.ApiProvider;
import com.avanza.astrix.beans.publish.SimpleAstrixBeanDefinition;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.ft.BeanFaultTolerance;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.ft.DefaultHystrixCommandNamingStrategy;
import com.avanza.astrix.ft.HystrixBeanFaultToleranceProvider;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.gigaspaces.internal.client.cache.SpaceCacheException;


public class AstrixGigaSpaceProxyTest {
	
	private BeanFaultTolerance faultTolerance;

	@Before
	public void setup() {
		MapConfigSource configSource = new MapConfigSource();
		configSource.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);
		DynamicConfig config = DynamicConfig.create(configSource);
		BeanConfigurations beanConfigurations = new BeanConfigurations();
		beanConfigurations.setConfig(config);
		BeanFaultToleranceFactory factory = new BeanFaultToleranceFactory(new HystrixBeanFaultToleranceProvider(), new DefaultHystrixCommandNamingStrategy(), beanConfigurations);
		factory.setConfig(config);
		faultTolerance = factory.create(new SimpleAstrixBeanDefinition<>(ApiProvider.create("test-provider"), AstrixBeanKey.create(GigaSpace.class)));
	}
	
	@Test
	public void proxiesInvocations() throws Exception {
		GigaSpace gigaSpace = Mockito.mock(GigaSpace.class);
		GigaSpace proxied = AstrixGigaSpaceProxy.create(gigaSpace, faultTolerance, new HystrixCommandSettings());
		
		Mockito.stub(gigaSpace.count(null)).toReturn(21);
		
		assertEquals(21, proxied.count(null));
	}
	
	@Test
	public void wrappsSpaceCacheExceptionsInServiceUnavailableException() throws Exception {
		GigaSpace gigaSpace = Mockito.mock(GigaSpace.class);
		
		Mockito.stub(gigaSpace.count(null)).toThrow(new SpaceCacheException(""));
		GigaSpace proxied = AstrixGigaSpaceProxy.create(gigaSpace, faultTolerance, new HystrixCommandSettings());
		
		try {
			proxied.count(null);
			fail("Expected ServiceUnavailableException or subclass to be thrown");
		} catch (ServiceUnavailableException e) {
			// Expected
		}
	}

}
