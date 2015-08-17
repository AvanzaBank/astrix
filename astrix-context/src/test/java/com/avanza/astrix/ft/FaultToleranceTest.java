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

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;

import rx.Observable;

public class FaultToleranceTest {
	
	private FakeFaultTolerance faultTolerance = new FakeFaultTolerance();
	private AstrixContext astrixContext;
	private Ping ping;
	private TestAstrixConfigurer astrixConfigurer;
	private Ping anotherPing;
	
	@Before
	public void setup() {
		astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApi.class);
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

	public interface Ping {
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
		@Override
		public <T> Observable<T> observe(Supplier<Observable<T>> observable, CommandSettings settings) {
			appliedFaultToleranceCount.incrementAndGet();
			return observable.get();
		}

		@Override
		public <T> T execute(CheckedCommand<T> command, CommandSettings settings) throws Throwable {
			appliedFaultToleranceCount.incrementAndGet();
			return command.call();
		}
		
	}

}
