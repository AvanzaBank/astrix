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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PreDestroy;

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
	
	
	@Test
	public void createdBeansAreCached() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithInternalDriver.class);
				context.export(Ping.class);
			}
		});
		Ping ping1 = moduleManager.getInstance(Ping.class);
		Ping ping2 = moduleManager.getInstance(Ping.class);
		assertSame(ping1, ping2);
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
				moduleContext.bind(PingCollector.class, PingCollectorImpl.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
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
		PingCollector pingPluginCollector = moduleManager.getInstance(PingCollector.class);
		assertEquals(2, pingPluginCollector.pingInstanceCount());
	}
	
	@Test
	public void injectingAllBeansOfImportedTypesWithNoRegisteredProviders() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, PingCollectorImpl.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
			}
		});
		PingCollector pingPluginCollector = moduleManager.getInstance(PingCollector.class);
		assertEquals(0, pingPluginCollector.pingInstanceCount());
	}
	
	@Test
	public void multipleExportedBeansOfImportedType_UsesFirstRegisteredProvider() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
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
		PingCollector pingPluginCollector = moduleManager.getInstance(PingCollector.class);
		assertEquals("oof", pingPluginCollector.getPing().ping("foo"));
	}
	
	@Test
	public void getBeansOfTypeReturnsAllExportedBeansOfAGivenType() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
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
		assertEquals(1, moduleManager.getBeansOfType(PingCollector.class).size());
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
				moduleContext.export(PingCollector.class);
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
		assertTrue(exportedBeanKeys.contains(AstrixBeanKey.create(PingCollector.class, "collector")));
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
	
	@Test
	public void setterInjectedDependencies() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(AType.class, A.class);
				
				moduleContext.importType(BType.class);
				
				moduleContext.export(AType.class);
			}
			@Override
			public String name() {
				return "A";
			}
		});
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(BType.class, B.class);
				moduleContext.importType(CType.class);
				moduleContext.export(BType.class);
			}
			@Override
			public String name() {
				return "B";
			}
		});
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(CType.class, C.class);
				moduleContext.export(CType.class);
			}
			@Override
			public String name() {
				return "C";
			}
		});
		
		assertEquals(A.class, moduleManager.getInstance(AType.class).getClass());
		assertThat(moduleManager.getInstance(AType.class).getB(), instanceOf(B.class));
	}
	
	@Test
	public void includesBoundComponentsInSameModuleWhenInjectingListOfAllInstancesOfGivenType() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.bind(PingCollector.class, PingCollectorImpl.class);
				
				moduleContext.export(PingCollector.class);
			}
		});
		
		assertEquals(1, moduleManager.getInstance(PingCollector.class).pingInstanceCount());
	}
	
	@Test
	public void exportingMultipleInterfaceFromSameInstance() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, SuperPing.class);
				moduleContext.bind(PingCollector.class, SuperPing.class);
				
				moduleContext.export(Ping.class);
				moduleContext.export(PingCollector.class);
			}
			@Override
			public String name() {
				return "ping";
			}
		});
		Ping ping = moduleManager.getInstance(Ping.class);
		PingCollector pingCollector = moduleManager.getInstance(PingCollector.class);
		assertNotNull(ping);
		assertSame(ping, pingCollector);
	}
	
//	@Test
	public void strategiesSupport() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, PingWithImportedDriver.class);

				moduleContext.importType(PingDriver.class);
				
				moduleContext.export(Ping.class);
			}
			@Override
			public String name() {
				return "ping";
			}
		});
		Ping ping = moduleManager.getInstance(Ping.class);
		PingCollector pingCollector = moduleManager.getInstance(PingCollector.class);
		assertNotNull(ping);
		assertSame(ping, pingCollector);
	}
	
	
	
	
	// TODO: detect circular dependencies!
//	@Test
	public void circularDependencies() throws Exception {
		ModuleManager moduleManager = new ModuleManager();
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(BType.class, CircularTypeB.class);
				
				moduleContext.importType(CType.class);
				
				moduleContext.export(BType.class);
			}
			@Override
			public String name() {
				return "b";
			}
		});
		moduleManager.register(new NamedModule() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(CType.class, CircularTypeC.class);
				
				moduleContext.importType(BType.class);
				
				moduleContext.export(CType.class);
			}
			@Override
			public String name() {
				return "c";
			}
		});
		
		moduleManager.getInstance(BType.class);
	}
	
	public static class CircularTypeB implements BType {
		CType c;
		public CircularTypeB(CType c) {
			this.c = c;
		}
		
	}
	
	public static class CircularTypeC implements CType {
		BType b;
		public CircularTypeC(BType b) {
			this.b = b;
		}
	}
	
	
	public interface AType {
		BType getB();
	}
	
	public interface BType {
	}
	
	public interface CType {
	}
	
	public static class A implements AType {
		private BType b;
		
		@AstrixInject
		public void setB(BType b) {
			this.b = b;
		}
		
		@Override
		public BType getB() {
			return this.b;
		}
	}
	
	public static class B implements BType  {
		private CType c;

		@AstrixInject
		public void setC(CType c) {
			this.c = c;
		}
	}
	
	public static class C implements CType  {
	}
	
	public static class SuperPing implements Ping, PingCollector {

		@Override
		public int pingInstanceCount() {
			return 1;
		}

		@Override
		public Ping getPing() {
			return this;
		}

		@Override
		public String ping(String msg) {
			return msg;
		}
		
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
	
	public interface PingCollector {
		int pingInstanceCount();
		Ping getPing();
	}
	
	public static class PingCollectorImpl implements PingCollector {
		private final Collection<Ping> pingInstances;

		public PingCollectorImpl(List<Ping> pingPlugins) {
			this.pingInstances = pingPlugins;
		}
		
		public int pingInstanceCount() {
			return pingInstances.size();
		}
		
		@Override
		public Ping getPing() {
			throw new UnsupportedOperationException();
		}
	}
	
	public static class SinglePingCollector implements PingCollector {
		private final Ping ping;

		public SinglePingCollector(Ping ping) {
			this.ping = ping;
		}
		
		public Ping getPing() {
			return ping;
		}

		@Override
		public int pingInstanceCount() {
			return ping != null ? 1 : 0;
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
