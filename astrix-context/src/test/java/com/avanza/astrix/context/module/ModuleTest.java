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
package com.avanza.astrix.context.module;

import static org.junit.Assert.assertEquals;

import java.util.Collection;
import java.util.List;

import javax.annotation.PreDestroy;

import org.junit.Test;

import com.avanza.astrix.context.module.Module;
import com.avanza.astrix.context.module.ModuleContext;
import com.avanza.astrix.context.module.ModuleManager;


public class ModuleTest {
	
	@Test
	public void singleModule() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new IndependentPingModule());
		
		assertEquals(IndependentPing.class, moduleManager.getInstance(Ping.class).getClass());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itsNotAllowedToPullNonExportedInstancesFromAModule() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new IndependentPingModule());
		moduleManager.getInstance(PingDriverImpl.class);
	}
	
	@Test
	public void moduleWithDependencies() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new DependentPingModule());
		moduleManager.register(new PingDriverModule());
		
		assertEquals(DependentPing.class, moduleManager.getInstance(Ping.class).getClass());
	}
	
	@Test
	public void destroyingAModuleManagerInvokesDestroyAnnotatedMethodsExactlyOnce() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new DependentPingModule());
		moduleManager.register(new PingDriverModule());

		// NOTE: Create Ping to ensure that PingDriver is note destroyed twice.
		//       Once when module containing ping is destroyed, and once when drive 
		//       module is destroyed
		moduleManager.getInstance(Ping.class); 
		PingDriver pingDriver = moduleManager.getInstance(PingDriver.class);
		
		moduleManager.destroy();
		
		assertEquals(1, pingDriver.destroyCount());
	}
	
	@Test
	public void multipleProvidersForSameApi_UsesFirstProvider() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new PingModule());
		moduleManager.register(new ReversePingModule());
		
		Ping ping = moduleManager.getInstance(Ping.class);
		
		assertEquals("not reversed", ping.ping("not reversed"));
	}
	
	@Test
	public void itsPossibleToImportAllExportedBeansOfAGivenType() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.importAllOfType(Ping.class);
				moduleContext.export(PingPluginCollector.class);
			}
		});
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
		});
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
		});
		PingPluginCollector pingPluginCollector = moduleManager.getInstance(PingPluginCollector.class);
		assertEquals(2, pingPluginCollector.pingPluginCount());
	}
	
	
	public interface Ping {
		String ping(String msg);
	}
	
	public interface PingPlugin {
		String ping(String msg);
	}
	
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
	
	public static class PingPluginCollector {
		private final Collection<Ping> pingPlugins;

		public PingPluginCollector(List<Ping> pingPlugins) {
			this.pingPlugins = pingPlugins;
		}
		
		public int pingPluginCount() {
			return pingPlugins.size();
		}
		
		
	}
	
	public static class NormalPing implements Ping, PingPlugin {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	public static class ReversePing implements Ping {
		@Override
		public String ping(String msg) {
			return new StringBuilder(msg).reverse().toString();
		}
	}
	
	public static class PingModule implements Module {
		@Override
		public void prepare(ModuleContext moduleContext) {
			moduleContext.bind(Ping.class, NormalPing.class);
			moduleContext.export(Ping.class);
		}
	}
	

	public static class ReversePingModule implements Module {
		@Override
		public void prepare(ModuleContext moduleContext) {
			moduleContext.bind(Ping.class, ReversePing.class);
			moduleContext.export(Ping.class);
		}
	}
	
	public static class IndependentPing implements Ping {
		
		private PingDriverImpl pingDriver;
		
		public IndependentPing(PingDriverImpl pingDependency) {
			this.pingDriver = pingDependency;
		}

		@Override
		public String ping(String msg) {
			return pingDriver.ping(msg);
		}

	}
	
	public static class DependentPing implements Ping {
		
		private PingDriver pingDriver;
		
		public DependentPing(PingDriver pingdriver) {
			this.pingDriver = pingdriver;
		}

		@Override
		public String ping(String msg) {
			return pingDriver.ping(msg);
		}
	}
	
	public static class IndependentPingModule implements Module {

		@Override
		public void prepare(ModuleContext context) {
			context.bind(Ping.class, IndependentPing.class);
			context.export(Ping.class);
		}

	}
	
	public static class DependentPingModule implements Module {

		@Override
		public void prepare(ModuleContext context) {
			context.bind(Ping.class, DependentPing.class);
			context.importType(PingDriver.class);
			context.export(Ping.class);
		}

	}
	
	public static class PingDriverModule implements Module {

		@Override
		public void prepare(ModuleContext context) {
			context.bind(PingDriver.class, PingDriverImpl.class);
			context.export(PingDriver.class);
		}
	}

}
