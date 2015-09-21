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
package com.avanza.astrix.beans.service;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.CompletableFuture;

import org.junit.After;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;

public class DirectComponentTest {
	
	private AstrixContext astrixContext;

	@Test
	public void itsPossibleToStubReactiveServiceUsingDirectComponent() throws Exception {
		InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
		serviceRegistry.registerProvider(Ping.class, new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixContext = astrixConfigurer.configure();
		
		PingAsync ping = astrixContext.getBean(PingAsync.class);
		assertEquals("foo", ping.ping("foo").get());
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public interface PingAsync {
		CompletableFuture<String> ping(String msg);
	}
	
	public class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}
	
	@After
	public void after() {
		AstrixTestUtil.closeSafe(astrixContext);
	}
	
	@AstrixApiProvider
	public static interface PingApiProvider {
		@Service
		Ping ping();
	}

}
