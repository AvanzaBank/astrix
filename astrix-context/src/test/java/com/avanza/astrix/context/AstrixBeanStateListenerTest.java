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
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AstrixBeanStateListenerTest {
	
	private final TestAstrixConfigurer testAstrixConfigurer = new TestAstrixConfigurer();
	
	@Test
	void waitForBean_beanIsLibrary_waitsUntilAllTransitiveDependenciesAreBeBound() throws Exception {
		String myServiceId = DirectComponent.register(MyService.class, new MyServiceImpl());
		testAstrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1);
		testAstrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		testAstrixConfigurer.registerApiProvider(MyServiceProvider.class);
		AstrixContext astrix = testAstrixConfigurer.configure();
		
		final MyClient myClient = astrix.getBean(MyClient.class);
		
		assertThrows(ServiceUnavailableException.class, () -> myClient.hello("kalle"), "Expected service to be unbound");

		// Make myService available in configuration => Allows bean to be bound
		testAstrixConfigurer.set("myService", DirectComponent.getServiceUri(myServiceId));
		
		astrix.waitForBean(MyClient.class, 1000);
		assertEquals("hello: kalle", myClient.hello("kalle"));
		
	}
	
	public interface MyService {
		String hello(String msg);
	}
	
	public static class MyServiceImpl implements MyService {
		@Override
		public String hello(String msg) {
			return "hello: " + msg;
		}
	}
	
	public interface MyClient {
		String hello(String msg);
	}
	
	public static class MyClientImpl implements MyClient {
		private final MyService service;
		public MyClientImpl(MyService service) {
			this.service = service;
		}
		@Override
		public String hello(String msg) {
			return service.hello(msg);
		}
	}
	
	@AstrixApiProvider
	public static class MyLibraryProvider {
		
		@Library
		public MyClient myClient(MyService service) {
			return new MyClientImpl(service);
		}
	}
	
	@AstrixApiProvider
	public interface MyServiceProvider {
		
		@AstrixConfigDiscovery("myService")
		@Service
		MyService myService();
	}
	
	

}
