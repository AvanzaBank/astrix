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
package com.avanza.astrix.ft.plugin;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Service;

public class AstrixFaultToleranceTest {
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() throws Exception {
		MapConfigSource config = new MapConfigSource();
		FakeFaultTolerancePlugin faultTolerancePlugin = new FakeFaultTolerancePlugin();

		AstrixFaultTolerance faultTolerance = new AstrixFaultTolerance(faultTolerancePlugin);
		faultTolerance.setConfig(new DynamicConfig(config));
		
		Ping pingWithFt = faultTolerance.addFaultTolerance(Ping.class, new PingImpl(), new HystrixCommandSettings("fooKey", "foo")); 
		
		assertEquals(0, faultTolerancePlugin.appliedFaultToleranceCount.get());
		assertEquals("foo", pingWithFt.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());

		config.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, "false");
		
		assertEquals("foo", pingWithFt.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenCircuit() throws Exception {
		MapConfigSource config = new MapConfigSource();
		FakeFaultTolerancePlugin faultTolerancePlugin = new FakeFaultTolerancePlugin();

		AstrixFaultTolerance faultTolerance = new AstrixFaultTolerance(faultTolerancePlugin);
		faultTolerance.setConfig(new DynamicConfig(config));
		
		Ping pingWithFt = faultTolerance.addFaultTolerance(Ping.class, new PingImpl(), new HystrixCommandSettings("fooKey", "foo"));
																
		config.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, "true");
		assertEquals(0, faultTolerancePlugin.appliedFaultToleranceCount.get());
		assertEquals("foo", pingWithFt.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());

		

		config.set("astrix.faultTolerance." + Ping.class.getName() + ".enabled", "false");
		assertEquals("foo", pingWithFt.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());
	}
	
	public interface Ping {
		String ping(String name);
	}
	
	public static class PingImpl implements Ping {
		public String ping(String name) {
			return name;
		}
	}
	
	@AstrixApiProvider
	interface MyServiceProvider {
		@AstrixConfigLookup("pingUri")
		@Service
		Ping ping();
	}
	
	public static class FakeFaultTolerancePlugin implements AstrixFaultTolerancePlugin {
		
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger();
		
		@Override
		public <T> T addFaultTolerance(Class<T> api, T provider,
				HystrixCommandSettings settings) {
			return ReflectionUtil.newProxy(api, new InvocationCounterProxy(appliedFaultToleranceCount, provider));
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
