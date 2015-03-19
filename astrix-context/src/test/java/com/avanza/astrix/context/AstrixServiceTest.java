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

import org.apache.log4j.BasicConfigurator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.astrix.test.util.Supplier;

public class AstrixServiceTest {
	
	private static final String GREETING_SERVICE_URI = "greetingServiceUri";
	private AstrixContextImpl context;
	TestAstrixConfigurer configurer = new TestAstrixConfigurer();
	
	static {
		BasicConfigurator.configure();
	}
	
	@Before
	public void setup() {
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 10);
		configurer.registerApiProvider(GreetingApiProvider.class);
		context = (AstrixContextImpl) configurer.configure();
	}
	
	@After
	public void destroy() {
		context.destroy();
	}
	
	@Test
	public void lookupService_lookupSuccessful_ServiceIsImmediatlyBound() throws Exception {
		configurer.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	
	@Test
	public void lookupService_lookupFailure_ServiceIsBoundLaterWhenLookupSuccessful() throws Exception {
		final GreetingService dummyService = context.getBean(GreetingService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service lookup to fail");
		} catch (ServiceUnavailableException e) {
		}

		configurer.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hello: kalle")));
	}
	
	@Test
	public void serviceIsBoundToNewProviderWhenServiceIsMoved() throws Exception {
		configurer.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		configurer.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hej: ")));
		
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hej: kalle")));
	}
	
	@Test
	public void whenServiceIsRemovedFromSourceOfLookupItShouldStartThrowingServiceUnavailable() throws Exception {
		configurer.set(GREETING_SERVICE_URI, AstrixDirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		configurer.removeSetting(GREETING_SERVICE_URI);
		
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
	
	@AstrixApiProvider
	public interface GreetingApiProvider {
		@AstrixConfigLookup(GREETING_SERVICE_URI)
		@Service
		GreetingService greetingService();
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
