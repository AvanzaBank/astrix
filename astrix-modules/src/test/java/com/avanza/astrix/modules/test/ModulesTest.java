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
package com.avanza.astrix.modules.test;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.PreDestroy;

import org.hamcrest.Matchers;
import org.junit.Test;

import com.avanza.astrix.modules.AstrixInject;
import com.avanza.astrix.modules.CircularDependency;
import com.avanza.astrix.modules.MissingBeanBinding;
import com.avanza.astrix.modules.MissingProvider;
import com.avanza.astrix.modules.Module;
import com.avanza.astrix.modules.ModuleContext;
import com.avanza.astrix.modules.ModuleInstancePostProcessor;
import com.avanza.astrix.modules.ModuleNameConflict;
import com.avanza.astrix.modules.Modules;
import com.avanza.astrix.modules.ModulesConfigurer;
import com.avanza.astrix.modules.StrategyProvider;


public class ModulesTest {
	
	
	@Test
	public void exportedBeansAreAccessibleOutsideTheModule() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithInternalDriver.class);
				context.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		assertEquals(PingWithInternalDriver.class, modules.getInstance(Ping.class).getClass());
	}
	
	@Test
	public void itsPossibleToBindToInstancesCreatedOutsideTheModuleSystem() throws Exception {
		final Ping ping = new Ping() {
			@Override
			public String ping(String msg) {
				return "";
			}
		};
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, ping);
				
				context.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		
		Ping exportedPing = modules.getInstance(Ping.class);
		assertSame(ping, exportedPing);
	}
	
	@Test(expected = MissingBeanBinding.class)
	public void throwsMissingBeanBindingForDependencyToNonBoundAbstractTypes() throws Exception {
		// A -> B
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(AType.class, A.class); 
				context.export(AType.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		modules.getInstance(AType.class);
	}
	
	
	@Test
	public void createdBeansAreCached() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithInternalDriver.class);
				context.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		Ping ping1 = modules.getInstance(Ping.class);
		Ping ping2 = modules.getInstance(Ping.class);
		assertSame(ping1, ping2);
	}
	
	@Test(expected = MissingProvider.class)
	public void itsNotAllowedToPullNonExportedInstancesFromAModule() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithInternalDriver.class);
				context.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		modules.getInstance(NormalPingDriver.class);
	}
	
	@Test(expected = ModuleNameConflict.class)
	public void itsNotAllowedToRegisterMultipleModulesWithSameName() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
			}
			@Override
			public String name() {
				return "a";
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
			}
			@Override
			public String name() {
				return "a";
			}
		});
	}
	
	@Test
	public void itsPossibleToImportBeansExportedByOtherModules() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithImportedDriver.class);
				context.importType(PingDriver.class);
				context.export(Ping.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(PingDriver.class, NormalPingDriver.class);
				context.export(PingDriver.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		assertEquals(PingWithImportedDriver.class, modules.getInstance(Ping.class).getClass());
	}
	
	@Test
	public void destroyingAModulesInstanceInvokesDestroyAnnotatedMethodsExactlyOnce() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(Ping.class, PingWithImportedDriver.class);
				
				context.importType(PingDriver.class);
				
				context.export(Ping.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext context) {
				context.bind(PingDriver.class, NormalPingDriver.class);
				
				context.export(PingDriver.class);
			}
		});

		// NOTE: Create Ping to ensure that PingDriver is note destroyed twice.
		//       Once when module containing ping is destroyed, and once when drive 
		//       module is destroyed
		Modules modules = modulesConfigurer.configure();
		modules.getInstance(Ping.class); 
		PingDriver pingDriver = modules.getInstance(PingDriver.class);
		
		modules.destroy();
		
		assertEquals(1, pingDriver.destroyCount());
	}
	
	@Test
	public void multipleExportedBeansOfSameType_UsesFirstProvider() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		
		Modules modules = modulesConfigurer.configure();
		Ping ping = modules.getInstance(Ping.class);
		assertEquals("not reversed", ping.ping("not reversed"));
	}
	
	@Test
	public void itsPossibleToImportAllExportedBeansOfAGivenType() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, PingCollectorImpl.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		PingCollector pingPluginCollector = modules.getInstance(PingCollector.class);
		assertEquals(2, pingPluginCollector.pingInstanceCount());
	}
	
	@Test
	public void injectingAllBeansOfImportedTypesWithNoRegisteredProviders() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, PingCollectorImpl.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		PingCollector pingPluginCollector = modules.getInstance(PingCollector.class);
		assertEquals(0, pingPluginCollector.pingInstanceCount());
	}
	
	@Test
	public void multipleExportedBeansOfImportedType_UsesFirstRegisteredProvider() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		PingCollector pingPluginCollector = modules.getInstance(PingCollector.class);
		assertEquals("oof", pingPluginCollector.getPing().ping("foo"));
	}
	
	@Test
	public void getBeansOfTypeReturnsAllExportedBeansOfAGivenType() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(PingCollector.class, SinglePingCollector.class);
				moduleContext.importType(Ping.class);
				moduleContext.export(PingCollector.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.export(Ping.class);
			}
		});
		Modules modules = modulesConfigurer.configure();
		Collection<Ping> pings = modules.getAll(Ping.class);
		assertEquals(2, pings.size());
		assertThat(pings, hasItem(Matchers.<Ping>instanceOf(NormalPing.class)));
		assertThat(pings, hasItem(Matchers.<Ping>instanceOf(ReversePing.class)));
	}
	
	@Test
	public void beanPostProcessorAreAppliedToAllCreatedBeans() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		final BlockingQueue<Object> postProcessedBeans = new LinkedBlockingQueue<Object>();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, ReversePing.class);
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.registerBeanPostProcessor(new ModuleInstancePostProcessor() {
			@Override
			public void postProcess(Object bean) {
				postProcessedBeans.add(bean);
			}
		});

		Modules modules = modulesConfigurer.configure();
		modules.getInstance(Ping.class); // trigger creation of Ping
		assertThat(postProcessedBeans.poll(), instanceOf(ReversePing.class));
	}
	
	@Test
	public void setterInjectedDependencies() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
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
		modulesConfigurer.register(new Module() {
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
		modulesConfigurer.register(new Module() {
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
		
		Modules modules = modulesConfigurer.configure();

		assertEquals(A.class, modules.getInstance(AType.class).getClass());
		assertThat(modules.getInstance(AType.class).getB(), instanceOf(B.class));
	}
	
	@Test
	public void includesBoundComponentsInSameModuleWhenInjectingListOfAllInstancesOfGivenType() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, NormalPing.class);
				moduleContext.bind(PingCollector.class, PingCollectorImpl.class);
				
				moduleContext.export(PingCollector.class);
			}
		});
		
		Modules modules = modulesConfigurer.configure();
		assertEquals(1, modules.getInstance(PingCollector.class).pingInstanceCount());
	}
	
	@Test
	public void exportingMultipleInterfaceFromSameInstance() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
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
		Modules modules = modulesConfigurer.configure();
		Ping ping = modules.getInstance(Ping.class);
		PingCollector pingCollector = modules.getInstance(PingCollector.class);
		assertNotNull(ping);
		assertSame(ping, pingCollector);
	}
	
	@Test
	public void strategiesSupport() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, PingWithImportedDriver.class);

				moduleContext.importType(PingDriver.class);
				
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(StrategyProvider.create(PingDriver.class, NormalPingDriver.class));
		Modules modules = modulesConfigurer.configure();
		
		Ping ping = modules.getInstance(Ping.class);
		assertEquals("foo", ping.ping("foo"));
	}
	
	@Test
	public void registerDefaultDoesNotOverridePreviouslyRegisteredStrategy() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, PingWithImportedDriver.class);

				moduleContext.importType(PingDriver.class);
				
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(StrategyProvider.create(PingDriver.class, NormalPingDriver.class));
		modulesConfigurer.registerDefault(StrategyProvider.create(PingDriver.class, ReversePingDriver.class));
		Modules modules = modulesConfigurer.configure();
		
		Ping ping = modules.getInstance(Ping.class);
		assertEquals("foo", ping.ping("foo"));
	}
	
	@Test
	public void registerOverridesPreviouslyRegisteredStrategy() throws Exception {
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(Ping.class, PingWithImportedDriver.class);
				moduleContext.importType(PingDriver.class);
				moduleContext.export(Ping.class);
			}
		});
		modulesConfigurer.register(StrategyProvider.create(PingDriver.class, NormalPingDriver.class));
		modulesConfigurer.register(StrategyProvider.create(PingDriver.class, ReversePingDriver.class));
		Modules modules = modulesConfigurer.configure();
		
		Ping ping = modules.getInstance(Ping.class);
		assertEquals("oof", ping.ping("foo"));
	}
	
	@Test
	public void circularDependenciesAreAllowedOnAModularLevel() throws Exception {
		/*
		 * Class dependencies contains no cycles:
		 * A -> B (ModA)
		 * B -> C (ModB)
 		 * C	  (ModA)
		 * 
		 * But module dependencies contains cycle:
		 * 
		 * ModuleB -> ModuleD
		 * ModuleD -> ModuleB
		 */
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(AType.class, A.class);
				moduleContext.bind(CType.class, C.class);
				
				moduleContext.importType(BType.class);
				
				moduleContext.export(AType.class);
				moduleContext.export(CType.class);
			}
			@Override
			public String name() {
				return "ModuleA";
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(BType.class, B.class);
				
				moduleContext.importType(CType.class);
				
				moduleContext.export(BType.class);
			}
			@Override
			public String name() {
				return "ModuleD";
			}
		});
		Modules modules = modulesConfigurer.configure();
		
		modules.getInstance(AType.class);
	}
	
	@Test(expected = CircularDependency.class)
	public void circularDependenciesAreDetectedBetweenModules() throws Exception {
		/*
		 * A -> B (ModA)
		 * B -> A (ModB)
		 * 
		 */
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(AType.class, A.class);
				
				moduleContext.importType(BType.class);
				
				moduleContext.export(AType.class);
			}
			@Override
			public String name() {
				return "ModuleA";
			}
		});
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(BType.class, CircularB.class);
				
				moduleContext.importType(AType.class);
				
				moduleContext.export(BType.class);
			}
			@Override
			public String name() {
				return "ModuleB";
			}
		});
		Modules modules = modulesConfigurer.configure();
		
		modules.getInstance(AType.class);
	}
	
	@Test(expected = CircularDependency.class)
	public void circularDependenciesAreNotAllowedOnInsideAModule() throws Exception {
		/*
		 * Class dependencies contains cycle:
		 * A -> B, B -> A
		 * 
		 */
		ModulesConfigurer modulesConfigurer = new ModulesConfigurer();
		modulesConfigurer.register(new Module() {
			@Override
			public void prepare(ModuleContext moduleContext) {
				moduleContext.bind(AType.class, A.class);
				moduleContext.bind(BType.class, CircularB.class);
				
				moduleContext.export(BType.class);
			}
			@Override
			public String name() {
				return "b";
			}
		});
		Modules modules = modulesConfigurer.configure();
		modules.getInstance(BType.class);
	}
	
	public interface AType {
		BType getB();
	}
	
	public interface BType {
	}
	
	public interface CType {
	}
	
	public interface DType {
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
	

	public static class CircularB implements BType  {
		private AType a;

		@AstrixInject
		public void setA(AType a) {
			this.a = a;
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
	
	public static class NormalPingDriver implements PingDriver {
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
	
	public static class ReversePingDriver implements PingDriver {
		private int destroyCount = 0;
		public String ping(String msg) {
			return new ReversePing().ping(msg);
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
		
		private NormalPingDriver pingDriver;
		
		public PingWithInternalDriver(NormalPingDriver pingDependency) {
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
