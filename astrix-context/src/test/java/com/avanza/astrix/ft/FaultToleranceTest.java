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
package com.avanza.astrix.ft;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.CommandSettings;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;

import rx.Observable;

public class FaultToleranceTest {
	
	private InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	private FakeFaultTolerance faultTolerance = new FakeFaultTolerance();
	private AstrixContext astrixContext;
	private Ping ping;
	private TestAstrixConfigurer astrixConfigurer;
	private Ping anotherPing;
	
	@Before
	public void setup() {
		serviceRegistry.registerProvider(Ping.class, "configured-ping", new PingImpl());
		astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerStrategy(FaultToleranceSpi.class, faultTolerance);
		astrixConfigurer.enableFaultTolerance(true);
		astrixContext = astrixConfigurer.configure();
		ping = astrixContext.getBean(Ping.class); 
		anotherPing = astrixContext.getBean(Ping.class, "another-ping");
	}
	
	@After
	public void destroy() {
		astrixContext.destroy();
	}
	
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() throws Throwable {
		assertEquals(0, getAppliedFaultToleranceCount());
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount());

		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);

		assertEquals("bar", ping.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount());
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenBean() throws Throwable {
		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		assertEquals(0, getAppliedFaultToleranceCount());
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount());

		astrixConfigurer.set(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED.nameFor(AstrixBeanKey.create(Ping.class)), "false");
		assertEquals("bar", ping.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount());
		

		assertEquals("bar", anotherPing.ping("bar"));
		assertEquals(2, getAppliedFaultToleranceCount());
	}
	
	@Test
	public void readsDefaultBeanSettingsFromBeanConfiguration() throws Throwable {
		astrixConfigurer.set(AstrixBeanSettings.INITIAL_CORE_SIZE, AstrixBeanKey.create(Ping.class, "configured-ping"), 4);
		astrixConfigurer.set(AstrixBeanSettings.INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD, AstrixBeanKey.create(Ping.class, "configured-ping"), 6);
		astrixConfigurer.set(AstrixBeanSettings.INITIAL_TIMEOUT, AstrixBeanKey.create(Ping.class, "configured-ping"), 100);
		astrixConfigurer.set(AstrixBeanSettings.INITIAL_MAX_CONCURRENT_REQUESTS, AstrixBeanKey.create(Ping.class, "configured-ping"), 21);
		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		
		astrixContext.getBean(Ping.class, "configured-ping").ping("foo");
		CommandSettings appliedSettings = faultTolerance.lastAppliedCommandSettings;
		assertNotNull(appliedSettings);
		assertEquals(4, appliedSettings.getInitialCoreSize());
		assertEquals(6, appliedSettings.getInitialQueueSizeRejectionThreshold());
		assertEquals(100, appliedSettings.getInitialTimeoutInMilliseconds());
		assertEquals(21, appliedSettings.getInitialSemaphoreMaxConcurrentRequests());
	}

	public interface Ping {
		String ping(String msg);
	}
	
	public interface ConfiguredPing {
		String ping(String msg);
	}

	@AstrixApiProvider
	public static class PingApi {

		@AstrixFaultToleranceProxy
		@Library
		public Ping ping() {
			return new PingImpl();
		}
		
		@AstrixFaultToleranceProxy
		@Library
		@AstrixQualifier("another-ping")
		public Ping anotherPing() {
			return new PingImpl();
		}
		
		@Service
		@AstrixQualifier("configured-ping")
		public Ping configurerdPing() {
			return null;
		}
	}
	
	private static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	private int getAppliedFaultToleranceCount() {
		return faultTolerance.appliedFaultToleranceCount.get();
	}
	
	private static class FakeFaultTolerance implements FaultToleranceSpi {
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger(0);
		private CommandSettings lastAppliedCommandSettings;
		@Override
		public <T> Observable<T> observe(Supplier<Observable<T>> observable, CommandSettings settings) {
			lastAppliedCommandSettings = settings;
			appliedFaultToleranceCount.incrementAndGet();
			return observable.get();
		}

		@Override
		public <T> T execute(CheckedCommand<T> command, CommandSettings settings) throws Throwable {
			lastAppliedCommandSettings = settings;
			appliedFaultToleranceCount.incrementAndGet();
			return command.call();
		}
		
	}

}
