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

import org.junit.Before;
import org.junit.Test;

import rx.Observable;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanSettings;
import com.avanza.astrix.beans.publish.ApiProvider;
import com.avanza.astrix.beans.publish.SimpleAstrixBeanDefinition;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.function.Supplier;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

public class BeanFaultToleranceTest {
	
	private static final AstrixBeanKey<Ping> ASTRIX_BEAN_KEY = AstrixBeanKey.create(Ping.class);
	
	private FakeBeanFaultToleranceProvider fakeFaultToleranceImpl = new FakeBeanFaultToleranceProvider();
	private MapConfigSource config = new MapConfigSource();
	private BeanFaultTolerance fakeBeanFaultTolerance;

	@Before
	public void setup() {
		BeanFaultToleranceFactory faultToleranceFactory = new BeanFaultToleranceFactory(DynamicConfig.create(config), fakeFaultToleranceImpl, new CountingCachingHystrixCommandNamingStrategy());
		fakeBeanFaultTolerance = faultToleranceFactory.create(new SimpleAstrixBeanDefinition<>(ApiProvider.create("BeanFaultToleranceTest.PingApi"), ASTRIX_BEAN_KEY));
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceGloballyAtRuntime() throws Throwable {
		assertEquals(0, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
		assertEquals("foo", fakeBeanFaultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, new HystrixCommandSettings()));
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());

		config.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, false);
		
		assertEquals("foo", fakeBeanFaultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, new HystrixCommandSettings()));
		
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
	}
	
	@Test
	public void itShouldBePossibleToDisableFaultToleranceAtRuntimeForAGivenBean() throws Throwable {
		HystrixCommandSettings hystrixCommandSettings = new HystrixCommandSettings();
																
		config.set(AstrixSettings.ENABLE_FAULT_TOLERANCE, true);
		assertEquals(0, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());
		assertEquals("foo", fakeBeanFaultTolerance.execute(new Command<String>() {
			@Override
			public String call() {
				return "foo";
			}
		}, hystrixCommandSettings));
		assertEquals(1, fakeFaultToleranceImpl.appliedFaultToleranceCount.get());

		config.set(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED.nameFor(ASTRIX_BEAN_KEY), "false");
		assertEquals("foo", fakeBeanFaultTolerance.execute(new Command<String>() {
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
		astrixConfigurer.set(HystrixCommandNamingStrategy.class.getName(), CountingCachingHystrixCommandNamingStrategy.class.getName());
		astrixConfigurer.enableFaultTolerance(true);
		AstrixApplicationContext context = (AstrixApplicationContext) astrixConfigurer.configure();
		Ping ping = context.getBean(Ping.class);

		SimpleAstrixBeanDefinition<Ping> beanDefinition = new SimpleAstrixBeanDefinition<>(ApiProvider.create(PingApiProvider.class.getName()), AstrixBeanKey.create(Ping.class));
		String commandKeyName = context.getInstance(BeanFaultToleranceFactory.class).getCommandNamingStrategy().getCommandKeyName(beanDefinition);
		
		assertEquals(0, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, commandKeyName));
		assertEquals("foo", ping.ping("foo"));
		assertEquals(1, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, commandKeyName));
		assertEquals("foo", ping.ping("foo"));
		assertEquals(2, getEventCountForCommand(HystrixRollingNumberEvent.SUCCESS, commandKeyName));
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
		
	}
	
	public static class FakeBeanFaultToleranceProvider implements BeanFaultToleranceProvider {
		
		private final AtomicInteger appliedFaultToleranceCount = new AtomicInteger();
		
		@Override
		public <T> T execute(CheckedCommand<T> command, com.netflix.hystrix.HystrixCommand.Setter settings)
						throws Throwable {
			appliedFaultToleranceCount.incrementAndGet();
			return command.call();
		}
		

		@Override
		public <T> Observable<T> observe(Supplier<Observable<T>> observableFactory, Setter settings) {
			// TODO Auto-generated method stub
			return null;
		}


	}
	
}
