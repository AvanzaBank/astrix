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

import com.avanza.astrix.context.AsterixCircularDependency;
import com.avanza.astrix.context.AsterixContext;
import com.avanza.astrix.context.AsterixInject;
import com.avanza.astrix.context.AsterixSettingsAware;
import com.avanza.astrix.context.AsterixSettingsReader;
import com.avanza.astrix.context.MissingBeanDependencyException;
import com.avanza.astrix.context.MissingBeanProviderException;
import com.avanza.astrix.context.TestAsterixConfigurer;
import com.avanza.astrix.provider.library.AsterixExport;
import com.avanza.astrix.provider.library.AsterixLibraryProvider;


public class AsterixTest {
	
	@Test
	public void asterixSupportSimpleLibraries() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext asterixContext = asterixConfigurer.configure();
		
		HelloBean libraryBean = asterixContext.getBean(HelloBean.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test
	public void supportsLibrariesExportedUsingClasses() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(MyLibraryDescriptorNoInterface.class);
		AsterixContext asterixContext = asterixConfigurer.configure();

		HelloBeanImpl libraryBean = asterixContext.getBean(HelloBeanImpl.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test(expected = AsterixCircularDependency.class)
	public void detectsCircularDependenciesAmoungLibraries() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(CircularApiA.class);
		asterixConfigurer.registerApiDescriptor(CircularApiB.class);
		asterixConfigurer.registerApiDescriptor(CircularApiC.class);
		AsterixContext asterixContext = asterixConfigurer.configure();

		asterixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test(expected = MissingBeanDependencyException.class)
	public void detectsMissingBeanDependencies() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(DependentApi.class);
		AsterixContext asterixContext = asterixConfigurer.configure();

		asterixContext.getBean(DependentBean.class);
	}
	
	@Test(expected = MissingBeanProviderException.class)
	public void detectsMissingBeans() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixContext asterixContext = asterixConfigurer.configure();

		asterixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test
	public void doesNotForceDependenciesForUnusedApis() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(DependentApi.class);
		asterixConfigurer.registerApiDescriptor(IndependentApi.class);
		AsterixContext asterixContext = asterixConfigurer.configure();

		IndependentApi bean = asterixContext.getBean(IndependentApi.class);
		assertNotNull(bean);
	}
	
	@Test
	public void cachesCreatedApis() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext asterixContext = asterixConfigurer.configure();

		HelloBean beanA = asterixContext.getBean(HelloBean.class);
		HelloBean beanB = asterixContext.getBean(HelloBean.class);
		assertSame(beanA, beanB);
	}
	
	@Test
	public void canInitRegularClasses() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixContext asterixContext = asterixConfigurer.configure();
		SimpleClass simple = asterixContext.getInstance(SimpleClass.class);
		
		assertNotNull(simple.settings);
	}
	
	@Test
	public void cachesCreatedInstancesOfRegularClasses() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixContext asterixContext = asterixConfigurer.configure();
		SimpleClass simple1 = asterixContext.getInstance(SimpleClass.class);
		SimpleClass simple2 = asterixContext.getInstance(SimpleClass.class);
		
		assertSame(simple1, simple2);
	}
	
	@Test
	public void asterixContextShouldInstantiateAndInjectRequiredClasses() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixContext asterixContext = asterixConfigurer.configure();
		MultipleDependentClass dependentClass = asterixContext.getInstance(MultipleDependentClass.class);
		
		assertNotNull(dependentClass.dep);
		assertNotNull(dependentClass.dep2);
		assertNotNull(dependentClass.dep3);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void injectAnnotatedMethodMustAcceptAtLeastOneDependency() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixContext asterixContext = asterixConfigurer.configure();
		asterixContext.getInstance(IllegalDependendclass.class);
	}
	
	@Test
	public void preDestroyAnnotatedMethodsOnInstancesCreatedByAsterixAreInvokedWhenAsterixContextIsDestroyed() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		AsterixContext asterixContext = asterixConfigurer.configure();
		SimpleClass simpleClass = asterixContext.getInstance(SimpleClass.class);
		assertFalse(simpleClass.destroyed);
		
		asterixContext.destroy();
		assertTrue(simpleClass.destroyed);
	}
	
	@Test
	public void preDestroyAnnotatedMethodsOnLibraryFactoryInstancesAreInvokedWhenAsterixContextIsDestroyed() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext context = asterixConfigurer.configure();
		
		HelloBeanImpl helloBean = (HelloBeanImpl) context.getBean(HelloBean.class);
		assertFalse(helloBean.destroyed);
		context.destroy();
		assertTrue(helloBean.destroyed);
	}
	
	@Test
	public void manyAsterixContextShouldBeAbleToRunInSameProcessWithoutInterferenceInShutdown() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext context = asterixConfigurer.configure();
		
		TestAsterixConfigurer asterixConfigurer2 = new TestAsterixConfigurer();
		asterixConfigurer2.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext context2 = asterixConfigurer2.configure();
		context.destroy();
		
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBean.class);
		assertFalse(helloBean2.destroyed);
		context2.destroy();
		assertTrue(helloBean2.destroyed);
	}
	
	@Test
	public void librariesCreatedUsingDifferentContextsShouldReturnDifferentInstances() throws Exception {
		TestAsterixConfigurer asterixConfigurer = new TestAsterixConfigurer();
		asterixConfigurer.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext context = asterixConfigurer.configure();
		
		TestAsterixConfigurer asterixConfigurer2 = new TestAsterixConfigurer();
		asterixConfigurer2.registerApiDescriptor(MyLibraryDescriptor.class);
		AsterixContext context2 = asterixConfigurer2.configure();
		
		HelloBeanImpl helloBean1 = (HelloBeanImpl) context.getBean(HelloBean.class);
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBean.class);
		
		assertNotSame(helloBean1, helloBean2);
	}
	
	static class IllegalDependendclass {
		@AsterixInject
		public void setVersioningPlugin() {
		}
	}
	
	static class MultipleDependentClass {
		
		private SimpleClass dep;
		private SimpleDependenctClass dep2;
		private SimpleClass dep3;

		@AsterixInject
		public void setVersioningPlugin(SimpleClass dep, SimpleDependenctClass dep2) {
			this.dep = dep;
			this.dep2 = dep2;
		}
		
		@AsterixInject
		public void setVersioningPlugin(SimpleClass dep3) {
			this.dep3 = dep3;
		}
	}
	
	static class SimpleDependenctClass {
		
		private SimpleClass dependency;

		@AsterixInject
		public void setVersioningPlugin(SimpleClass dep) {
			this.dependency = dep;
		}
	}
	
	static class SimpleClass implements AsterixSettingsAware {
		private AsterixSettingsReader settings;
		private volatile boolean destroyed = false;
		
		@Override
		public void setSettings(AsterixSettingsReader settings) {
			this.settings = settings;
		}
		
		@PreDestroy
		public void destroy() {
			this.destroyed = true;
		}
	}
	
	
	@AsterixLibraryProvider
	static class MyLibraryDescriptor {
		private HelloBeanImpl instance = new HelloBeanImpl();
		
		@AsterixExport
		public HelloBean create() {
			return instance;
		}
		
		@PreDestroy
		public void destroy() {
			instance.destroyed  = true;
		}
	}
	
	@AsterixLibraryProvider
	static class MyLibraryDescriptorNoInterface {
		
		@AsterixExport
		public HelloBeanImpl create() {
			return new HelloBeanImpl();
		}
	}
	
	@AsterixLibraryProvider
	static class CircularApiA {
		
		@AsterixExport
		public HelloBeanImpl create(GoodbyeBeanImpl goodbyeBean) {
			// we don't actually need to use goodbyeBean
			return new HelloBeanImpl();
		}
	}
	
	@AsterixLibraryProvider
	static class CircularApiB {
		
		@AsterixExport
		public GoodbyeBeanImpl create(ChatBeanImpl chatBean) {
			return new GoodbyeBeanImpl();
		}
	}
	
	@AsterixLibraryProvider
	static class CircularApiC {
		
		@AsterixExport
		public ChatBeanImpl create(HelloBeanImpl helloBean) {
			return new ChatBeanImpl();
		}
	}

	@AsterixLibraryProvider
	static class DependentApi {
		
		@AsterixExport
		public DependentBean create(NonProvidedBean bean) {
			return new DependentBean();
		}
	}
	
	@AsterixLibraryProvider
	static class IndependentApi {
		
		@AsterixExport
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
