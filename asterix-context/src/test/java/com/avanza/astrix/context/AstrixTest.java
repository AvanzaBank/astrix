/*
 * Copyright 2014-2015 Avanza Bank AB
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
package com.avanza.astrix.context;

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import javax.annotation.PreDestroy;

import org.junit.Test;

import com.avanza.astrix.context.AstrixCircularDependency;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.avanza.astrix.context.MissingBeanDependencyException;
import com.avanza.astrix.context.MissingBeanProviderException;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.library.AstrixExport;
import com.avanza.astrix.provider.library.AstrixLibraryProvider;


public class AstrixTest {
	
	@Test
	public void AstrixSupportSimpleLibraries() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		
		HelloBean libraryBean = AstrixContext.getBean(HelloBean.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test
	public void supportsLibrariesExportedUsingClasses() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(MyLibraryDescriptorNoInterface.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		HelloBeanImpl libraryBean = AstrixContext.getBean(HelloBeanImpl.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test(expected = AstrixCircularDependency.class)
	public void detectsCircularDependenciesAmoungLibraries() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(CircularApiA.class);
		AstrixConfigurer.registerApiDescriptor(CircularApiB.class);
		AstrixConfigurer.registerApiDescriptor(CircularApiC.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		AstrixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test(expected = MissingBeanDependencyException.class)
	public void detectsMissingBeanDependencies() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(DependentApi.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		AstrixContext.getBean(DependentBean.class);
	}
	
	@Test(expected = MissingBeanProviderException.class)
	public void detectsMissingBeans() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		AstrixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test
	public void doesNotForceDependenciesForUnusedApis() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(DependentApi.class);
		AstrixConfigurer.registerApiDescriptor(IndependentApi.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		IndependentApi bean = AstrixContext.getBean(IndependentApi.class);
		assertNotNull(bean);
	}
	
	@Test
	public void cachesCreatedApis() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		HelloBean beanA = AstrixContext.getBean(HelloBean.class);
		HelloBean beanB = AstrixContext.getBean(HelloBean.class);
		assertSame(beanA, beanB);
	}
	
	@Test
	public void canInitRegularClasses() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		SimpleClass simple = AstrixContext.getInstance(SimpleClass.class);
		
		assertNotNull(simple.settings);
	}
	
	@Test
	public void cachesCreatedInstancesOfRegularClasses() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		SimpleClass simple1 = AstrixContext.getInstance(SimpleClass.class);
		SimpleClass simple2 = AstrixContext.getInstance(SimpleClass.class);
		
		assertSame(simple1, simple2);
	}
	
	@Test
	public void AstrixContextShouldInstantiateAndInjectRequiredClasses() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		MultipleDependentClass dependentClass = AstrixContext.getInstance(MultipleDependentClass.class);
		
		assertNotNull(dependentClass.dep);
		assertNotNull(dependentClass.dep2);
		assertNotNull(dependentClass.dep3);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void injectAnnotatedMethodMustAcceptAtLeastOneDependency() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		AstrixContext.getInstance(IllegalDependendclass.class);
	}
	
	@Test
	public void preDestroyAnnotatedMethodsOnInstancesCreatedByAstrixAreInvokedWhenAstrixContextIsDestroyed() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		SimpleClass simpleClass = AstrixContext.getInstance(SimpleClass.class);
		assertFalse(simpleClass.destroyed);
		
		AstrixContext.destroy();
		assertTrue(simpleClass.destroyed);
	}
	
	@Test
	public void preDestroyAnnotatedMethodsOnLibraryFactoryInstancesAreInvokedWhenAstrixContextIsDestroyed() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext context = AstrixConfigurer.configure();
		
		HelloBeanImpl helloBean = (HelloBeanImpl) context.getBean(HelloBean.class);
		assertFalse(helloBean.destroyed);
		context.destroy();
		assertTrue(helloBean.destroyed);
	}
	
	@Test
	public void manyAstrixContextShouldBeAbleToRunInSameProcessWithoutInterferenceInShutdown() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext context = AstrixConfigurer.configure();
		
		TestAstrixConfigurer AstrixConfigurer2 = new TestAstrixConfigurer();
		AstrixConfigurer2.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext context2 = AstrixConfigurer2.configure();
		context.destroy();
		
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBean.class);
		assertFalse(helloBean2.destroyed);
		context2.destroy();
		assertTrue(helloBean2.destroyed);
	}
	
	@Test
	public void librariesCreatedUsingDifferentContextsShouldReturnDifferentInstances() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext context = AstrixConfigurer.configure();
		
		TestAstrixConfigurer AstrixConfigurer2 = new TestAstrixConfigurer();
		AstrixConfigurer2.registerApiDescriptor(MyLibraryDescriptor.class);
		AstrixContext context2 = AstrixConfigurer2.configure();
		
		HelloBeanImpl helloBean1 = (HelloBeanImpl) context.getBean(HelloBean.class);
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBean.class);
		
		assertNotSame(helloBean1, helloBean2);
	}
	
	static class IllegalDependendclass {
		@AstrixInject
		public void setVersioningPlugin() {
		}
	}
	
	static class MultipleDependentClass {
		
		private SimpleClass dep;
		private SimpleDependenctClass dep2;
		private SimpleClass dep3;

		@AstrixInject
		public void setVersioningPlugin(SimpleClass dep, SimpleDependenctClass dep2) {
			this.dep = dep;
			this.dep2 = dep2;
		}
		
		@AstrixInject
		public void setVersioningPlugin(SimpleClass dep3) {
			this.dep3 = dep3;
		}
	}
	
	static class SimpleDependenctClass {
		
		private SimpleClass dependency;

		@AstrixInject
		public void setVersioningPlugin(SimpleClass dep) {
			this.dependency = dep;
		}
	}
	
	static class SimpleClass implements AstrixSettingsAware {
		private AstrixSettingsReader settings;
		private volatile boolean destroyed = false;
		
		@Override
		public void setSettings(AstrixSettingsReader settings) {
			this.settings = settings;
		}
		
		@PreDestroy
		public void destroy() {
			this.destroyed = true;
		}
	}
	
	
	@AstrixLibraryProvider
	static class MyLibraryDescriptor {
		private HelloBeanImpl instance = new HelloBeanImpl();
		
		@AstrixExport
		public HelloBean create() {
			return instance;
		}
		
		@PreDestroy
		public void destroy() {
			instance.destroyed  = true;
		}
	}
	
	@AstrixLibraryProvider
	static class MyLibraryDescriptorNoInterface {
		
		@AstrixExport
		public HelloBeanImpl create() {
			return new HelloBeanImpl();
		}
	}
	
	@AstrixLibraryProvider
	static class CircularApiA {
		
		@AstrixExport
		public HelloBeanImpl create(GoodbyeBeanImpl goodbyeBean) {
			// we don't actually need to use goodbyeBean
			return new HelloBeanImpl();
		}
	}
	
	@AstrixLibraryProvider
	static class CircularApiB {
		
		@AstrixExport
		public GoodbyeBeanImpl create(ChatBeanImpl chatBean) {
			return new GoodbyeBeanImpl();
		}
	}
	
	@AstrixLibraryProvider
	static class CircularApiC {
		
		@AstrixExport
		public ChatBeanImpl create(HelloBeanImpl helloBean) {
			return new ChatBeanImpl();
		}
	}

	@AstrixLibraryProvider
	static class DependentApi {
		
		@AstrixExport
		public DependentBean create(NonProvidedBean bean) {
			return new DependentBean();
		}
	}
	
	@AstrixLibraryProvider
	static class IndependentApi {
		
		@AstrixExport
		public IndependentApi create() {
			return new IndependentApi();
		}
	}
	
	interface HelloBean {
		String hello(String msg);
	}
	
	static class HelloBeanImpl implements HelloBean {
		public boolean destroyed = false;

		public String hello(String msg) {
			return "hello: " + msg;
		}
	}
	
	static class GoodbyeBeanImpl {
		public String goodbye(String msg) {
			return "goodbye: " + msg;
		}
	}
	
	static class ChatBeanImpl {
		public String chat(String msg) {
			return "yada yada yada: " + msg;
		}
	}
	
	static class DependentBean {
		public String chat(String msg) {
			return "yada yada yada: " + msg;
		}
	}

	static class IndependentBean {
		public String chat(String msg) {
			return "yada yada yada: " + msg;
		}
	}

	static class NonProvidedBean {
		public String chat(String msg) {
			return "yada yada yada: " + msg;
		}
	}

}
