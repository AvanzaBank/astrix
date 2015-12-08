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
package com.avanza.astrix.context;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import javax.annotation.PreDestroy;

import org.junit.Test;

import com.avanza.astrix.beans.core.BasicFuture;
import com.avanza.astrix.beans.factory.CircularDependency;
import com.avanza.astrix.beans.ft.BeanFaultTolerance;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.core.function.CheckedCommand;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;

import rx.Observable;


public class AstrixLibraryTest {
	
	@Test
	public void aLibraryCanExportInterfaces() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		AstrixContextImpl AstrixContext = (AstrixContextImpl) AstrixConfigurer.configure();
		
		HelloBean libraryBean = AstrixContext.getBean(HelloBean.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test
	public void aLibraryCanExportClasses() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(MyLibraryProviderNoInterface.class);
		AstrixContextImpl AstrixContext = (AstrixContextImpl) AstrixConfigurer.configure();

		HelloBeanImpl libraryBean = AstrixContext.getBean(HelloBeanImpl.class);
		assertEquals("hello: kalle", libraryBean.hello("kalle"));
	}
	
	@Test(expected = CircularDependency.class)
	public void detectsCircularDependenciesAmongLibraries() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(CircularApiA.class);
		AstrixConfigurer.registerApiProvider(CircularApiB.class);
		AstrixConfigurer.registerApiProvider(CircularApiC.class);
		AstrixContextImpl AstrixContext = (AstrixContextImpl) AstrixConfigurer.configure();

		AstrixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test
	public void doesNotForceDependenciesForUnusedApis() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(DependentApi.class);
		AstrixConfigurer.registerApiProvider(IndependentApi.class);
		AstrixContextImpl AstrixContext = (AstrixContextImpl) AstrixConfigurer.configure();

		IndependentApi bean = AstrixContext.getBean(IndependentApi.class);
		assertNotNull(bean);
	}
	
	@Test
	public void preDestroyAnnotatedMethodsOnLibraryFactoryInstancesAreInvokedWhenAstrixContextIsDestroyed() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		AstrixContextImpl context = (AstrixContextImpl) astrixConfigurer.configure();
		
		HelloBeanImpl helloBean = (HelloBeanImpl) context.getBean(HelloBean.class);
		assertFalse(helloBean.destroyed);
		context.destroy();
		assertTrue(helloBean.destroyed);
	}
	
	@Test
	public void librariesCreatedUsingDifferentContextsShouldReturnDifferentInstances() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(MyLibraryProvider.class);
		AstrixContextImpl context = (AstrixContextImpl) astrixConfigurer.configure();
		
		TestAstrixConfigurer astrixConfigurer2 = new TestAstrixConfigurer();
		astrixConfigurer2.registerApiProvider(MyLibraryProvider.class);
		AstrixContextImpl context2 = (AstrixContextImpl) astrixConfigurer2.configure();
		
		HelloBeanImpl helloBean1 = (HelloBeanImpl) context.getBean(HelloBean.class);
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBean.class);
		
		assertNotSame(helloBean1, helloBean2);
	}
	
	@Test
	public void appliesFaultToleranceProxyToAstrixFaultToleranceProxyAnnotatedLibraries() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(MyLibraryProviderWithFaultTolerance.class);
		astrixConfigurer.enableFaultTolerance(true);
		final AtomicInteger appliedOberveCount = new AtomicInteger(0);
		final AtomicInteger appliedExecuteCount = new AtomicInteger(0);
		BeanFaultTolerance beanFt = new BeanFaultTolerance() {
			@Override
			public <T> Observable<T> observe(Supplier<Observable<T>> observable) {
				appliedOberveCount.incrementAndGet();
				return observable.get();
			}
			@Override
			public <T> T execute(CheckedCommand<T> command) throws Throwable {
				appliedExecuteCount.incrementAndGet();
				return command.call();
			}
		};
		astrixConfigurer.registerStrategy(BeanFaultToleranceFactorySpi.class, beanKey -> beanFt);
		AstrixContext context = astrixConfigurer.configure();
		
		HelloBean helloBean= context.getBean(HelloBean.class);
		
		assertEquals(0, appliedExecuteCount.get());
		assertEquals("hello: bar", helloBean.hello("bar"));
		assertEquals(1, appliedExecuteCount.get());
		
		assertEquals(1, appliedExecuteCount.get());
		assertEquals("hello: bar", helloBean.asyncHello("bar").get());
		assertEquals(2, appliedExecuteCount.get());
	}
	
	@Test
	public void doesNotApplyFaultToleranceProxyToNonAstrixFaultToleranceProxyAnnotatedLibraries() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.enableFaultTolerance(true);
		astrixConfigurer.registerApiProvider(MyLibraryProvider.class); // No Fault tolerance
		final AtomicInteger appliedOberveCount = new AtomicInteger(0);
		final AtomicInteger appliedExecuteCount = new AtomicInteger(0);
		BeanFaultTolerance beanFt = new BeanFaultTolerance() {
			@Override
			public <T> Observable<T> observe(Supplier<Observable<T>> observable) {
				appliedOberveCount.incrementAndGet();
				return observable.get();
			}
			@Override
			public <T> T execute(CheckedCommand<T> command) throws Throwable {
				appliedExecuteCount.incrementAndGet();
				return command.call();
			}
		};
		astrixConfigurer.registerStrategy(BeanFaultToleranceFactorySpi.class, beanKey -> beanFt);
		AstrixContext context = astrixConfigurer.configure();
		
		HelloBean helloBean= context.getBean(HelloBean.class);
		
		assertEquals(0, appliedExecuteCount.get());
		assertEquals("hello: bar", helloBean.hello("bar"));
		assertEquals(0, appliedExecuteCount.get());
		
		assertEquals(0, appliedOberveCount.get());
		assertEquals("hello: bar", helloBean.asyncHello("bar").get());
		assertEquals(0, appliedOberveCount.get());
		
	}
	
	@AstrixApiProvider
	public static class MyLibraryProvider {
		private HelloBeanImpl instance = new HelloBeanImpl();
		
		@Library
		public HelloBean create() {
			return instance;
		}
		
		@PreDestroy
		public void destroy() {
			instance.destroyed  = true;
		}
	}
	
	@AstrixApiProvider
	public static class MyLibraryProviderNoInterface {
		
		@Library
		public HelloBeanImpl create() {
			return new HelloBeanImpl();
		}
	}
	
	@AstrixApiProvider
	public static class MyLibraryProviderWithFaultTolerance {
		
		@AstrixFaultToleranceProxy
		@Library
		public HelloBean create() {
			return new HelloBeanImpl();
		}
	}
	
	@AstrixApiProvider
	public static class CircularApiA {
		
		@Library
		public HelloBeanImpl create(GoodbyeBeanImpl goodbyeBean) {
			// we don't actually need to use goodbyeBean
			return new HelloBeanImpl();
		}
	}
	
	@AstrixApiProvider
	public static class CircularApiB {
		
		@Library
		public GoodbyeBeanImpl create(ChatBeanImpl chatBean) {
			return new GoodbyeBeanImpl();
		}
	}
	
	@AstrixApiProvider
	public static class CircularApiC {
		
		@Library
		public ChatBeanImpl create(HelloBeanImpl helloBean) {
			return new ChatBeanImpl();
		}
	}

	@AstrixApiProvider
	public  static class DependentApi {
		
		@Library
		public DependentBean create(NonProvidedBean bean) {
			return new DependentBean();
		}
	}
	
	@AstrixApiProvider
	public static class IndependentApi {
		
		@Library
		public IndependentApi create() {
			return new IndependentApi();
		}
	}
	
	public interface HelloBean {
		String hello(String msg);
		Future<String> asyncHello(String msg);
	}
	
	static class HelloBeanImpl implements HelloBean {
		public boolean destroyed = false;

		public String hello(String msg) {
			return "hello: " + msg;
		}
		
		@Override
		public Future<String> asyncHello(String msg) {
			return new BasicFuture<>(hello(msg));
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
