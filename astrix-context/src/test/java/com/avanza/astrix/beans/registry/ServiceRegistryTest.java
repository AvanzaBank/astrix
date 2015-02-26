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
package com.avanza.astrix.beans.registry;

import static org.junit.Assert.fail;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixDirectComponent;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.Poller;

public class ServiceRegistryTest {
	static {
		BasicConfigurator.configure();
	}
	
	@Test
	public void whenServiceNotAvailableOnFirstBindAttemptTheServiceBeanShouldReattemptToBindLater() throws Exception {
		// "Reserve" id for PingImpl
		String serviceId = AstrixDirectComponent.register(Ping.class, new PingImpl());
		
		TestAstrixConfigurer config = new TestAstrixConfigurer();
		config.registerApiProvider(AstrixServiceRegistryLibraryProvider.class);
		config.registerApiProvider(AstrixServiceRegistryServiceProvider.class);
		config.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10);
		config.set("pingUri", AstrixDirectComponent.getServiceUri(serviceId));
		config.registerApiProvider(PingServiceApiProvider.class);
		AstrixContext context = config.configure();
		
		// Unregister to simulate service that is available in config, but provider not available.
		AstrixDirectComponent.unregister(serviceId);
		
		final Ping ping = context.getBean(Ping.class);
		try {
			ping.ping("foo");
			fail("Bean should not be bound");
		} catch (ServiceUnavailableException e) {
			// expected
		}
		
		AstrixDirectComponent.register(Ping.class, new PingImpl(), serviceId);

		new Poller(1000, 10).check(AstrixTestUtil.isSuccessfulServiceInvocation(new Runnable() {
			@Override
			public void run() {
				ping.ping("foo");
			}
		}));
	}
	
	
	@AstrixApiProvider
	public interface PingServiceApiProvider {
		@AstrixConfigLookup("pingUri")
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping(String arg);
	}
	
	public class PingImpl implements Ping {

		@Override
		public String ping(String arg) {
			return arg;
		}
		
	}

}

