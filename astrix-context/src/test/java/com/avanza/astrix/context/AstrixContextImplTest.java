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

import static junit.framework.Assert.assertSame;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Test;

import com.avanza.astrix.beans.factory.MissingBeanDependencyException;
import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.context.module.AstrixInject;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;

public class AstrixContextImplTest {
	
	@Test(expected = MissingBeanProviderException.class)
	public void detectsMissingBeans() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		AstrixContextImpl AstrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		AstrixContext.getBean(HelloBeanImpl.class);
	}
	
	@Test(expected = MissingBeanDependencyException.class)
	public void detectsMissingBeanDependencies() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(DependentApi.class);
		AstrixContextImpl AstrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		AstrixContext.getBean(DependentBean.class);
	}
	
	@Test
	public void astrixBeansAreDestroyedWhenContextIsDestroy() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		HelloBeanImpl helloBean = astrixContext.getBean(HelloBeanImpl.class);
		assertFalse(helloBean.destroyed);
		
		astrixContext.destroy();
		
		assertTrue(helloBean.destroyed);
	}
	
	@Test
	public void astrixBeansAreInitializedWhenFirstCreated() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		HelloBeanImpl helloBean = astrixContext.getBean(HelloBeanImpl.class);
		assertTrue(helloBean.initialized);
	}
	
	@Test
	public void createdAstrixBeansAreCached() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl AstrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		HelloBeanImpl beanA = AstrixContext.getBean(HelloBeanImpl.class);
		HelloBeanImpl beanB = AstrixContext.getBean(HelloBeanImpl.class);
		assertSame(beanA, beanB);
	}
	
	@Test
	public void moduleManagerIsDestroyedWhenContextIsDestroyed() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(HelloBean.class, new HelloBeanImpl());
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();
		HelloBean helloBean = astrixContext.getInstance(HelloBean.class); // treat HelloBean as internal class
		assertFalse(helloBean.isDestoryed());
		
		astrixContext.destroy();
		assertTrue(helloBean.isDestoryed());
	}
	
	@Test
	public void manyAstrixContextShouldBeAbleToRunInSameProcessWithoutInterferenceInShutdown() throws Exception {
		TestAstrixConfigurer AstrixConfigurer = new TestAstrixConfigurer();
		AstrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContext context = AstrixConfigurer.configure();
		
		TestAstrixConfigurer astrixConfigurer2 = new TestAstrixConfigurer();
		astrixConfigurer2.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl context2 = (AstrixContextImpl) astrixConfigurer2.configure();
		context.destroy();
		
		HelloBeanImpl helloBean2 = (HelloBeanImpl) context2.getBean(HelloBeanImpl.class);
		assertFalse(helloBean2.destroyed);
		
		context2.destroy();
		assertTrue(helloBean2.destroyed);
	}
	
	@AstrixApiProvider
	public static class DependentApi {
		
		@Library
		public DependentBean create(GoodbyeBean bean) {
			return new DependentBean();
		}
	}
	
	public static class DependentBean {
		public String chat(String msg) {
			return "yada yada yada: " + msg;
		}
	}
	
	public static class GoodbyeBean {
		public String goodbye() {
			return "goodbye";
		}
	}
	
	@AstrixApiProvider
	public static class HelloBeanLibrary {
		@Library
		public HelloBeanImpl create() {
			return new HelloBeanImpl();
		}
	}
	
	public interface HelloBean {
		boolean isDestoryed();
	}
	
	public static class HelloBeanImpl implements HelloBean {
		
		boolean initialized = false;
		boolean destroyed = false;
		public String chat(String msg) {
			return "yada yada yada: " + msg;
		}
		
		@PostConstruct
		public void init() {
			this.initialized = true;
		}
		
		@PreDestroy
		public void destroyed() {
			this.destroyed = true;
		}

		@Override
		public boolean isDestoryed() {
			return destroyed;
		}
	}

}
