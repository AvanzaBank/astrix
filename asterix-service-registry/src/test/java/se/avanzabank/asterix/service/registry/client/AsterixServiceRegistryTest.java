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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.Test;
import org.openspaces.remoting.Routing;

import se.avanzabank.asterix.context.AsterixContext;
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
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixDirectComponent directComponent = new AsterixDirectComponent();
		asterixConfigurer.registerPlugin(AsterixServiceRegistryComponent.class, directComponent);
		asterixConfigurer.registerApiDescriptor(DummyServiceApiDescriptor.class);
		asterixConfigurer.registerApiDescriptor(FakeServiceRegistryDescriptor.class);
		AsterixContext context = asterixConfigurer.configure();
		
		context.getBean(AsterixServiceRegistry.class).register(DummyService.class, new AsterixServiceProperties() {{
			setProperty("providerName", "dummyImpl");
			setComponent("direct");
		}});
		DummyService dummyService = context.getBean(DummyService.class);
		
		try {
			dummyService.hello("kalle");
			fail("Excpected service not to be registered in registry");
		} catch (ServiceUnavailableException e) {
		}
		directComponent.register("dummyImpl", new DummyServiceImpl());
		Thread.sleep(1000); // TODO: decrease intervall for bind attempt
		assertEquals(new DummyServiceImpl().hello("kalle"), dummyService.hello("kalle"));
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
