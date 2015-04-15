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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import rx.Observable;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;

public class AstrixFaultToleranceTest {
	
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
	
	public static class FakeFaultToleranceImpl implements AstrixFaultTolerance.Impl {
		
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger();
		
		@Override
		public <T> Observable<T> observe(Observable<T> observable, ObservableCommandSettings settings) {
			appliedFaultToleranceCount.incrementAndGet();
			return observable;
		}

		@Override
		public <T> T execute(CheckedCommand<T> command,
				HystrixCommandSettings settings) throws Throwable {
			appliedFaultToleranceCount.incrementAndGet();
			return command.call();
		}

	}
	
	static class InvocationCounterProxy implements InvocationHandler {

		private final AtomicInteger totalInvocationCount;
		private final Object target;

		public InvocationCounterProxy(AtomicInteger totalInvocationCount, Object target) {
			this.totalInvocationCount = totalInvocationCount;
			this.target = target;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			this.totalInvocationCount.incrementAndGet();
			return method.invoke(target, args);
		}
	}

}
