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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;

public class AsyncServiceTest {
	
	private PingAsync ping;
	private AstrixContext context;
	private BlockingPing server = new BlockingPing();

	@Before
	public void setup() {
		SingleServiceComponent singleService = new SingleServiceComponent(PingAsync.class, server);
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, singleService);
		astrixConfigurer.set("pingUri", singleService.getName() + ":");

		context = astrixConfigurer.configure();
		ping = context.getBean(PingAsync.class);
	}
	
	@After
	public void after() {
		context.destroy();
	}
	
	@Test
	public void asyncServiceInvocationShouldRunAsynchronouslyWithMethodCalll() throws Exception {
		Future<String> response = ping.ping("foo");
		server.setResponse("bar");
		assertEquals("bar", response.get());
	}
	
	@Test
	public void asyncServiceInvocationShouldStartSynchronouslyWithMethodCalll() throws Exception {
		PingAsync ping = context.getBean(PingAsync.class);

		@SuppressWarnings("unused")
		Future<String> response = ping.ping("foo");
		assertEquals("Service invocation should be started synchronously with method call. Last server invocation: ", 
					 "foo", server.pingRequests.poll(1, TimeUnit.SECONDS));
	}
	
	@Test(expected = MissingBeanProviderException.class)
	public void validatesAllThatAllMethodsInReactiveTypeAreReactive() throws Exception {
		AstrixContext context = new TestAstrixConfigurer().registerApiProvider(BrokenPingApi.class)
								  .configure();
		context.getBean(BrokenPingAsync.class);
	}
	
	@Test(expected = MissingBeanProviderException.class)
	public void validatesAllThatAllMethodsInReactiveTypeCorrespondToSyncVersion() throws Exception {
		AstrixContext context = new TestAstrixConfigurer().registerApiProvider(InconsistentPingApi.class)
								  .configure();
		context.getBean(InconsistentPingAsync.class);
	}
	
	public static final class BlockingPing implements PingAsync {
		
		private final BlockingQueue<String> pingResponses = new LinkedBlockingQueue<>();
		private final BlockingQueue<String> pingRequests = new LinkedBlockingQueue<>();

		@Override
		public CompletableFuture<String> ping(String msg) {
			pingRequests.add(msg);
			CompletableFuture<String> result = new CompletableFuture<String>();
			new Thread(() -> {
				try {
					String response = pingResponses.poll(1, TimeUnit.SECONDS);
					if (response != null) {
						result.complete(response);
					} else {
						result.completeExceptionally(new IllegalStateException("TIMEOUT"));
					}
				} catch (InterruptedException e) {
					result.completeExceptionally(new IllegalStateException("TIMEOUT"));
				}
			}).start();
			return result;
		}

		public void setResponse(String response) {
			this.pingResponses.add(response);
		}
	}

	public interface Ping {
		String ping(String msg);
	}
	
	public interface PingAsync {
		CompletableFuture<String> ping(String msg);
	}
	
	public interface BrokenPing {
		String invalidPing(String msg);
		String validPing(String msg);
	}
	
	public interface BrokenPingAsync {
		Future<String> invalidPing(String msg); // Future is not a reactive type
		CompletableFuture<String> validPing(String msg);
	}
	
	public interface InconsistentPing {
		String ping(String msg);
	}
	
	public interface InconsistentPingAsync {
		CompletableFuture<String> inconsistendPingMethod(String msg);
	}
	
	@AstrixApiProvider
	public static interface PingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		Ping ping();
	}
	
	@AstrixApiProvider
	public static interface BrokenPingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		BrokenPing ping();
	}
	
	@AstrixApiProvider
	public static interface InconsistentPingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		InconsistentPing ping();
	}
	
	private static class SingleServiceComponent implements ServiceComponent {
		
		private Class<?> api;
		private Object instance;
		
		
		public SingleServiceComponent(Class<?> api, Object instance) {
			this.api = api;
			this.instance = instance;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties) {
			return new SimpleBoundServiceBeanInstance<T>((T) instance);
		}

		@Override
		public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
			return new ServiceProperties();
		}

		@Override
		public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> exportedServiceDefinition) {
			return new ServiceProperties();
		}

		@Override
		public String getName() {
			return "single-service";
		}

		@Override
		public boolean canBindType(Class<?> type) {
			return type.equals(api);
		}

		@Override
		public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition) {
			throw new UnsupportedOperationException();
		}

		@Override
		public boolean requiresProviderInstance() {
			return false;
		}
		
	}
	


}
