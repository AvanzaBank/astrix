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
package com.avanza.astrix.beans.service;

import org.junit.Test;

import com.avanza.astrix.beans.registry.AstrixServiceRegistryLibraryProvider;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryServiceProvider;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;

public class ServiceBeanInstanceTest {
	
	
	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	
	@Test
	public void testName() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		astrixConfigurer.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		AstrixContext astrixContext = astrixConfigurer.configure();
		
		Ping ping = astrixContext.getBean(Ping.class);
		
	}
	
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public static class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}

}
