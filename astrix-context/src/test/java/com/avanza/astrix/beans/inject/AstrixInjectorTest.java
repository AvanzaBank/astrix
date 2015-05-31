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



public class AstrixInjectorTest {
	
	
	@Test
	public void boundBeansRefersToSameBeanInstanceAsItsImplementationClass() throws Exception {
		AstrixInjector injector = new AstrixInjector(new AstrixPlugins());
		injector.bind(Foo.class, FooImpl.class);
		
		Foo fooBean = injector.getBean(Foo.class);
		FooImpl fooImplBean = injector.getBean(FooImpl.class);
		assertSame(fooBean, fooImplBean);
	}
	
	@Test
	public void pluginInstanceDoesNotReferToTheSameBeanInstancaAsItsImplementationClass() throws Exception {
		AstrixPlugins plugins = new AstrixPlugins();
		plugins.registerPlugin(Foo.class, new FooImpl());
		AstrixInjector injector = new AstrixInjector(plugins);
		
		Foo fooBean = injector.getBean(Foo.class);
		FooImpl fooImplBean = injector.getBean(FooImpl.class);
		assertNotSame(fooBean, fooImplBean);
	}
	
	@Test
	public void itsPossibleToWireInAllPluginsOfGivenType() throws Exception {
		AstrixPlugins plugins = new AstrixPlugins();
		plugins.registerPlugin(Foo.class, new FooImpl());
		plugins.registerPlugin(Foo.class, new FooImpl2());
		AstrixInjector injector = new AstrixInjector(plugins);
		
		FooConsumer fooConsumer = injector.getBean(FooConsumer.class);
		assertEquals(2, fooConsumer.foos.size());
	}
	
	public interface Foo {
	}
	
	public class FooImpl implements Foo {
	}

	public class FooImpl2 implements Foo {
	}
	

	public static class FooConsumer {
		
		private List<Foo> foos;

		public FooConsumer(List<Foo> foos) {
			this.foos = foos;
		}
	}

}
