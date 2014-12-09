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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import javax.annotation.PreDestroy;

import org.junit.Test;

import com.avanza.astrix.provider.library.AstrixExport;
import com.avanza.astrix.provider.library.AstrixLibraryProvider;


public class AstrixLibraryTest {
	
	@Test
	public void aLibraryCanExportInterfaces() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		
		HelloBean libraryBean = AstrixContext.getBean(HelloBean.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test
	public void aLibraryCanExportClasses() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(MyLibraryProviderNoInterface.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		HelloBeanImpl libraryBean = AstrixContext.getBean(HelloBeanImpl.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test(expected = AstrixCircularDependency.class)
	public void detectsCircularDependenciesAmongLibraries() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(CircularApiA.class);
		AstrixConfigurer.registerApiProvider(CircularApiB.class);
		AstrixConfigurer.registerApiProvider(CircularApiC.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		AstrixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test
	public void doesNotForceDependenciesForUnusedApis() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(DependentApi.class);
		AstrixConfigurer.registerApiProvider(IndependentApi.class);
		AstrixContext AstrixContext = AstrixConfigurer.configure();

		IndependentApi bean = AstrixContext.getBean(IndependentApi.class);
		assertNotNull(bean);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void injectAnnotatedMethodMustAcceptAtLeastOneDependency() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixContext AstrixContext = AstrixConfigurer.configure();
		AstrixContext.getInstance(IllegalDependendClass.class);
	}
	
	@Test
	public void preDestroyAnnotatedMethodsOnLibraryFactoryInstancesAreInvokedWhenAstrixContextIsDestroyed() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		AstrixContext context = AstrixConfigurer.configure();
		
		HelloBeanImpl helloBean = (HelloBeanImpl) context.getBean(HelloBean.class);
		assertFalse(helloBean.destroyed);
		context.destroy();
		assertTrue(helloBean.destroyed);
	}
	
	@Test
	public void librariesCreatedUsingDifferentContextsShouldReturnDifferentInstances() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		AstrixContext context = AstrixConfigurer.configure();
		
		TestAstrixConfigurer AstrixConfigurer2 = new TestAstrixConfigurer();
		AstrixConfigurer2.registerApiProvider(MyLibraryProvider.class);
		AstrixContext context2 = AstrixConfigurer2.configure();
		
		HelloBeanImpl helloBean1 = (HelloBeanImpl) context.getBean(HelloBean.class);
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBean.class);
		
		assertNotSame(helloBean1, helloBean2);
	}
	
	static class IllegalDependendClass {
		@AstrixInject
		public void setVersioningPlugin() {
		}
	}
	
	@AstrixLibraryProvider
	static class MyLibraryProvider {
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
	static class MyLibraryProviderNoInterface {
		
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
