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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PreDestroy;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeanPostProcessor;
import com.avanza.astrix.beans.factory.AstrixBeans;


public class ModuleTest {
	
	@Test
	public void exportedBeansAreAccessibleOutsideTheModule() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithInternalDriver.class);
				context.export(Ping.class);
			}
		});
		assertEquals(PingWithInternalDriver.class, moduleManager.getInstance(Ping.class).getClass());
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void itsNotAllowedToPullNonExportedInstancesFromAModule() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithInternalDriver.class);
				context.export(Ping.class);
			}
		});
		moduleManager.getInstance(PingDriverImpl.class);
	}
	
	@Test
	public void itsPossibleToImportBeansExportedByOtherModules() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithImportedDriver.class);
				context.importType(PingDriver.class);
				context.export(Ping.class);
			}
		});
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(PingDriver.class, PingDriverImpl.class);
				context.export(PingDriver.class);
			}
		});
		assertEquals(PingWithImportedDriver.class, moduleManager.getInstance(Ping.class).getClass());
	}
	
	@Test
	public void destroyingAModuleManagerInvokesDestroyAnnotatedMethodsExactlyOnce() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithImportedDriver.class);
				context.importType(PingDriver.class);
				context.export(Ping.class);
			}
		});
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(PingDriver.class, PingDriverImpl.class);
				context.export(PingDriver.class);
			}
		});

		// NOTE: Create Ping to ensure that PingDriver is note destroyed twice.
		//       Once when module containing ping is destroyed, and once when drive 
		//       module is destroyed
		moduleManager.getInstance(Ping.class); 
		PingDriver pingDriver = moduleManager.getInstance(PingDriver.class);
		
		moduleManager.destroy();
		
		assertEquals(1, pingDriver.destroyCount());
	}
	
	@Test
	public void multipleExportedBeansOfSameType_UsesFirstProvider() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
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
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		
		Ping ping = moduleManager.getInstance(Ping.class);
		assertEquals("not reversed", ping.ping("not reversed"));
	}
	
	@Test
	public void itsPossibleToImportAllExportedBeansOfAGivenType() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.importType(Ping.class);
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
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		PingPluginCollector pingPluginCollector = moduleManager.getInstance(PingPluginCollector.class);
		assertEquals(2, pingPluginCollector.pingPluginCount());
	}
	
	@Test
	public void injectingAllBeansOfImportedTypesWithNoRegisteredProviders() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.importType(Ping.class);
				moduleContext.export(PingPluginCollector.class);
			}
		});
		PingPluginCollector pingPluginCollector = moduleManager.getInstance(PingPluginCollector.class);
		assertEquals(0, pingPluginCollector.pingPluginCount());
	}
	
	@Test
	public void multipleExportedBeansOfImportedType_UsesFirstRegisteredProvider() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(SinglePingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(SinglePingCollector.class);
			}
		});
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
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
		SinglePingCollector pingPluginCollector = moduleManager.getInstance(SinglePingCollector.class);
		assertEquals("oof", pingPluginCollector.getPing().ping("foo"));
	}
	
	@Test
	public void getBeansOfTypeReturnsAllExportedBeansOfAGivenType() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(SinglePingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(SinglePingCollector.class);
			}
		});
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
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
		assertEquals(1, moduleManager.getBeansOfType(SinglePingCollector.class).size());
		assertEquals(2, moduleManager.getBeansOfType(Ping.class).size());
	}
	
	@Test
	public void allExportedBeansOfType() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(SinglePingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(SinglePingCollector.class);
			}
			@Override
			public String name() {
				return "collector";
			}
		});
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
			@Override
			public String name() {
				return "reverse-ping";
			}
		});
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
			@Override
			public String name() {
				return "ping";
			}
		});
		Set<AstrixBeanKey<?>> exportedBeanKeys = moduleManager.getExportedBeanKeys();
		assertEquals(3, exportedBeanKeys.size());
		assertTrue(exportedBeanKeys.contains(AstrixBeanKey.create(SinglePingCollector.class, "collector")));
		assertTrue(exportedBeanKeys.contains(AstrixBeanKey.create(Ping.class, "reverse-ping")));
		assertTrue(exportedBeanKeys.contains(AstrixBeanKey.create(Ping.class, "ping")));
		
		assertEquals(NormalPing.class, moduleManager.getInstance(Ping.class, "ping").getClass());
		assertEquals(ReversePing.class, moduleManager.getInstance(Ping.class, "reverse-ping").getClass());
	}
	
	@Test
	public void beanPostProcessorAreAppliedToAllCreatedBeansThatAreNotCreatedBeforeRegisteringThePostProcessor() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		final BlockingQueue<Object> postProcessedBeans = new LinkedBlockingQueue<Object>();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		moduleManager.registerBeanPostProcessor(new AstrixBeanPostProcessor() {
			@Override
			public void postProcess(Object bean, AstrixBeans astrixBeans) {
				postProcessedBeans.add(bean);
			}
		});
		
		moduleManager.getInstance(Ping.class); // trigger creation of Ping
		assertThat(postProcessedBeans.poll(), instanceOf(ReversePing.class));
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
	
	public static class SinglePingCollector {
		private final Ping ping;

		public SinglePingCollector(Ping ping) {
			this.ping = ping;
		}
		
		public Ping getPing() {
			return ping;
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
	
	public static class PingWithInternalDriver implements Ping {
		
		private PingDriverImpl pingDriver;
		
		public PingWithInternalDriver(PingDriverImpl pingDependency) {
			this.pingDriver = pingDependency;
		}

		@Override
		public String ping(String msg) {
			return pingDriver.ping(msg);
		}

	}
	
	public static class PingWithImportedDriver implements Ping {
		
		private PingDriver pingDriver;
		
		public PingWithImportedDriver(PingDriver pingdriver) {
			this.pingDriver = pingdriver;
		}

		@Override
		public String ping(String msg) {
			return pingDriver.ping(msg);
		}
	}
	
}
