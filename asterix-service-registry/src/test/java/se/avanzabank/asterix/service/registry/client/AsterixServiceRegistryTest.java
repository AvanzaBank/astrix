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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.openspaces.remoting.Routing;

import se.avanzabank.asterix.context.AsterixContext;
import se.avanzabank.asterix.context.AsterixSettings;
import se.avanzabank.asterix.context.Poller;
import se.avanzabank.asterix.context.Probe;
import se.avanzabank.asterix.context.TestAsterixConfigurer;
import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.provider.component.AsterixServiceRegistryComponentNames;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;
import se.avanzabank.asterix.provider.library.AsterixExport;
import se.avanzabank.asterix.provider.library.AsterixLibraryProvider;
import se.avanzabank.asterix.service.registry.app.ServiceKey;


public class AsterixServiceRegistryTest {
	
	@Test
	public void lookupServiceInRegistryAndBind() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixDirectComponent directComponent = new AsterixDirectComponent();
		asterixConfigurer.registerPlugin(AsterixServiceRegistryComponent.class, directComponent);
		asterixConfigurer.registerApiDescriptor(DummyServiceApiDescriptor.class);
		asterixConfigurer.registerApiDescriptor(FakeServiceRegistryDescriptor.class);
		AsterixContext context = asterixConfigurer.configure();
		
		directComponent.register("dummyImpl", new DummyServiceImpl());
		context.getBean(AsterixServiceRegistry.class).register(DummyService.class, new AsterixServiceProperties() {{
			setProperty("providerName", "dummyImpl");
			setComponent("direct");
		}});
		
		DummyService dummyService = context.getBean(DummyService.class);
		assertEquals(new DummyServiceImpl().hello("kalle"), dummyService.hello("kalle"));
	}
	
	@Test
	public void serviceIsEventuallyBoundWhenServiceNotAvailableInRegistryOnFirstLookup() throws Exception {
		AsterixDirectComponent directComponent = new AsterixDirectComponent();
		TestAsterixConfigurer configurer = new TestAsterixConfigurer();
		configurer.set(AsterixSettings.BEAN_REBIND_ATTEMP_INTERVAL, 50);
		configurer.registerPlugin(AsterixServiceRegistryComponent.class, directComponent);
		configurer.registerApiDescriptor(DummyServiceApiDescriptor.class);
		configurer.registerApiDescriptor(FakeServiceRegistryDescriptor.class);
		AsterixContext context = configurer.configure();
		
		context.getBean(AsterixServiceRegistry.class).register(DummyService.class, new AsterixServiceProperties() {{
			setProperty("providerName", "dummyImpl");
			setComponent("direct");
		}});
		final DummyService dummyService = context.getBean(DummyService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service not to be registered in registry");
		} catch (ServiceUnavailableException e) {
		}
		directComponent.register("dummyImpl", new DummyServiceImpl());
		assertEventually(serviceInvocationResult(new Supplier<String>() {
			@Override
			public String get() {
				return dummyService.hello("kalle");
			}
		}, equalTo("hello: kalle")));
		
	}
	
	interface Supplier<T> {
		T get();
	}
	
	private <T> Probe serviceInvocationResult(final Supplier<T> serviceInvocation, final Matcher<T> matcher) {
		return new Probe() {
			
			private T lastResult;
			private Exception lastException;

			@Override
			public void sample() {
				try {
					this.lastResult = serviceInvocation.get();
				} catch (Exception e) {
					this.lastException = e;
				}
			}
			
			@Override
			public boolean isSatisfied() {
				return matcher.matches(lastResult);
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected serviceInovcation to return: ");
				matcher.describeTo(description);
				if (lastException != null) {
					description.appendText("\nBut last serviceInvocation threw exception: " + lastException.toString());
				} else {
					description.appendText("\nBut last serviceInvocation returnded " + lastResult);
				}
			}
		};
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
			DummyService.class
		},
		components = {
			AsterixServiceRegistryComponentNames.DIRECT
		} 
	)
	public static class DummyServiceApiDescriptor {
		
	}
	
	public interface DummyService {
		String hello(String msg);
	}
	
	public static class DummyServiceImpl implements DummyService {
		@Override
		public String hello(String msg) {
			return "hello: " + msg;
		}
	}
	

}
