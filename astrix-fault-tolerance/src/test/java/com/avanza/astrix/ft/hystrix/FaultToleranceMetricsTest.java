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
package com.avanza.astrix.ft.hystrix;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.context.mbeans.MBeanServerFacade;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AssertBlockPoller;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixEventType;

public class FaultToleranceMetricsTest {
	
	private FakeMBeanServer mbeanServer = new FakeMBeanServer();
	
	@Rule
	public AutoCloseableRule autoClose = new AutoCloseableRule();
	
	@Before
	public void before() {
		Hystrix.reset();
	}
	
	@Test
	public void exportsMbean() throws Exception {
		InMemoryServiceRegistry reg = new InMemoryServiceRegistry();
		reg.registerProvider(Ping.class, msg -> msg);
		TestAstrixConfigurer testAstrixConfigurer = new TestAstrixConfigurer();
		testAstrixConfigurer.set(AstrixSettings.EXPORT_ASTRIX_MBEANS, true);
		testAstrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, reg.getServiceUri());
		testAstrixConfigurer.registerApiProvider(PingApi.class);
		testAstrixConfigurer.registerStrategy(MBeanServerFacade.class, mbeanServer);
		testAstrixConfigurer.enableFaultTolerance(true);
		AstrixContext context = autoClose.add(testAstrixConfigurer.configure());
		
		Ping ping = context.getBean(Ping.class);
		initMetrics(ping, context);
		
		BeanFaultToleranceMetricsMBean mbean = mbeanServer.getExportedMBean("BeanFaultToleranceMetrics", AstrixBeanKey.create(Ping.class).toString());
		assertEquals(0, mbean.getErrorCount());
		assertEquals(0, mbean.getSuccessCount());
		assertEquals(0, mbean.getErrorPercentage());
		assertEquals(0, mbean.getRollingMaxConcurrentExecutions());
		
		ping.ping("foo");
		
		eventually(() -> assertEquals(0, mbean.getErrorCount()));
		eventually(() -> assertEquals(1, mbean.getSuccessCount()));
		eventually(() -> assertEquals(0, mbean.getErrorPercentage()));
		eventually(() -> assertEquals(1, mbean.getRollingMaxConcurrentExecutions()));
	}

	@Test
	public void countsErrors() throws Exception {
		InMemoryServiceRegistry reg = new InMemoryServiceRegistry();
		reg.registerProvider(Ping.class, msg -> {
			throw new ServiceUnavailableException("");
		});
		TestAstrixConfigurer testAstrixConfigurer = new TestAstrixConfigurer();
		testAstrixConfigurer.set(AstrixSettings.EXPORT_ASTRIX_MBEANS, true);
		testAstrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, reg.getServiceUri());
		testAstrixConfigurer.registerApiProvider(PingApi.class);
		testAstrixConfigurer.registerStrategy(MBeanServerFacade.class, mbeanServer);
		testAstrixConfigurer.enableFaultTolerance(true);
		AstrixContext context = autoClose.add(testAstrixConfigurer.configure());
		
		Ping ping = context.getBean(Ping.class);
		
		BeanFaultToleranceMetricsMBean mbean = mbeanServer.getExportedMBean("BeanFaultToleranceMetrics", AstrixBeanKey.create(Ping.class).toString());
		assertEquals(0, mbean.getErrorCount());
		assertEquals(0, mbean.getSuccessCount());
		assertEquals(0, mbean.getErrorPercentage());
		
		try {
			ping.ping("foo");
		} catch (ServiceUnavailableException e) {
			// Expected
		}

		eventually(() -> {
			assertEquals(0, mbean.getSuccessCount());
			assertEquals(1, mbean.getErrorCount());
			assertEquals(100, mbean.getErrorPercentage());
		});
	}

	private void initMetrics(Ping ping, AstrixContext context) {
		try {
			ping.ping("foo");
		} catch (Exception e) {
		}
		HystrixFaultToleranceFactory faultTolerance = (HystrixFaultToleranceFactory) AstrixApplicationContext.class.cast(context).getInstance(BeanFaultToleranceFactorySpi.class);
		HystrixCommandKey key = faultTolerance.getCommandKey(AstrixBeanKey.create(Ping.class));
		HystrixCommandMetrics.getInstance(key).getCumulativeCount(HystrixEventType.SUCCESS);
	}

	private void eventually(Runnable assertion) throws InterruptedException {
		new AssertBlockPoller(3000, 25).check(assertion);
	}
	
	private static class FakeMBeanServer implements MBeanServerFacade {
		
		private Map<MBeanKey, Object> exportedMBeans = new HashMap<>();
		
		@Override
		public void registerMBean(Object mbean, String folder, String name) {
			this.exportedMBeans.put(new MBeanKey(folder, name), mbean);
		}

		public BeanFaultToleranceMetricsMBean getExportedMBean(String folder, String name) {
			return java.util.Optional.ofNullable(exportedMBeans.get(new MBeanKey(folder, name)))
									.map(BeanFaultToleranceMetricsMBean.class::cast)
									.orElseThrow(() -> new AssertionError("No mbean exported: folder=" + folder + " name=" + name ));
		}
	}
	
	private static class MBeanKey {
		private final String folder;
		private final String name;
		public MBeanKey(String folder, String name) {
			this.folder = folder;
			this.name = name;
		}
		@Override
		public int hashCode() {
			return Objects.hash(toString());
		}
		@Override
		public boolean equals(Object obj) {
			return this.toString().equals(obj.toString());
		}
		@Override
		public String toString() {
			return "MBeanKey [folder=" + folder + ", name=" + name + "]";
		}
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	@AstrixApiProvider
	private interface PingApi {
		@Service
		Ping ping();
	}

}
