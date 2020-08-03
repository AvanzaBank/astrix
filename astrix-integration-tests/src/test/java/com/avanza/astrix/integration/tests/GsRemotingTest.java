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
package com.avanza.astrix.integration.tests;

import static com.avanza.astrix.test.util.AstrixTestUtil.assertThrows;
import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.EmbeddedSpaceFactoryBean;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.core.env.MapPropertySource;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixBroadcast;
import com.avanza.astrix.core.AstrixRouting;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixApplication;
import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.spring.AstrixFrameworkBean;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.gs.test.JVMGlobalLus;
import com.j_spaces.core.IJSpace;

public class GsRemotingTest {
	
	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	@Rule 
	public AutoCloseableRule autoClosables = new AutoCloseableRule();
	
	@Test
	public void routedServiceInvocationThrowServiceUnavailableWhenProxyIsContextIsClosed() throws Exception {
		AnnotationConfigApplicationContext pingServer = autoClosables.add(new AnnotationConfigApplicationContext());
		pingServer.register(PingAppConfig.class);
		pingServer.getEnvironment().getPropertySources().addFirst(new MapPropertySource("props", new HashMap<String, Object>() {{
			put("serviceRegistryUri", serviceRegistry.getServiceUri());
		}}));
		pingServer.refresh();
		
		AstrixContext context = autoClosables.add(
				new TestAstrixConfigurer().registerApiProvider(PingApi.class)
										  .set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri())
										  .set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 200)
										  .configure());
		Ping ping = context.waitForBean(Ping.class, 10000);
		
		assertEquals("foo", ping.ping("foo"));
		
		context.destroy();

		assertThrows(() -> ping.ping("foo"), ServiceUnavailableException.class);
	}
	
	@Test
	public void broadcastedServiceInvocationThrowServiceUnavailableWhenProxyIsContextIsClosed() throws Exception {
		AnnotationConfigApplicationContext pingServer = autoClosables.add(new AnnotationConfigApplicationContext());
		pingServer.register(PingAppConfig.class);
		pingServer.getEnvironment().getPropertySources().addFirst(new MapPropertySource("props", new HashMap<String, Object>() {{
			put("serviceRegistryUri", serviceRegistry.getServiceUri());
		}}));
		pingServer.refresh();
		
		AstrixContext context = autoClosables.add(
				new TestAstrixConfigurer().registerApiProvider(PingApi.class)
										  .set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri())
										  .set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 200)
										  .configure());
		Ping ping = context.waitForBean(Ping.class, 10000);
		
		assertEquals("foo", ping.broadcastPing("foo").get(0));
		
		context.destroy();

		assertThrows(() -> ping.broadcastPing("foo"), ServiceUnavailableException.class);
	}
	
	public interface Ping {
		String ping(@AstrixRouting String msg);
		@AstrixBroadcast
		List<String> broadcastPing(String msg);
	}
	
	@AstrixApiProvider
	public interface PingApi {
		@Service
		Ping ping();
	}
	
	@AstrixServiceExport(Ping.class)
	public static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}

		@Override
		public List<String> broadcastPing(String msg) {
			return Arrays.asList(msg);
		}
	}


	@AstrixApplication(exportsRemoteServicesFor = PingApi.class, defaultServiceComponent = AstrixServiceComponentNames.GS_REMOTING)
	public static class PingAppConfig {
		
		
		@Bean
		public AstrixFrameworkBean astrix(Environment env) {
			AstrixFrameworkBean astrix = new AstrixFrameworkBean();
			astrix.set(AstrixSettings.SERVICE_REGISTRY_URI, env.getProperty("serviceRegistryUri"));
			astrix.setApplicationDescriptor(PingAppConfig.class);
			return astrix;
		}

		@Bean
		public Ping ping() {
			return new PingImpl();
		}
		@Bean
		public GigaSpace gs(IJSpace space) {
			return new GigaSpaceConfigurer(space).create();
		}
		@Bean
		public EmbeddedSpaceFactoryBean spaceFactoryBean() {
			EmbeddedSpaceFactoryBean spaceFactory = new EmbeddedSpaceFactoryBean(UniqueSpaceNameSeed.getSpaceName());
			spaceFactory.setLookupGroups(JVMGlobalLus.getLookupGroupName());
			return spaceFactory;
		}
	}
	
}
