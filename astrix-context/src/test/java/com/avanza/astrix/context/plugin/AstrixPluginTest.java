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
package com.avanza.astrix.context.plugin;

import static org.junit.Assert.assertEquals;

import javax.annotation.PreDestroy;

import org.junit.Test;


public class AstrixPluginTest {
	
	@Test
	public void simplePlugin() throws Exception {
		PluginManager pluginManager = new PluginManager();
		pluginManager.register(new IndependentPingPlugin());
		
		assertEquals(IndependentPing.class, pluginManager.getPluginInstance(Ping.class).getClass());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itsNotAllowedToPullNonExportedInstancesFromAPlugin() throws Exception {
		PluginManager pluginManager = new PluginManager();
		pluginManager.register(new IndependentPingPlugin());
		pluginManager.getPluginInstance(PingDriverImpl.class);
	}
	
	@Test
	public void dependentPlugin() throws Exception {
		PluginManager pluginManager = new PluginManager();
		pluginManager.register(new DependentPingPlugin());
		pluginManager.register(new PingDriverPlugin());
		
		assertEquals(DependentPing.class, pluginManager.getPluginInstance(Ping.class).getClass());
	}
	
	@Test
	public void destroyingAPluginDoesNotDestroyImportedPlugins() throws Exception {
		PluginManager pluginManager = new PluginManager();
		pluginManager.register(new DependentPingPlugin());
		pluginManager.register(new PingDriverPlugin());
		
		Ping ping = pluginManager.getPluginInstance(Ping.class);
		PingDriver pingDriver = pluginManager.getPluginInstance(PingDriver.class);
		
		pluginManager.destroy();
		
		assertEquals(1, ping.destroyCount());
		assertEquals(1, pingDriver.destroyCount());
	}
	
	@Test
	public void multipleProvidersForSameTypeAPluginDoesNotDestroyImportedPlugins() throws Exception {
		PluginManager pluginManager = new PluginManager();
		pluginManager.register(new DependentPingPlugin());
		pluginManager.register(new PingDriverPlugin());
		
		Ping ping = pluginManager.getPluginInstance(Ping.class);
		PingDriver pingDriver = pluginManager.getPluginInstance(PingDriver.class);
		
		pluginManager.destroy();
		
		assertEquals(1, ping.destroyCount());
		assertEquals(1, pingDriver.destroyCount());
	}
	
	@com.avanza.astrix.core.AstrixPlugin
	public interface Ping {
		String ping(String msg);
		int destroyCount();
	}
	
	@com.avanza.astrix.core.AstrixPlugin
	public interface PingDriver {
		String ping(String msg);
		int destroyCount();
	}
	
	public static class PingDriverImpl implements PingDriver {
		private int destroyCount = 0;
		public String ping(String msg) {
			return msg;
		}

		@Override
		public int destroyCount() {
			return destroyCount;
		}
		
		@PreDestroy
		public void destroy() {
			destroyCount++;
		}
	}
	
	public static class IndependentPing implements Ping {
		
		private PingDriverImpl pingDriver;
		private int destroyCount = 0;
		
		public IndependentPing(PingDriverImpl pingDependency) {
			this.pingDriver = pingDependency;
		}

		@Override
		public String ping(String msg) {
			return pingDriver.ping(msg);
		}

		@Override
		public int destroyCount() {
			return destroyCount;
		}
		
		@PreDestroy
		public void destroy() {
			destroyCount++;
		}
	}
	
	public static class DependentPing implements Ping {
		
		private PingDriver pingDriver;
		private int destroyCount = 0;
		
		public DependentPing(PingDriver pingdriver) {
			this.pingDriver = pingdriver;
		}

		@Override
		public String ping(String msg) {
			return pingDriver.ping(msg);
		}

		@Override
		public int destroyCount() {
			return destroyCount;
		}
		
		@PreDestroy
		public void destroy() {
			destroyCount++;
		}
	}
	
	public static class IndependentPingPlugin implements AstrixPlugin {

		@Override
		public void prepare(PluginContext context) {
			context.bind(Ping.class, IndependentPing.class);
			context.export(Ping.class);
		}

	}
	
	public static class DependentPingPlugin implements AstrixPlugin {

		@Override
		public void prepare(PluginContext context) {
			context.bind(Ping.class, DependentPing.class);
			context.importPlugin(PingDriver.class);
			context.export(Ping.class);
		}

	}
	
	public static class PingDriverPlugin implements AstrixPlugin {

		@Override
		public void prepare(PluginContext context) {
			context.bind(PingDriver.class, PingDriverImpl.class);
			context.export(PingDriver.class);
		}
	}

}
