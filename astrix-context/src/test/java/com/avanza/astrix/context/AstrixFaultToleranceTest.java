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
package com.avanza.astrix.context;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.avanza.astrix.core.util.ProxyUtil;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Service;

public class AstrixFaultToleranceTest {
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() throws Exception {
		FakeFaultTolerancePlugin faultTolerancePlugin = new FakeFaultTolerancePlugin();
		
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.registerPlugin(AstrixFaultTolerancePlugin.class, faultTolerancePlugin);
		configurer.registerApiProvider(MyServiceProvider.class);
		configurer.enableFaultTolerance(true);
		configurer.set("pingUri", AstrixDirectComponent.registerAndGetUri(Ping.class, new PingImpl()));
		
		AstrixContext context = configurer.configure();
		
		Ping ping = context.getBean(Ping.class);
		
		assertEquals(0, faultTolerancePlugin.appliedFaultToleranceCount.get());
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());

		configurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenCircuit() throws Exception {
		FakeFaultTolerancePlugin faultTolerancePlugin = new FakeFaultTolerancePlugin();
		
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.registerPlugin(AstrixFaultTolerancePlugin.class, faultTolerancePlugin);
		configurer.registerApiProvider(MyServiceProvider.class);
		configurer.enableFaultTolerance(true);
		configurer.set("pingUri", AstrixDirectComponent.registerAndGetUri(Ping.class, new PingImpl()));
		
		AstrixContext context = configurer.configure();
		
		Ping ping = context.getBean(Ping.class);
		
		configurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		assertEquals(0, faultTolerancePlugin.appliedFaultToleranceCount.get());
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());

		

		configurer.set("astrix.faultTolerance." + Ping.class.getName() + ".enabled", false);
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, faultTolerancePlugin.appliedFaultToleranceCount.get());
	}
	
	interface Ping {
		String ping(String name);
	}
	
	static class PingImpl implements Ping {
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
	
	static class FakeFaultTolerancePlugin implements AstrixFaultTolerancePlugin {
		
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger();
		
		@Override
		public <T> T addFaultTolerance(FaultToleranceSpecification<T> spec) {
			return ProxyUtil.newProxy(spec.getApi(), new InvocationCounterProxy(appliedFaultToleranceCount, spec.getProvider()));
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
