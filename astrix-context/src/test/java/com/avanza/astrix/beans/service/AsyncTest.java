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

import static org.junit.Assert.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;

public class AsyncTest {
	
private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	
	@Test
	public void asyncServiceInvocationIsAsynchronous() throws Exception {
		final CountDownLatch latch = new CountDownLatch(1);
		serviceRegistry.registerProvider(Ping.class, new SlowPing(latch));
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		AstrixContext context = astrixConfigurer.configure();
		
		PingAsync ping = context.getBean(PingAsync.class);
		Future<String> response = ping.ping("foo");
		latch.countDown();
		assertEquals("foo", response.get());
		
	}
	
	public static final class SlowPing implements Ping {
		private final CountDownLatch latch;

		private SlowPing(CountDownLatch latch) {
			this.latch = latch;
		}

		@Override
		public String ping(String msg) {
			try {
				if (latch.await(1, TimeUnit.SECONDS)) {
					return msg;
				}
			} catch (InterruptedException e) {
			}
			throw new IllegalStateException("TIMEOUT");
		}
	}

	public interface Ping {
		String ping(String msg);
	}
	
	public interface PingAsync {
		Future<String> ping(String msg);
	}
	
	@AstrixApiProvider
	public static interface PingApi {
		@Service
		Ping ping();
	}


}
