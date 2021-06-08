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

import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AsyncServiceTest {
	
	private PingAsync ping;
	private AstrixContext context;
	private BlockingPing server = new BlockingPing();

	@BeforeEach
	void setup() {
		SingleServiceComponent singleService = new SingleServiceComponent(PingAsync.class, server);
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.registerPlugin(ServiceComponent.class, singleService);
		astrixConfigurer.set("pingUri", singleService.getName() + ":");

		context = astrixConfigurer.configure();
		ping = context.getBean(PingAsync.class);
	}
	
	@AfterEach
	void after() {
		context.destroy();
	}
	
	@Test
	void asyncServiceInvocationShouldRunAsynchronouslyWithMethodCall() throws Exception {
		Future<String> response = ping.ping("foo");
		server.setResponse("bar");
		assertEquals("bar", response.get());
	}
	
	@Test
	void asyncServiceInvocationShouldStartSynchronouslyWithMethodCall() throws Exception {
		PingAsync ping = context.getBean(PingAsync.class);

		@SuppressWarnings("unused")
		Future<String> response = ping.ping("foo");
		assertEquals("foo", server.pingRequests.poll(1, SECONDS), "Service invocation should be started synchronously with method call. Last server invocation: ");
	}
	
	@Test
	void validatesAllThatAllMethodsInReactiveTypeAreReactive() {
		AstrixContext context = new TestAstrixConfigurer().registerApiProvider(BrokenPingApi.class)
								  .configure();
		assertThrows(MissingBeanProviderException.class, () -> context.getBean(BrokenPingAsync.class));
	}
	
	@Test
	void validatesAllThatAllMethodsInReactiveTypeCorrespondToSyncVersion() {
		AstrixContext context = new TestAstrixConfigurer().registerApiProvider(InconsistentPingApi.class)
								  .configure();
		assertThrows(MissingBeanProviderException.class, () -> context.getBean(InconsistentPingAsync.class));
	}
	
	public static final class BlockingPing implements PingAsync {
		
		private final BlockingQueue<String> pingResponses = new LinkedBlockingQueue<>();
		private final BlockingQueue<String> pingRequests = new LinkedBlockingQueue<>();

		@Override
		public CompletableFuture<String> ping(String msg) {
			pingRequests.add(msg);
			CompletableFuture<String> result = new CompletableFuture<>();
			new Thread(() -> {
				try {
					String response = pingResponses.poll(1, SECONDS);
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
		CompletableFuture<String> inconsistentPingMethod(String msg);
	}
	
	@AstrixApiProvider
	public interface PingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		Ping ping();
	}
	
	@AstrixApiProvider
	public interface BrokenPingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		BrokenPing ping();
	}
	
	@AstrixApiProvider
	public interface InconsistentPingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		InconsistentPing ping();
	}
	
	private static class SingleServiceComponent implements ServiceComponent {
		
		private final Class<?> api;
		private final Object instance;
		
		
		public SingleServiceComponent(Class<?> api, Object instance) {
			this.api = api;
			this.instance = instance;
		}

		@SuppressWarnings("unchecked")
		@Override
		public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties) {
			return new SimpleBoundServiceBeanInstance<>((T) instance);
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
