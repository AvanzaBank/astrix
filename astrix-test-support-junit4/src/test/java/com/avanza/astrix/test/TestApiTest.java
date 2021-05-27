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
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static org.junit.Assert.assertEquals;

public class TestApiTest {
	
	@ClassRule
	public static AstrixRule astrixRule = new AstrixRule(PingEngineTestApi.class);
	
	@Rule
	public TestApiResetRule resetApis = new TestApiResetRule(astrixRule);
	
	@ClassRule
	public static FakePingaApp pingApp = new FakePingaApp(astrixRule);
	
	
	private Ping ping;
	
	@Before
	public void setup() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, astrixRule.getServiceRegistryUri());
		astrixConfigurer.registerApiProvider(PingApi.class);
		
		AstrixContext context = astrixConfigurer.configure();
		
		ping = context.getBean(Ping.class);
	}
	
	@Test
	public void useTestApi() throws Exception {
		astrixRule.getTestApi(PingEngineTestApi.class).setPrefix("hello: ");
		
		assertEquals("hello: foo", ping.ping("foo"));
	}
	
	@Test
	public void resetClearsStateAndCreatesNewInstanceOfTestApi() throws Exception {
		PingEngineTestApi testApi = astrixRule.getTestApi(PingEngineTestApi.class);
		testApi.setPrefix("hello: ");
		
		assertEquals("hello: foo", ping.ping("foo"));

		astrixRule.resetTestApis();
		
		// Setting values on "old" TestApi instance after reset should have no effect
		testApi.setPrefix("foo: "); 

		assertEquals("foo", ping.ping("foo"));
		
		// Get new intstance of test api and set prefix
		astrixRule.getTestApi(PingEngineTestApi.class).setPrefix("hello: ");
		assertEquals("hello: foo", ping.ping("foo"));
	}
	
	@AstrixServiceExport(Ping.class)
	private static class PrivatePingImpl implements Ping {
		
		private PingEngine pingEngine;

		public PrivatePingImpl(PingEngine pingEngine) {
			this.pingEngine = pingEngine;
		}
		
		@Override
		public String ping(String msg) {
			return pingEngine.ping(msg);
		}
		
	}
	
	public static class PingEngineTestApi implements TestApi {
		private String prefix = "";

		@Override
		public void exportServices(TestContext testContext) {
			testContext.registerService(PingEngine.class, msg -> prefix + msg);
		}
		
		public void setPrefix(String prefix) {
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
	public static class FakePingaApp implements TestRule {
		
		private AstrixApplicationContext context;
		private AstrixRule astrix;

		public FakePingaApp(AstrixRule astrix) {
			this.astrix = astrix;
		}

		public void start() {
			TestAstrixConfigurer astrixConfig = new TestAstrixConfigurer();
			astrixConfig.setApplicationDescriptor(FakePingaApp.class);
			astrixConfig.registerApiProvider(PingEngineApi.class);
			astrixConfig.set(AstrixSettings.SERVICE_REGISTRY_URI, astrix.getServiceRegistryUri());
			context = (AstrixApplicationContext) astrixConfig.configure();
			
			context.getInstance(ServiceExporter.class).addServiceProvider(new PrivatePingImpl(context.getBean(PingEngine.class)));
			context.getInstance(ServiceExporter.class).startPublishServices();
		}
		
		public void stop() {
			context.destroy();
		}
		
		@Override
		public Statement apply(Statement base, Description description) {
			return new Statement() {
				@Override
				public void evaluate() throws Throwable {
					try {
						start();
						base.evaluate();
					} finally {
						stop();
					}
				}
			};
		}
		
	}

}
