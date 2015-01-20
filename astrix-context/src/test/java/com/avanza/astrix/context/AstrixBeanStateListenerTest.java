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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import org.junit.Test;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;

public class AstrixBeanStateListenerTest {
	
	TestAstrixConfigurer testAstrixConfigurer = new TestAstrixConfigurer();
	
	@Test
	public void waitForBean_beanIsLibrary_waitsUntilAllTransitiveDependenciesAreBeBound() throws Exception {
		String myServiceId = AstrixDirectComponent.register(MyService.class, new MyServiceImpl());
		testAstrixConfigurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 1);
		testAstrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		testAstrixConfigurer.registerApiProvider(MyServiceProvider.class);
		AstrixContextImpl astrix = (AstrixContextImpl) testAstrixConfigurer.configure();
		
		final MyClient myClient = astrix.getBean(MyClient.class);
		
		try {
			myClient.hello("kalle");
			fail("Expected service to be unbound");
		} catch (ServiceUnavailableException e) {

		}
		
		// Make myService available in configuration => Allows bean to be bound
		testAstrixConfigurer.set("myService", AstrixDirectComponent.getServiceUri(myServiceId));
		
		astrix.waitForBean(MyClient.class, 1000);
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
	
	@com.avanza.astrix.provider.core.AstrixApiProvider
	public static class MyLibraryProvider {
		
		@Library
		public MyClient myClient(MyService service) {
			return new MyClientImpl(service);
		}
	}
	
	@com.avanza.astrix.provider.core.AstrixApiProvider
	public interface MyServiceProvider {
		
		@AstrixConfigLookup("myService")
		@Service
		MyService myServcie();
	}
	
	

}
