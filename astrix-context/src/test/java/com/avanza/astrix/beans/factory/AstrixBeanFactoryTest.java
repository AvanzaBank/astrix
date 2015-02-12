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
package com.avanza.astrix.beans.factory;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.avanza.astrix.beans.factory.AstrixBeanFactory;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixCircularDependency;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.beans.factory.SimpleAstrixFactoryBeanRegistry;
import com.avanza.astrix.config.DynamicLongProperty;



public class AstrixBeanFactoryTest {
	
	private DynamicLongProperty beanBindAttemptInterval = new DynamicLongProperty(100);
	
	@Test(expected = AstrixCircularDependency.class)
	public void detectsCircularDependencies1() throws Exception {
		/*    __________________
		 *   |                  |
		 *   v                  |
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<Ping>(Ping.class) {
			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class));
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<Pong>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class)); 
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<PingPong>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				context.getBean(beanKey(Ping.class));
				return new PingPong();
			}
		};
		
		SimpleAstrixFactoryBeanRegistry registry = new SimpleAstrixFactoryBeanRegistry();
		registry.registerFactory(pingFactory);
		registry.registerFactory(pongFactory);
		registry.registerFactory(pingpongFactory);
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory(registry);
		beanFactory.getBean(beanKey(Ping.class));
	}
	
	@Test(expected = AstrixCircularDependency.class)
	public void detectsCircularDependencies2() throws Exception {
		/*             _________
		 *            |         |
		 *            v         |
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<Ping>(Ping.class) {
			
			
			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class)); // "Depend on Pong";
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<Pong>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class)); // "Depend on PingPong";
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<PingPong>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class)); // "Depend on Pong";
				return new PingPong();
			}
		};
		
		SimpleAstrixFactoryBeanRegistry registry = new SimpleAstrixFactoryBeanRegistry();
		registry.registerFactory(pingFactory);
		registry.registerFactory(pongFactory);
		registry.registerFactory(pingpongFactory);
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory(registry);
		beanFactory.getBean(beanKey(Ping.class));
	}
	
	@Test
	public void nonCircularDependency() throws Exception {
		/*    __________________
		 *   |                  |
		 *   |                  v
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<Ping>(Ping.class) {
			
			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class));
				context.getBean(beanKey(PingPong.class));
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<Pong>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class)); // "Depend on PingPong";
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<PingPong>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				return new PingPong();
			}
		};
		
		SimpleAstrixFactoryBeanRegistry registry = new SimpleAstrixFactoryBeanRegistry();
		registry.registerFactory(pingFactory);
		registry.registerFactory(pongFactory);
		registry.registerFactory(pingpongFactory);
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory(registry);
		beanFactory.getBean(beanKey(Ping.class));
	}
	
	@Test
	public void cachesCreatedBeans() throws Exception {
		/*    __________________
		 *   |                  |
		 *   |                  v
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<Ping>(Ping.class) {
			
			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class));
				context.getBean(beanKey(PingPong.class));
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<Pong>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class));
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<PingPong>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				creationCount++;
				return new PingPong();
			}
		};
		
		SimpleAstrixFactoryBeanRegistry registry = new SimpleAstrixFactoryBeanRegistry();
		registry.registerFactory(pingFactory);
		registry.registerFactory(pongFactory);
		registry.registerFactory(pingpongFactory);
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory(registry);
		beanFactory.getBean(beanKey(Ping.class));
		
		assertEquals(1, pingpongFactory.creationCount);
	}
	
	static class Ping {
	}
	
	static class Pong {
	}
	
	static class PingPong {
	}
	
	static <T> AstrixBeanKey<T> beanKey(Class<T> type) {
		return AstrixBeanKey.create(type, null);
	}
	
	
	
	static abstract class SimpleAstrixFactoryBean<T> implements AstrixFactoryBean<T> {

		private AstrixBeanKey<T> key;
		int creationCount = 0;
		
		public SimpleAstrixFactoryBean(Class<T> type) {
			this.key = AstrixBeanKey.create(type, null);
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return this.key;
		}

	}

}
