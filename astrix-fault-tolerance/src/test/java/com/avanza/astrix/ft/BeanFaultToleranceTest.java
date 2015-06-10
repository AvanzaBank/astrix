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

import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanSettings;
import com.avanza.astrix.beans.publish.ApiProvider;
import com.avanza.astrix.beans.publish.SimpleAstrixBeanDefinition;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

public class BeanFaultToleranceTest {
	
	private static final AstrixBeanKey<Ping> ASTRIX_BEAN_KEY = AstrixBeanKey.create(Ping.class);
	
	private AstrixApplicationContext context;
	private TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
	private Ping ping;
	private Ping anotherPing;

	@Before
	public void setup() {
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		astrixConfigurer.set(HystrixCommandNamingStrategy.class.getName(), CountingCachingHystrixCommandNamingStrategy.class.getName());
		astrixConfigurer.enableFaultTolerance(true);
		context = (AstrixApplicationContext) astrixConfigurer.configure();
		ping = context.getBean(Ping.class);
		anotherPing = context.getBean(Ping.class, "another-ping");
	}
	
	@Test
	public void usesHystrixFaultToleranceProxyProviderPluginToApplyFaultToleranceToLibraries() throws Exception {
		assertEquals(0, getAppliedFaultToleranceCount(Ping.class));

		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));
		
		assertEquals("foo", ping.ping("foo"));
		assertEquals(2, getAppliedFaultToleranceCount(Ping.class));
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() throws Throwable {
		assertEquals(0, getAppliedFaultToleranceCount(Ping.class));

		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));

		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);

		assertEquals("bar", ping.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenBean() throws Throwable {
		astrixConfigurer.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		assertEquals(0, getAppliedFaultToleranceCount(Ping.class));
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));

		astrixConfigurer.set(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED.nameFor(ASTRIX_BEAN_KEY), "false");
		assertEquals("bar", ping.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class));
		

		assertEquals(0, getAppliedFaultToleranceCount(Ping.class, "another-ping"));
		assertEquals("bar", anotherPing.ping("bar"));
		assertEquals(1, getAppliedFaultToleranceCount(Ping.class, "another-ping"));
		
	}

	private int getAppliedFaultToleranceCount(Class<?> beanType) {
		return getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, getCommandKey(AstrixBeanKey.create(beanType)));
	}
	
	private int getAppliedFaultToleranceCount(Class<?> beanType, String qualifier) {
		return getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, getCommandKey(AstrixBeanKey.create(beanType, qualifier)));
	}
	
	private <T> String getCommandKey(AstrixBeanKey<T> beanKey) {
		SimpleAstrixBeanDefinition<T> beanDefinition = new SimpleAstrixBeanDefinition<>(ApiProvider.create(PingApiProvider.class.getName()), beanKey);
		return context.getInstance(BeanFaultToleranceFactory.class).getCommandNamingStrategy().getCommandKeyName(beanDefinition);
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
	
}
