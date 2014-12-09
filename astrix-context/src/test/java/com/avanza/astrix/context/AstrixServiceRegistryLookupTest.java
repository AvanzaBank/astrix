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
package com.avanza.astrix.context;

import static com.avanza.astrix.test.util.AstrixTestUtil.isExceptionOfType;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationException;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationResult;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.AstrixServiceProvider;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.astrix.test.util.Supplier;


public class AstrixServiceRegistryLookupTest {
	
	private static final String GREETING_SERVICE_URI = "greetingServiceUri";
	private AstrixContext context;
	
	@Before
	public void setup() {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 10);
		configurer.registerApiDescriptor(GreetingApiDescriptor.class);
		context = configurer.configure();
	}
	
	@After
	public void destroy() {
		context.destroy();
	}
	
	@Test
	public void lookupService_serviceAvailableInRegistry_ServiceIsImmediatlyBound() throws Exception {
		context.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	
	@Test
	public void lookupService_ServiceLookupFails_ServiceIsBoundWhenServiceBecamesAvailable() throws Exception {
		final GreetingService dummyService = context.getBean(GreetingService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service lookup to fail");
		} catch (ServiceUnavailableException e) {
		}

		context.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hello: kalle")));
	}
	
	@Test
	public void serviceIsBoundToNewProviderWhenServiceIsMoved() throws Exception {
		context.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		context.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hej: ")));
		
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hej: kalle")));
	}
	
	@Test
	public void whenServiceIsRemovedFromSourceOfLookupItShouldStartThrowingServiceUnavailable() throws Exception {
		context.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		context.removeSetting(GREETING_SERVICE_URI);
		
		assertEventually(serviceInvocationException(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, isExceptionOfType(ServiceUnavailableException.class)));
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(1000, 1).check(probe);
	}
	
	@AstrixConfigLookup(GREETING_SERVICE_URI)
	@AstrixServiceProvider(GreetingService.class)
	public static class GreetingApiDescriptor {
	}
	
	public interface GreetingService {
		String hello(String msg);
	}
	
	public static class GreetingServiceImpl implements GreetingService {
		
		private String greeting;

		public GreetingServiceImpl(String greeting) {
			this.greeting = greeting;
		}
		
		@Override
		public String hello(String msg) {
			return greeting + msg;
		}
	}
	

}
