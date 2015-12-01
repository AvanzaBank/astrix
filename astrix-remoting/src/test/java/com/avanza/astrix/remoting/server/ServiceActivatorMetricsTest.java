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
package com.avanza.astrix.remoting.server;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ServiceActivatorMetricsTest {
	
	@Test
	public void exportsMBeanForExportedService() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(Ping.class, new Ping() {
			@Override
			public String ping(String msg) {
				return msg;
			}
		});
		
		Ping ping = remotingDriver.createRemotingProxy(Ping.class);
		
		ServiceInvocationMonitorMBean pingMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", Ping.class.getName()));
		
		assertEquals(0D, pingMonitor.get50thPercentile(), 0.01D);
		
		ping.ping("msg");
		
		assertEquals(0, pingMonitor.getErrorCount());
		assertEquals(1, pingMonitor.getInvocationCount());
	}
	
	@Test
	public void exportedMBeanTracksInvocationErrorCount() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(Ping.class, new Ping() {
			@Override
			public String ping(String msg) {
				throw new RuntimeException("this is an error");
			}
		});
		
		Ping ping = remotingDriver.createRemotingProxy(Ping.class);
		
		ServiceInvocationMonitorMBean pingMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", Ping.class.getName()));
		
		assertEquals(0D, pingMonitor.get50thPercentile(), 0.01D);
		
		
		try {
			ping.ping("msg");
		} catch (RuntimeException e) {
			// Expected
		}
		
		assertEquals(1, pingMonitor.getErrorCount());
		assertEquals(1, pingMonitor.getInvocationCount());
	}
	
	@Test
	public void exportsMBeanForEachExportedServiceMethod() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(Ping.class, new Ping() {
			@Override
			public String ping(String msg) {
				return msg;
			}
		});
		
		Ping ping = remotingDriver.createRemotingProxy(Ping.class);
		
		ServiceInvocationMonitorMBean pingMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", Ping.class.getName() + "#ping"));
		
		assertEquals(0D, pingMonitor.get50thPercentile(), 0.01D);
		
		ping.ping("msg");
		
		assertEquals(0, pingMonitor.getErrorCount());
		assertEquals(1, pingMonitor.getInvocationCount());
	}
	
	@Test
	public void exportsMBeanForEachExportedAllServicesInActivator() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(Ping.class, new Ping() {
			@Override
			public String ping(String msg) {
				return msg;
			}
		});
		remotingDriver.registerServer(Pong.class, new Pong() {
			@Override
			public String ping(String msg) {
				return msg;
			}
		});
		
		Ping ping = remotingDriver.createRemotingProxy(Ping.class);
		Pong pong = remotingDriver.createRemotingProxy(Pong.class);
		
		ServiceInvocationMonitorMBean pingMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", "AllServicesAggregated"));
		
		assertEquals(0D, pingMonitor.get50thPercentile(), 0.01D);
		
		ping.ping("msg");
		assertEquals(1, pingMonitor.getInvocationCount());
		pong.ping("msg");
		assertEquals(2, pingMonitor.getInvocationCount());
	}
	
	@Test
	public void itIsPossibleToGloballyDisableAllExportedServiceMetrics() throws Exception {
		AstrixRemotingDriver remotingDriver = new AstrixRemotingDriver();
		remotingDriver.registerServer(Ping.class, new Ping() {
			@Override
			public String ping(String msg) {
				return msg;
			}
		});
		
		Ping ping = remotingDriver.createRemotingProxy(Ping.class);
		
		ServiceInvocationMonitorMBean pingMethodMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", Ping.class.getName() + "#ping"));
		ServiceInvocationMonitorMBean pingServiceMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", Ping.class.getName()));
		ServiceInvocationMonitorMBean aggregatedMonitor = remotingDriver.hasExportedMbeanOfType(ServiceInvocationMonitorMBean.class, new MBeanKey("ExportedServices", "AllServicesAggregated"));
		
		remotingDriver.setExportedServiceMetricsEnabled(false);
		
		ping.ping("msg");
		
		assertEquals(0, pingMethodMonitor.getInvocationCount());
		assertEquals(0, pingServiceMonitor.getInvocationCount());
		assertEquals(0, aggregatedMonitor.getInvocationCount());
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public interface Pong {
		String ping(String msg);
	}

}
