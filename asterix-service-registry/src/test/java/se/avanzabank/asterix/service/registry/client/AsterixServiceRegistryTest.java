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
import static se.avanzabank.asterix.context.AsterixTestUtil.serviceInvocationResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Before;
import org.junit.Test;
import org.openspaces.remoting.Routing;

import se.avanzabank.asterix.context.AsterixContext;
import se.avanzabank.asterix.context.AsterixSettings;
import se.avanzabank.asterix.context.Poller;
import se.avanzabank.asterix.context.Probe;
import se.avanzabank.asterix.context.Supplier;
import se.avanzabank.asterix.context.TestAsterixConfigurer;
import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.provider.component.AsterixServiceRegistryComponentNames;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.provider.library.AsterixExport;
import se.avanzabank.asterix.provider.library.AsterixLibraryProvider;
import se.avanzabank.asterix.service.registry.app.ServiceKey;


public class AsterixServiceRegistryTest {
	
	private AsterixDirectComponent directComponent;
	private AsterixServiceRegistry serviceRegistry;
	private AsterixContext context;

	@Before
	public void setup() {
		TestAsterixConfigurer configurer = new TestAsterixConfigurer();
		configurer.set(AsterixSettings.BEAN_REBIND_ATTEMP_INTERVAL, 10);
		configurer.registerApiDescriptor(GreetingApiDescriptor.class);
		configurer.registerApiDescriptor(FakeServiceRegistryDescriptor.class);
		context = configurer.configure();
		directComponent = context.getPluginInstance(AsterixDirectComponent.class);
		serviceRegistry = context.getBean(AsterixServiceRegistry.class);
	}
	
	@Test
	public void lookupServiceInRegistryAndBind() throws Exception {
		final String objectId = directComponent.register(new GreetingServiceImpl("hello: "));
		
		context.getBean(AsterixServiceRegistry.class).register(GreetingService.class, new AsterixServiceProperties() {{
			setProperty("providerName", objectId);
			setComponent("direct");
		}});
		
		GreetingService greetingService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), greetingService.hello("kalle"));
	}
	
	@Test
	public void serviceIsEventuallyBoundWhenServiceNotAvailableInRegistryOnFirstLookup() throws Exception {
		final String objectId = directComponent.register(new GreetingServiceImpl("hello: "));
		final GreetingService dummyService = context.getBean(GreetingService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service not to be registered in registry");
		} catch (ServiceUnavailableException e) {
		}
		
		serviceRegistry.register(GreetingService.class, new AsterixServiceProperties() {{
			setProperty("providerName", objectId);
			setComponent("direct");
		}});
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hello: kalle")));
	}
	
//	@Test
	public void serviceIsReboundIfServiceIsMovedInRegistry() throws Exception {
		final String id = directComponent.register(new GreetingServiceImpl("hello: "));
		serviceRegistry.register(GreetingService.class, new AsterixServiceProperties() {{
			setProperty("providerName", id);
			setComponent("direct");
		}});
		
		final GreetingService dummyService = context.getBean(GreetingService.class);
		assertEquals(new GreetingServiceImpl("hello: ").hello("kalle"), dummyService.hello("kalle"));
		
		final String newId = directComponent.register(new GreetingServiceImpl("hej : "));
		serviceRegistry.register(GreetingService.class, new AsterixServiceProperties() {{
			setProperty("providerName", newId);
			setComponent("direct");
		}});
		
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hej: kalle")));
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(1000, 1).check(probe);
	}
	
	@AsterixLibraryProvider
	public static class FakeServiceRegistryDescriptor {
		@AsterixExport
		public AsterixServiceRegistry create() {
			return new FakeServiceRegistry();
		}
	}
	
	static class FakeServiceRegistry implements AsterixServiceRegistry {
		
		private Map<ServiceKey, AsterixServiceProperties> servicePropertiesByKey = new ConcurrentHashMap<>();
		
		@Override
		public <T> AsterixServiceProperties lookup(@Routing Class<T> type) {
			return this.servicePropertiesByKey.get(new ServiceKey(type.getName()));
		}
		@Override
		public <T> AsterixServiceProperties lookup(@Routing Class<T> type, String qualifier) {
			return this.servicePropertiesByKey.get(new ServiceKey(type.getName(), qualifier));
		}
		@Override
		public <T> void register(@Routing Class<T> type, AsterixServiceProperties properties) {
			this.servicePropertiesByKey.put(new ServiceKey(type.getName(), properties.getQualifier()), properties);
		}
	}
	
	@AsterixServiceRegistryApi(
		exportedApis = { 
			GreetingService.class
		},
		components = {
			AsterixServiceRegistryComponentNames.DIRECT
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
