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
package com.avanza.astrix.beans.inject;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

import java.util.List;

import org.junit.Test;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.core.AstrixPlugin;



public class AstrixInjectorTest {
	
	AstrixPlugins plugins = new AstrixPlugins();
	AstrixStrategies strategies = new AstrixStrategies(DynamicConfig.create(new MapConfigSource()));
	
	@Test
	public void boundBeansRefersToSameBeanInstanceAsItsImplementationClass() throws Exception {
		AstrixInjector injector = new AstrixInjector(plugins, strategies);
		injector.bind(FooPlugin.class, FooImpl.class);
		
		FooPlugin fooBean = injector.getBean(FooPlugin.class);
		FooImpl fooImplBean = injector.getBean(FooImpl.class);
		assertSame(fooBean, fooImplBean);
	}
	
	@Test
	public void pluginInstanceDoesNotReferToTheSameBeanInstancaAsItsImplementationClass() throws Exception {
		plugins.registerPlugin(FooPlugin.class, new FooImpl());
		AstrixInjector injector = new AstrixInjector(plugins, strategies);
		
		FooPlugin fooBean = injector.getBean(FooPlugin.class);
		FooImpl fooImplBean = injector.getBean(FooImpl.class);
		assertNotSame(fooBean, fooImplBean);
	}
	
	@Test
	public void itsPossibleToWireInAllPluginsOfGivenType() throws Exception {
		AstrixPlugins plugins = new AstrixPlugins();
		plugins.registerPlugin(FooPlugin.class, new FooImpl());
		plugins.registerPlugin(FooPlugin.class, new FooImpl2());
		AstrixInjector injector = new AstrixInjector(plugins, strategies);
		
		FooConsumer fooConsumer = injector.getBean(FooConsumer.class);
		assertEquals(2, fooConsumer.foos.size());
	}
	
	@AstrixPlugin
	public interface FooPlugin {
	}
	
	public class FooImpl implements FooPlugin {
	}

	public class FooImpl2 implements FooPlugin {
	}
	

	public static class FooConsumer {
		
		private List<FooPlugin> foos;

		public FooConsumer(List<FooPlugin> foos) {
			this.foos = foos;
		}
	}

}
