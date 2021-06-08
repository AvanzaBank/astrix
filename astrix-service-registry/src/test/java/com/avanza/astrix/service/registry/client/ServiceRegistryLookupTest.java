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
package com.avanza.astrix.service.registry.client;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryLibraryProvider;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryServiceProvider;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.DirectComponent;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.avanza.astrix.test.util.AstrixTestUtil.serviceInvocationResult;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class ServiceRegistryLookupTest {
	
	private static final long UNUSED_LEASE = 10_000L;
	private final InMemoryServiceRegistry fakeServiceRegistry = new InMemoryServiceRegistry();
	private ServiceRegistryExporterClient serviceRegistryExporterClient;
	private AstrixContext context;

	@BeforeEach
	void setup() {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL, 10);
		configurer.set(AstrixSettings.SERVICE_REGISTRY_URI, fakeServiceRegistry.getServiceUri());
		configurer.registerApiProvider(GreetingApiProvider.class);
		configurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		configurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		context = configurer.configure();
		serviceRegistryExporterClient = new ServiceRegistryExporterClient(fakeServiceRegistry, "default", "bar");
	}
	
	@Test
	void lookupService_serviceAvailableInRegistry_ServiceIsImmediatelyBound() {
		final String objectId = DirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryExporterClient.register(GreetingService.class, DirectComponent.getServiceProperties(objectId), UNUSED_LEASE);
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	@Test
	void lookupService_ServiceAvailableInRegistryButItsNotPossibleToBindToIt_ServiceIsBoundWhenServiceBecamesAvailable() throws Exception {
		final String objectId = DirectComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		final GreetingService dummyService = context.getBean(GreetingService.class);

		assertThrows(ServiceUnavailableException.class, () -> dummyService.hello("kalle"), "Expected service not to be registered in registry");

		serviceRegistryExporterClient.register(GreetingService.class, DirectComponent.getServiceProperties(objectId), UNUSED_LEASE);
		assertEventually(serviceInvocationResult(() -> dummyService.hello("kalle"), equalTo("hello: kalle")));
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
