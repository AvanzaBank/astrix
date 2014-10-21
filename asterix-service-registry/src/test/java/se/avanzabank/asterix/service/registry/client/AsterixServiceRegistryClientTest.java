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
package se.avanzabank.asterix.service.registry.client;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static se.avanzabank.asterix.test.util.AsterixTestUtil.serviceInvocationException;
import static se.avanzabank.asterix.test.util.AsterixTestUtil.serviceInvocationResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Test;

import se.avanzabank.asterix.context.AsterixContext;
import se.avanzabank.asterix.context.AsterixSettings;
import se.avanzabank.asterix.context.TestAsterixConfigurer;
import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.provider.library.AsterixExport;
import se.avanzabank.asterix.provider.library.AsterixLibraryProvider;
import se.avanzabank.asterix.service.registry.app.ServiceKey;
import se.avanzabank.asterix.service.registry.server.AsterixServiceRegistryEntry;
import se.avanzabank.asterix.test.util.Poller;
import se.avanzabank.asterix.test.util.Probe;
import se.avanzabank.asterix.test.util.Supplier;


public class AsterixServiceRegistryClientTest {
	
	private static final long UNUSED_LEASE = 10_000L;
	private AsterixDirectComponent directComponent;
	private AsterixServiceRegistryClient serviceRegistryClient;
	private AsterixContext context;
	private InMemoryServiceRegistry fakeServiceRegistry;
	
//	static {
//		// TODO: remove debugging information
//		BasicConfigurator.configure();
//		Logger.getRootLogger().setLevel(Level.WARN);
//		Logger.getLogger("se.avanzabank.asterix").setLevel(Level.DEBUG);
//	}

	@Before
	public void setup() {
		TestAsterixConfigurer configurer = new TestAsterixConfigurer();
		configurer.set(AsterixSettings.BEAN_REBIND_ATTEMPT_INTERVAL, 10);
		configurer.set(AsterixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 10);
		configurer.registerApiDescriptor(GreetingApiDescriptor.class);
		configurer.registerApiDescriptor(InMemoryServiceRegistryDescriptor.class);
		configurer.registerApiDescriptor(AsterixServiceRegistryLibrary.class);
		context = configurer.configure();
		directComponent = context.getPluginInstance(AsterixDirectComponent.class);
		fakeServiceRegistry = (InMemoryServiceRegistry) context.getBean(AsterixServiceRegistry.class);
		serviceRegistryClient = context.getBean(AsterixServiceRegistryClient.class);
	}
	
	@Test
	public void lookupService_serviceAvailableInRegistry_ServiceIsImmediatlyBound() throws Exception {
		final String objectId = directComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, directComponent.getServiceProperties(objectId), UNUSED_LEASE);
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	@Test
	public void lookupService_ServiceAvailableInRegistryButItsNotPossibleToBindToIt_ServiceIsBoundWhenServiceBecamesAvailable() throws Exception {
		final String objectId = directComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		final GreetingService dummyService = context.getBean(GreetingService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service not to be registered in registry");
		} catch (ServiceUnavailableException e) {
		}

		serviceRegistryClient.register(GreetingService.class, directComponent.getServiceProperties(objectId), UNUSED_LEASE);
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hello: kalle")));
	}
	
	@Test
	public void serviceIsReboundIfServiceIsMovedInRegistry() throws Exception {
		final String providerId = directComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, directComponent.getServiceProperties(providerId), UNUSED_LEASE);
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		final String newProviderId = directComponent.register(GreetingService.class, new GreetingServiceImpl("hej: "));
		serviceRegistryClient.register(GreetingService.class, directComponent.getServiceProperties(newProviderId), UNUSED_LEASE);
		
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hej: kalle")));
	}
	
	@Test
	public void whenServiceIsRemovedFromRegistryItShouldStartThrowingServiceUnavailable() throws Exception {
		final String providerId = directComponent.register(GreetingService.class, new GreetingServiceImpl("hello: "));
		serviceRegistryClient.register(GreetingService.class, directComponent.getServiceProperties(providerId), UNUSED_LEASE);
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals("hello: kalle", dummyService.hello("kalle"));
		
		fakeServiceRegistry.clear(); // Simulate lease expiry
		
		assertEventually(serviceInvocationException(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, new TypeSafeMatcher<ServiceUnavailableException>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("Expected ServiceUnavailableException to be thrown");
			}
			@Override
			protected boolean matchesSafely(ServiceUnavailableException item) {
				return true;
			}
		}));
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(1000, 1).check(probe);
	}
	
	@AsterixLibraryProvider
	public static class InMemoryServiceRegistryDescriptor {
		@AsterixExport
		public AsterixServiceRegistry create() {
			return new InMemoryServiceRegistry();
		}
	}
	
	static class InMemoryServiceRegistry implements AsterixServiceRegistry {
		
		private Map<ServiceKey, AsterixServiceRegistryEntry> servicePropertiesByKey = new ConcurrentHashMap<>();
		
		@Override
		public <T> AsterixServiceRegistryEntry lookup(String type) {
			return this.servicePropertiesByKey.get(new ServiceKey(type));
		}
		@Override
		public <T> AsterixServiceRegistryEntry lookup(String type, String qualifier) {
			return this.servicePropertiesByKey.get(new ServiceKey(type, qualifier));
		}
		@Override
		public <T> void register(AsterixServiceRegistryEntry properties, long lease) {
			this.servicePropertiesByKey.put(new ServiceKey(properties.getServiceBeanType(), properties.getQualifier()), properties);
		}
		
		public void clear() {
			this.servicePropertiesByKey.clear();
		}
	}
	
	@AsterixServiceRegistryApi(
		exportedApis = { 
			GreetingService.class
		}
	)
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
