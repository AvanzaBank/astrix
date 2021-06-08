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
package com.avanza.astrix.context;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.avanza.astrix.test.util.AstrixTestUtil.isExceptionOfType;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationException;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationResult;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AstrixServiceTest {
	
	private static final String GREETING_SERVICE_URI = "greetingServiceUri";
	private final TestAstrixConfigurer configurer = new TestAstrixConfigurer();
	private AstrixContextImpl context;

	@BeforeEach
	void setup() {
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 10);
		configurer.registerApiProvider(GreetingApiProvider.class);
		context = (AstrixContextImpl) configurer.configure();
	}
	
	@AfterEach
	void destroy() {
		context.destroy();
	}
	
	@Test
	void lookupService_lookupSuccessful_ServiceIsImmediatelyBound() {
		configurer.set(GREETING_SERVICE_URI, DirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	
	@Test
	void lookupService_lookupFailure_ServiceIsBoundLaterWhenLookupSuccessful() throws Exception {
		final GreetingService dummyService = context.getBean(GreetingService.class);
		
		assertThrows(ServiceUnavailableException.class, () -> dummyService.hello("kalle"), "Expected service lookup to fail");

		configurer.set(GREETING_SERVICE_URI, DirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		assertEventually(serviceInvocationResult(() -> dummyService.hello("kalle"), equalTo("hello: kalle")));
	}
	
	@Test
	void serviceIsBoundToNewProviderWhenServiceIsMoved() throws Exception {
		configurer.set(GREETING_SERVICE_URI, DirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		configurer.set(GREETING_SERVICE_URI, DirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hej: ")));
		
		assertEventually(serviceInvocationResult(() -> dummyService.hello("kalle"), equalTo("hej: kalle")));
	}
	
	@Test
	void whenServiceIsRemovedFromSourceOfLookupItShouldStartThrowingServiceUnavailable() throws Exception {
		configurer.set(GREETING_SERVICE_URI, DirectComponent.registerAndGetUri(GreetingService.class, new GreetingServiceImpl("hello: ")));
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		configurer.removeSetting(GREETING_SERVICE_URI);
		
		assertEventually(serviceInvocationException(() -> dummyService.hello("kalle"), isExceptionOfType(ServiceUnavailableException.class)));
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(1000, 1).check(probe);
	}
	
	@AstrixApiProvider
	public interface GreetingApiProvider {
		@AstrixConfigDiscovery(GREETING_SERVICE_URI)
		@Service
		GreetingService greetingService();
	}
	
	public interface GreetingService {
		String hello(String msg);
	}
	
	public static class GreetingServiceImpl implements GreetingService {
		
		private final String greeting;

		public GreetingServiceImpl(String greeting) {
			this.greeting = greeting;
		}
		
		@Override
		public String hello(String msg) {
			return greeting + msg;
		}
	}

}
