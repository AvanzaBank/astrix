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
package com.avanza.astrix.test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixApplication;
import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.serviceunit.ServiceExporter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstrixExtensionTest {
	
	@RegisterExtension
	static AstrixExtension astrixRule = AstrixExtension.create(PingEngineTestApi.class);
	
	@RegisterExtension
	static FakePingApp pingApp = new FakePingApp(astrixRule);

	private Ping ping;
	
	@BeforeEach
	void setup(AstrixTestContext astrixTestContext) {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, astrixTestContext.getServiceRegistryUri());
		astrixConfigurer.registerApiProvider(PingApi.class);
		
		AstrixContext context = astrixConfigurer.configure();
		
		ping = context.getBean(Ping.class);
	}
	
	@Test
	void useTestApi(PingEngineTestApi testApi) {
		testApi.setPrefix("hello: ");
		
		assertEquals("hello: foo", ping.ping("foo"));
	}
	
	@Test
	void resetClearsStateAndCreatesNewInstanceOfTestApi(AstrixTestContext astrixTestContext) {
		PingEngineTestApi testApi = astrixTestContext.getTestApi(PingEngineTestApi.class);
		testApi.setPrefix("hello: ");
		
		assertEquals("hello: foo", ping.ping("foo"));

		astrixTestContext.resetTestApis();
		
		// Setting values on "old" TestApi instance after reset should have no effect
		testApi.setPrefix("foo: "); 

		assertEquals("foo", ping.ping("foo"));
		
		// Get new intstance of test api and set prefix
		astrixTestContext.getTestApi(PingEngineTestApi.class).setPrefix("hello: ");
		assertEquals("hello: foo", ping.ping("foo"));
	}
	
	@AstrixServiceExport(Ping.class)
	private static class PrivatePingImpl implements Ping {
		
		private final PingEngine pingEngine;

		PrivatePingImpl(PingEngine pingEngine) {
			this.pingEngine = pingEngine;
		}
		
		@Override
		public String ping(String msg) {
			return pingEngine.ping(msg);
		}
		
	}
	
	static class PingEngineTestApi implements TestApi {
		private String prefix = "";

		@Override
		public void exportServices(TestContext testContext) {
			testContext.registerService(PingEngine.class, msg -> prefix + msg);
		}
		
		void setPrefix(String prefix) {
			this.prefix  = prefix;
		}
	}
	
	public interface Ping {
		String ping(String ping);
	}
	
	public interface PingEngine {
		String ping(String ping);
	}
	
	@AstrixApiProvider
	public interface PingEngineApi {
		
		@Service
		PingEngine pingEngine();
	}
	
	@AstrixApiProvider
	public interface PingApi {
		
		@Service
		Ping ping();
	}

	@AstrixApplication(
		exportsRemoteServicesFor = PingApi.class,
		defaultServiceComponent = AstrixServiceComponentNames.DIRECT
	)
	static class FakePingApp implements BeforeAllCallback, AfterAllCallback {

		private final AstrixExtension astrix;
		private AstrixApplicationContext context;

		FakePingApp(AstrixExtension astrix) {
			this.astrix = astrix;
		}

		@Override
		public void beforeAll(ExtensionContext extensionContext) {
			TestAstrixConfigurer astrixConfig = new TestAstrixConfigurer();
			astrixConfig.setApplicationDescriptor(FakePingApp.class);
			astrixConfig.registerApiProvider(PingEngineApi.class);
			astrixConfig.set(AstrixSettings.SERVICE_REGISTRY_URI, astrix.getAstrixTestContext(extensionContext).getServiceRegistryUri());
			context = (AstrixApplicationContext) astrixConfig.configure();

			context.getInstance(ServiceExporter.class).addServiceProvider(new PrivatePingImpl(context.getBean(PingEngine.class)));
			context.getInstance(ServiceExporter.class).startPublishServices();
		}

		@Override
		public void afterAll(ExtensionContext extensionContext) {
			context.destroy();
		}

	}

}
