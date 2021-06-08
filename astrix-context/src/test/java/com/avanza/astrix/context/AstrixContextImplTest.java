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

import com.avanza.astrix.beans.factory.MissingBeanDependencyException;
import com.avanza.astrix.beans.factory.MissingBeanProviderException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Library;
import org.junit.jupiter.api.Test;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AstrixContextImplTest {
	
	@Test
	void detectsMissingBeans() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		assertThrows(MissingBeanProviderException.class, () -> astrixContext.getBean(HelloBeanImpl.class));
	}
	
	@Test
	void detectsMissingBeanDependencies() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(DependentApi.class);
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		assertThrows(MissingBeanDependencyException.class, () -> astrixContext.getBean(DependentBean.class));
	}
	
	@Test
	void astrixBeansAreDestroyedWhenContextIsDestroy() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		HelloBeanImpl helloBean = astrixContext.getBean(HelloBeanImpl.class);
		assertFalse(helloBean.destroyed);
		
		astrixContext.destroy();
		
		assertTrue(helloBean.destroyed);
	}
	
	@Test
	void astrixBeansAreInitializedWhenFirstCreated() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		HelloBeanImpl helloBean = astrixContext.getBean(HelloBeanImpl.class);
		assertTrue(helloBean.initialized);
	}
	
	@Test
	void createdAstrixBeansAreCached() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();

		HelloBeanImpl beanA = astrixContext.getBean(HelloBeanImpl.class);
		HelloBeanImpl beanB = astrixContext.getBean(HelloBeanImpl.class);
		assertSame(beanA, beanB);
	}
	
	@Test
	void moduleManagerIsDestroyedWhenContextIsDestroyed() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(HelloBean.class, new HelloBeanImpl());
		AstrixContextImpl astrixContext = (AstrixContextImpl) astrixConfigurer.configure();
		HelloBean helloBean = astrixContext.getInstance(HelloBean.class); // treat HelloBean as internal class
		assertFalse(helloBean.isDestroyed());
		
		astrixContext.destroy();
		assertTrue(helloBean.isDestroyed());
	}
	
	@Test
	void manyAstrixContextShouldBeAbleToRunInSameProcessWithoutInterferenceInShutdown() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiProvider(HelloBeanLibrary.class);
		AstrixContext context = astrixConfigurer.configure();
		
		TestAstrixConfigurer astrixConfigurer2 = new TestAstrixConfigurer();
		astrixConfigurer2.registerApiProvider(HelloBeanLibrary.class);
		AstrixContextImpl context2 = (AstrixContextImpl) astrixConfigurer2.configure();
		context.destroy();
		
		HelloBeanImpl helloBean2 = context2.getBean(HelloBeanImpl.class);
		assertFalse(helloBean2.destroyed);
		
		context2.destroy();
		assertTrue(helloBean2.destroyed);
	}
	
	@Test
	void throwsIllegalStateExceptionWhenStartingServicePublisherForNonServer() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		AstrixApplicationContext context = (AstrixApplicationContext) astrixConfigurer.configure();
		assertThrows(IllegalStateException.class, context::startServicePublisher);
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
		boolean isDestroyed();
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
		public boolean isDestroyed() {
			return destroyed;
		}
	}

}
