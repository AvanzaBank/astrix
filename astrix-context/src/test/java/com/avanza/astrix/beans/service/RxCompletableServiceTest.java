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

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.assertEquals;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.Service;

import rx.Completable;
import rx.schedulers.Schedulers;

public class RxCompletableServiceTest {

	private final BlockingPing server = new BlockingPing();
	private PingAsync ping;
	private AstrixContext context;

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
	public void asyncServiceInvocationShouldRunAsynchronouslyWithMethodCall() throws Exception {
		Completable response = ping.ping("foo");
		server.setResponse();

		response.await(1, SECONDS);
	}

	@Test
	public void asyncServiceInvocationShouldStartSynchronouslyWithMethodCall() throws Exception {
		PingAsync ping = context.getBean(PingAsync.class);

		ping.ping("foo").subscribe();
		assertEquals("Service invocation should be started synchronously with method call. Last server invocation: ",
					 "foo", server.pingRequests.poll(1, SECONDS));
	}

	public static final class BlockingPing implements PingAsync {

		private final Semaphore pingResponses = new Semaphore(0);
		private final BlockingQueue<String> pingRequests = new LinkedBlockingQueue<>();

		@Override
		public Completable ping(String msg) {
			pingRequests.add(msg);
			return Completable.create(subscriber -> {
				try {
					boolean responseAvailable = pingResponses.tryAcquire(1, SECONDS);
					if (responseAvailable) {
						subscriber.onCompleted();
					} else {
						subscriber.onError(new IllegalStateException("TIMEOUT"));
					}
				} catch (InterruptedException e) {
					subscriber.onError(new IllegalStateException("TIMEOUT"));
				}
			}).subscribeOn(Schedulers.computation());
		}

		void setResponse() {
			this.pingResponses.release();
		}
	}

	public interface Ping {
		void ping(String msg);
	}

	public interface PingAsync {
		Completable ping(String msg);
	}

	@AstrixApiProvider
	public interface PingApi {
		@AstrixConfigDiscovery("pingUri")
		@Service
		Ping ping();
	}
	
	private static class SingleServiceComponent implements ServiceComponent {
		
		private final Class<?> api;
		private final Object instance;

		SingleServiceComponent(Class<?> api, Object instance) {
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
