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
package com.avanza.astrix.beans.registry;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Service;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


class InMemoryServiceRegistryTest {
	
	@Test
	void itsPossibleToRegisterProviderForQualifiedBeans() {
		InMemoryServiceRegistry registry = new InMemoryServiceRegistry();
		registry.registerProvider(Ping.class, "ping", new PingImpl());
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, registry.getServiceUri());
		AstrixContext context = astrixConfigurer.configure();
		Ping ping = context.getBean(Ping.class, "ping");
		
		assertEquals("foo", ping.ping("foo"));
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		@AstrixQualifier("ping")
		Ping qualifiedPing();
	}
	
	public static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}

}
