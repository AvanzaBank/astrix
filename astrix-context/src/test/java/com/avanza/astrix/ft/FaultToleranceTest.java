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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.BeanFaultTolerance;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import rx.Observable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FaultToleranceTest {
	
	private final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	private final FakeFaultToleranceFactory faultTolerance = new FakeFaultToleranceFactory();
	private AstrixContext astrixContext;
	private final AstrixBeanKey<?> pingKey = AstrixBeanKey.create(Ping.class);
	private Ping ping;
	private TestAstrixConfigurer astrixConfigurer;
	private Ping anotherPing;
	private final AstrixBeanKey<?> anotherPingKey = AstrixBeanKey.create(Ping.class, "another-ping");
	
	@BeforeEach
	void setup() {
		serviceRegistry.registerProvider(Ping.class, "configured-ping", new PingImpl());
		astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApi.class);
		astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		astrixConfigurer.registerStrategy(BeanFaultToleranceFactorySpi.class, faultTolerance);
		astrixConfigurer.enableFaultTolerance(true);
		astrixContext = astrixConfigurer.configure();
		ping = astrixContext.getBean(Ping.class); 
		anotherPing = astrixContext.getBean(Ping.class, anotherPingKey.getQualifier());
	}
	
	@AfterEach
	void destroy() {
		astrixContext.destroy();
	}
	
	
	@Test
	void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() {
		assertEquals(0, getAppliedFaultToleranceCount(pingKey));
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount(pingKey));

		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);

		assertEquals("bar", ping.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount(pingKey));
	}
	
	private int getAppliedFaultToleranceCount(AstrixBeanKey<?> pingKey) {
		return this.faultTolerance.getAppliedFaultToleranceCount(pingKey);
	}

	@Test
	void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenBean() {
		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		assertEquals(0, getAppliedFaultToleranceCount(pingKey));
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount(pingKey));

		astrixConfigurer.set(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED.nameFor(AstrixBeanKey.create(Ping.class)), "false");
		assertEquals("bar", ping.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount(pingKey));
		

		assertEquals(0, getAppliedFaultToleranceCount(anotherPingKey));
		assertEquals("bar", anotherPing.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount(anotherPingKey));
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
		public Ping configuredPing() {
			return null;
		}
	}
	
	private static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	private static class FakeFaultToleranceFactory implements BeanFaultToleranceFactorySpi {
	
		private final Map<AstrixBeanKey<?>, FakeBeanFaultTolerance> ftByBeanKey = new HashMap<>();
		
		@Override
		public BeanFaultTolerance create(AstrixBeanKey<?> beanKey) {
			return this.ftByBeanKey.computeIfAbsent(beanKey, bk -> new FakeBeanFaultTolerance());
		}

		public int getAppliedFaultToleranceCount(AstrixBeanKey<?> pingKey) {
			return java.util.Optional.ofNullable(this.ftByBeanKey.get(pingKey))
									 .map(FakeBeanFaultTolerance::getAppliedFaultToleranceCount)
									 .orElse(0);
		}
	}
	
	private static class FakeBeanFaultTolerance implements BeanFaultTolerance {
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger(0);
		@Override
		public <T> Observable<T> observe(Supplier<Observable<T>> observable) {
			appliedFaultToleranceCount.incrementAndGet();
			return observable.get();
		}

		@Override
		public <T> T execute(CheckedCommand<T> command) throws Throwable {
			appliedFaultToleranceCount.incrementAndGet();
			return command.call();
		}
		
		public int getAppliedFaultToleranceCount() {
			return this.appliedFaultToleranceCount.get();
		}
	}

}
