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
package com.avanza.asterix.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.avanza.asterix.core.ServiceUnavailableException;
import com.avanza.asterix.provider.core.AsterixConfigApi;
import com.avanza.asterix.provider.library.AsterixExport;
import com.avanza.asterix.provider.library.AsterixLibraryProvider;

public class AsterixBeanStateListenerTest {
	
	@Test
	public void waitForBean_beanIsLibrary_waitsUntilAllTransitiveDependenciesAreBeBound() throws Exception {
		String myServiceId = AsterixDirectComponent.register(MyService.class, new MyServiceImpl());
		TestAsterixConfigurer testAsterixConfigurer = new TestAsterixConfigurer();
		testAsterixConfigurer.set(AsterixSettings.BEAN_REBIND_ATTEMPT_INTERVAL, 1);
		testAsterixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		testAsterixConfigurer.registerApiDescriptor(MyServiceDescriptor.class);
		AsterixContext asterix = testAsterixConfigurer.configure();
		
		final MyClient myClient = asterix.getBean(MyClient.class);
		
		try {
			myClient.hello("kalle");
			fail("Expected service to be unbound");
		} catch (ServiceUnavailableException e) {

		}
		
		// Make myService available in configuration => Allows bean to be bound
		asterix.set("myService", AsterixDirectComponent.getServiceUri(myServiceId));
		
		asterix.waitForBean(MyClient.class, 1000);
		assertEquals("hello: kalle", myClient.hello("kalle"));
		
	}
	
	public static interface MyService {
		String hello(String msg);
	}
	
	public static class MyServiceImpl implements MyService {
		@Override
		public String hello(String msg) {
			return "hello: " + msg;
		}
	}
	
	public static interface MyClient {
		String hello(String msg);
	}
	
	public static class MyClientImpl implements MyClient {
		private MyService service;
		public MyClientImpl(MyService service) {
			this.service = service;
		}
		@Override
		public String hello(String msg) {
			return service.hello(msg);
		}
	}
	
	@AsterixLibraryProvider
	public static class MyLibraryDescriptor {
		
		@AsterixExport
		public MyClient myClient(MyService service) {
			return new MyClientImpl(service);
		}
	}
	
	@AsterixConfigApi(
		entryName = "myService",
		exportedApis = MyService.class
	)
	public static class MyServiceDescriptor {
	}
	
	

}
