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
package com.avanza.astrix.ft;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import rx.Observable;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

public class AstrixFaultToleranceTest {
	
	public static final String PING_GROUP_KEY = "AstrixFaultToleranceTest.Ping.groupKey";
	public static final String PING_COMMAND_KEY = "AstrixFaultToleranceTest.Ping.commandKey";
	
	private FakeFaultToleranceImpl fakeFaultToleranceImpl;
	private AstrixFaultTolerance faultTolerance;
	private MapConfigSource config;

	@Before
	public void setup() {
		config = new MapConfigSource();
		fakeFaultToleranceImpl = new FakeFaultToleranceImpl();
		
		faultTolerance = new AstrixFaultTolerance(fakeFaultToleranceImpl);
		faultTolerance.setConfig(new DynamicConfig(config));
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() throws Exception {
		HystrixCommandSettings hystrixCommandSettings = new HystrixCommandSettings("fooKey", "foo");
		
		assertEquals(0, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
		assertEquals("foo", faultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, hystrixCommandSettings));
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());

		config.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);
		
		assertEquals("foo", faultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, hystrixCommandSettings));
		
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenCircuit() throws Exception {
		HystrixCommandSettings hystrixCommandSettings = new HystrixCommandSettings("fooKey", "foo");
																
		config.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		assertEquals(0, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
		assertEquals("foo", faultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, hystrixCommandSettings));
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());

		config.set("astrix.faultTolerance." + hystrixCommandSettings.getCommandKey() + ".enabled", "false");
		assertEquals("foo", faultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, hystrixCommandSettings));
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
	}
	
	@Test
	public void usesHystrixFaultToleranceProxyProviderPluginToApplyFaultToleranceToLibraries() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.enableFaultTolerance(true);
		AstrixContext context = astrixConfigurer.configure();
		Ping ping = context.getBean(Ping.class);
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, PING_COMMAND_KEY));
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, PING_COMMAND_KEY));
		assertEquals("foo", ping.ping("foo"));
		assertEquals(2, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, PING_COMMAND_KEY));
	}
	
	private int getEventCountForCommand(HystrixRollingNumberEvent hystrixRollingNumberEvent, String commandKey) {
		HystrixCommandMetrics metrics = HystrixCommandMetrics.getInstance(HystrixCommandKey.Factory.asKey(commandKey));
		if (metrics == null) {
			return 0;
		}
		int currentConcurrentExecutionCount = (int) metrics.getCumulativeCount(hystrixRollingNumberEvent);
		return currentConcurrentExecutionCount;
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public static class PingImpl implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	@AstrixApiProvider
	public static class PingApiProvider {
		@AstrixFaultToleranceProxy(groupKey = PING_GROUP_KEY, commandKey = PING_COMMAND_KEY)
		@Library
		public Ping ping() {
			return new PingImpl();
		}
		
	}
	
	public static class FakeFaultToleranceImpl implements AstrixFaultTolerance.Impl {
		
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger();
		
		@Override
		public <T> Observable<T> observe(Supplier<Observable<T>> observable, ObservableCommandSettings settings) {
			appliedFaultToleranceCount.incrementAndGet();
			return observable.get();
		}

		@Override
		public <T> T execute(CheckedCommand<T> command,
				HystrixCommandSettings settings) throws Throwable {
			appliedFaultToleranceCount.incrementAndGet();
			return command.call();
		}

	}
	
}
