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
package com.avanza.astrix.service.registry.client;

import static com.avanza.astrix.test.util.AstrixTestUtil.isExceptionOfType;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationException;
import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationResult;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistry;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryClient;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryLibraryProvider;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import com.avanza.astrix.test.util.Supplier;


public class AstrixServiceRegistryApiTest {
	
	private static final long UNUSED_LEASE = 10_000L;
	private AstrixServiceRegistryClient serviceRegistryClient;
	private AstrixContext context;
	private InMemoryServiceRegistry fakeServiceRegistry;
	
	@Before
	public void setup() {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 10);
		configurer.registerApiProvider(GreetingApiProvider.class);
		configurer.registerApiProvider(InMemoryServiceRegistryLibraryProvider.class);
		configurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		context = configurer.configure();
		fakeServiceRegistry = (InMemoryServiceRegistry) context.getBean(AstrixServiceRegistry.class);
		serviceRegistryClient = context.getBean(AstrixServiceRegistryClient.class);
	}
	
	@Test
	public void lookupService_serviceAvailableInRegistry_ServiceIsImmediatlyBound() throws Exception {
		final String objectId = AstrixDirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, AstrixDirectComponent.getServiceProperties(objectId), UNUSED_LEASE);
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	@Test
	public void lookupService_ServiceAvailableInRegistryButItsNotPossibleToBindToIt_ServiceIsBoundWhenServiceBecamesAvailable() throws Exception {
		final String objectId = AstrixDirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		final GreetingService dummyService = context.getBean(GreetingService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service not to be registered in registry");
		} catch (ServiceUnavailableException e) {
		}

		serviceRegistryClient.register(GreetingService.class, AstrixDirectComponent.getServiceProperties(objectId), UNUSED_LEASE);
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hello: kalle")));
	}
	
	@Test
	public void serviceIsReboundIfServiceIsMovedInRegistry() throws Exception {
		final String providerId = AstrixDirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, AstrixDirectComponent.getServiceProperties(providerId), UNUSED_LEASE);
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		final String newProviderId = AstrixDirectComponent.register(GreetingService.class, new GreetingServiceImpl("hej: "));
		serviceRegistryClient.register(GreetingService.class, AstrixDirectComponent.getServiceProperties(newProviderId), UNUSED_LEASE);
		
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hej: kalle")));
	}
	
	@Test
	public void whenServiceIsRemovedFromRegistryItShouldStartThrowingServiceUnavailable() throws Exception {
		final String providerId = AstrixDirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, AstrixDirectComponent.getServiceProperties(providerId), UNUSED_LEASE);
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		fakeServiceRegistry.clear(); // Simulate lease expiry
		
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
	interface GreetingApiProvider {
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
