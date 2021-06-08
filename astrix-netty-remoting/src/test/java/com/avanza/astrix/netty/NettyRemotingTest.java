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
package com.avanza.astrix.netty;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixApplication;
import com.avanza.astrix.provider.core.AstrixServiceExport;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.serviceunit.ServiceExporter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NettyRemotingTest {

	private final InMemoryServiceRegistry registry = new InMemoryServiceRegistry();
	private AstrixApplicationContext serverContext;
	private AstrixContext clientContext;
	
	@AfterEach
	void cleanup() {
		serverContext.destroy();
		clientContext.destroy();
	}
	
	@Test
	@Timeout(5)
	void nettyRemotingTest() throws Exception {
		
		serverContext = (AstrixApplicationContext) new TestAstrixConfigurer().setApplicationDescriptor(PingApp.class)
				.set(AstrixSettings.SERVICE_REGISTRY_URI, registry.getServiceUri())
				.set(AstrixSettings.SERVICE_ADMINISTRATOR_COMPONENT, AstrixServiceComponentNames.DIRECT).configure();
		serverContext.getInstance(ServiceExporter.class).addServiceProvider(new PingImpl());
		serverContext.startServicePublisher();
		
		clientContext = new TestAstrixConfigurer().registerApiProvider(PingApi.class)
																.set(AstrixSettings.SERVICE_REGISTRY_URI, registry.getServiceUri())
																.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10L)
																.configure();
		Ping ping = clientContext.waitForBean(Ping.class, 1000L);
		assertEquals("foo", ping.ping("foo"));
		
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	@AstrixServiceExport(Ping.class)
	public static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}

	@AstrixApiProvider
	public interface PingApi {
		@Service
		Ping ping();
	}
	
	@AstrixApplication(defaultServiceComponent = NettyRemotingComponent.NAME, exportsRemoteServicesFor = PingApi.class)
//	@AstrixApplication(defaultServiceComponent = AstrixServiceComponentNames.DIRECT, exportsRemoteServicesFor = PingApi.class)
	private static class PingApp {
	}
}
