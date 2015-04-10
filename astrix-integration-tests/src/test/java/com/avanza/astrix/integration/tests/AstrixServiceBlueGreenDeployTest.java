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
package com.avanza.astrix.integration.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.hamcrest.Description;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryClient;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.beans.registry.ServiceState;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.config.ConfigSource;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixApplication;
import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.serviceunit.ServiceAdministrator;
import com.avanza.astrix.spring.AstrixFrameworkBean;
import com.avanza.astrix.test.util.AstrixTestUtil;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceBlueGreenDeployTest {

	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	private AstrixSettings clientConfig = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(SERVICE_LEASE_RENEW_INTERVAL, 1000);
		set(BEAN_BIND_ATTEMPT_INTERVAL, 1000);
	}};
	
	private AstrixSettings serverInstance1Config = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
	}};
	
	private AstrixSettings serverInstance2Config = new AstrixSettings() {{
		set(SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
	}};
	

	private Ping ping;
	private AstrixContext clientContext;
	private AnnotationConfigApplicationContext appContext1;
	private AnnotationConfigApplicationContext appContext2;

	static {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.WARN);
		Logger.getLogger("com.avanza.astrix").setLevel(Level.DEBUG);
	}
	
	@Before
	public void setup() throws Exception {
		this.clientContext = new AstrixConfigurer().setConfig(DynamicConfig.create(clientConfig)).configure();
	}
	
	@After
	public void after() throws Exception {
		clientContext.destroy();
		closeSafe(appContext1);
		closeSafe(appContext2);
	}

	private void closeSafe(AutoCloseable autoClosable) {
		AstrixTestUtil.closeSafe(autoClosable);
	}

	@Test
	public void blueGreenDeployTest() throws Exception {
		appContext1 = new AnnotationConfigApplicationContext();
		appContext1.register(PingServerConfig.class);
		addEnvironmentProperty(appContext1, "configSourceId", serverInstance1Config.getConfigSourceId());
		
		serverInstance1Config.set(AstrixSettings.INITIAL_SERVICE_STATE, ServiceState.ACTIVE);
		serverInstance1Config.set(AstrixSettings.APPLICATION_NAME, "ping-server");
		serverInstance1Config.set(AstrixSettings.APPLICATION_TAG, "1");
		serverInstance1Config.set(AstrixSettings.APPLICATION_INSTANCE_ID, "ping-server-1");
		serverInstance1Config.set(AstrixSettings.SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		appContext1.refresh();
		

		appContext2 = new AnnotationConfigApplicationContext();
		appContext2.register(PingServerConfig.class);
		addEnvironmentProperty(appContext2, "configSourceId", serverInstance2Config.getConfigSourceId());
		serverInstance2Config.set(AstrixSettings.INITIAL_SERVICE_STATE, ServiceState.INACTIVE);
		serverInstance2Config.set(AstrixSettings.APPLICATION_NAME, "ping-server");
		serverInstance2Config.set(AstrixSettings.APPLICATION_TAG, "2");
		serverInstance2Config.set(AstrixSettings.APPLICATION_INSTANCE_ID, "ping-server-2");
		serverInstance2Config.set(AstrixSettings.SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		appContext2.refresh();

		
		this.ping = clientContext.waitForBean(Ping.class, 1000);

		String reply = ping.ping("foo");
		assertNotNull(reply);

		assertTrue("Expected reply from instance 1: " + reply, reply.startsWith("ping-server-1"));

		// Activate service-2
		ServiceAdministrator serviceInstance2Administrator = 
				clientContext.waitForBean(ServiceAdministrator.class, "ping-server-2", 1000L);
		serviceInstance2Administrator.setServiceState(ServiceState.ACTIVE);
		
		ServiceAdministrator serviceInstance1Administrator = 
				clientContext.waitForBean(ServiceAdministrator.class, "ping-server-1", 1000L);
		serviceInstance1Administrator.setServiceState(ServiceState.INACTIVE);
		
		AstrixServiceRegistryClient serviceRegistryClient = clientContext.getBean(AstrixServiceRegistryClient.class);
		List<AstrixServiceProperties> serviceProperties = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
		assertEquals(2, serviceProperties.size());
//		
		// Verify traffic eventually moves to instance-2
		assertEventually(new Probe() {
			private String lastReply;
			@Override
			public boolean isSatisfied() {
				return lastReply.startsWith("ping-server-2");
			}
			@Override
			public void sample() {
				lastReply = ping.ping("foo");
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected reply from instance 1, but was " + lastReply);
			}
		});
	}
	
	@Test
	public void serviceGoesToInactiveStateWhenServerIsDeactivated() throws Exception {
		appContext1 = new AnnotationConfigApplicationContext();
		appContext1.register(PingServerConfig.class);
		addEnvironmentProperty(appContext1, "configSourceId", serverInstance1Config.getConfigSourceId());
		serverInstance1Config.set(AstrixSettings.INITIAL_SERVICE_STATE, ServiceState.ACTIVE);
		serverInstance1Config.set(AstrixSettings.APPLICATION_NAME, "ping-server");
		serverInstance1Config.set(AstrixSettings.APPLICATION_TAG, "1");
		serverInstance1Config.set(AstrixSettings.APPLICATION_INSTANCE_ID, "ping-server-1");
		serverInstance1Config.set(AstrixSettings.SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT);
		appContext1.refresh();

		this.ping = clientContext.waitForBean(Ping.class, 1000);
		assertNotNull(ping.ping("foo"));
		
		ServiceAdministrator serviceInstance1Administrator = 
				clientContext.waitForBean(ServiceAdministrator.class, "ping-server-1", 1000);
		serviceInstance1Administrator.setServiceState(ServiceState.INACTIVE);
		assertEventually(new Probe() {
			
			private String currentServiceState;

			@Override
			public void sample() {
				AstrixServiceRegistryClient serviceRegistryClient = clientContext.getBean(AstrixServiceRegistryClient.class);
				List<AstrixServiceProperties> servicePropertyList = serviceRegistryClient.list(AstrixBeanKey.create(Ping.class));
				assertEquals("registered service count" + servicePropertyList, 1, servicePropertyList.size());
				AstrixServiceProperties serviceProperties = servicePropertyList.get(0);
				currentServiceState = serviceProperties.getProperties().get(AstrixServiceProperties.SERVICE_STATE);
			}
			
			@Override
			public boolean isSatisfied() {
				return ServiceState.INACTIVE.equals(currentServiceState);
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected servcie to be in state INACTIVE, but it was " + currentServiceState);
			}
		});
	}
	
	private void addEnvironmentProperty(AnnotationConfigApplicationContext context, String name, String value) {
		Map<String, Object> settings = new HashMap<>();
		settings.put(name, value);
		context.getEnvironment().getPropertySources().addFirst(new MapPropertySource("testSetting." + name, settings));
	}
	
	@Configuration
	public static class PingServerConfig {
		@Bean
		public static AstrixFrameworkBean astrix() {
			AstrixFrameworkBean astrix = new AstrixFrameworkBean();
			astrix.setSubsystem("default");
			astrix.setApplicationDescriptor(PingApplicationDescriptor.class);
			return astrix;
		}
		
		@Bean
		public DynamicConfig config(Environment env) {
			String configSourceId = env.getProperty("configSourceId");
			ConfigSource configSource = GlobalConfigSourceRegistry.getConfigSource(configSourceId);
			return DynamicConfig.create(configSource);
		}
		
		@Bean
		public Ping ping(DynamicConfig config) {
			return new PingImpl(AstrixSettings.APPLICATION_INSTANCE_ID.getFrom(config).get());
		}
	}

	@AstrixApplication(
		exportsRemoteServicesFor = PingApi.class,
		defaultServiceComponent = AstrixServiceComponentNames.DIRECT
	)
	public static class PingApplicationDescriptor {
	}

	@AstrixApiProvider
	public static interface PingApi {
		
		@Service
		Ping ping();
		
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	@AstrixServiceExport(Ping.class)
	public static class PingImpl implements Ping {
		
		private String applicationInstanceId;
		
		public PingImpl(String applicationInstanceId) {
			this.applicationInstanceId = applicationInstanceId;
		}

		@Override
		public String ping(String msg) {
			return applicationInstanceId + "-" + msg;
		}
	}
	
	private void assertEventually(Probe probe) throws InterruptedException {
		new Poller(10_000, 10).check(probe);
	}
	
}
