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
package com.avanza.astrix.beans.factory;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AstrixBeanFactoryTest {
	
	
	@Test
	void detectsCircularDependencies1() {
		/*    __________________
		 *   |                  |
		 *   v                  |
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<>(Ping.class) {
			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class));
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class));
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				context.getBean(beanKey(Ping.class));
				return new PingPong();
			}
		};
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory();
		beanFactory.registerFactory(pingFactory);
		beanFactory.registerFactory(pongFactory);
		beanFactory.registerFactory(pingpongFactory);
		
		assertThrows(CircularDependency.class, () -> beanFactory.getBean(beanKey(Ping.class)));
	}
	
	@Test
	void detectsCircularDependencies2() {
		/*             _________
		 *            |         |
		 *            v         |
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<>(Ping.class) {


			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class)); // "Depend on Pong";
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class)); // "Depend on PingPong";
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class)); // "Depend on Pong";
				return new PingPong();
			}
		};
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory();
		beanFactory.registerFactory(pingFactory);
		beanFactory.registerFactory(pongFactory);
		beanFactory.registerFactory(pingpongFactory);
		
		assertThrows(CircularDependency.class, () -> beanFactory.getBean(beanKey(Ping.class)));
	}
	
	@Test
	void nonCircularDependency() {
		/*    __________________
		 *   |                  |
		 *   |                  v
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<>(Ping.class) {

			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class));
				context.getBean(beanKey(PingPong.class));
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class)); // "Depend on PingPong";
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				return new PingPong();
			}
		};
		
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory();
		beanFactory.registerFactory(pingFactory);
		beanFactory.registerFactory(pongFactory);
		beanFactory.registerFactory(pingpongFactory);
		beanFactory.getBean(beanKey(Ping.class));
	}
	
	@Test
	void cachesCreatedBeans() {
		/*    __________________
		 *   |                  |
		 *   |                  v
		 * Ping --> Pong --> PingPong
		 */
		SimpleAstrixFactoryBean<Ping> pingFactory = new SimpleAstrixFactoryBean<>(Ping.class) {

			@Override
			public Ping create(AstrixBeans context) {
				context.getBean(beanKey(Pong.class));
				context.getBean(beanKey(PingPong.class));
				return new Ping();
			}
		};
		SimpleAstrixFactoryBean<Pong> pongFactory = new SimpleAstrixFactoryBean<>(Pong.class) {
			@Override
			public Pong create(AstrixBeans context) {
				context.getBean(beanKey(PingPong.class));
				return new Pong();
			}
		};
		SimpleAstrixFactoryBean<PingPong> pingpongFactory = new SimpleAstrixFactoryBean<>(PingPong.class) {
			@Override
			public PingPong create(AstrixBeans context) {
				creationCount++;
				return new PingPong();
			}
		};
		
		AstrixBeanFactory beanFactory = new AstrixBeanFactory();
		beanFactory.registerFactory(pingFactory);
		beanFactory.registerFactory(pongFactory);
		beanFactory.registerFactory(pingpongFactory);
		
		beanFactory.getBean(beanKey(Ping.class));
		
		assertEquals(1, pingpongFactory.creationCount);
	}
	
	private static class Ping {
	}
	
	private static class Pong {
	}
	
	private static class PingPong {
	}
	
	static <T> AstrixBeanKey<T> beanKey(Class<T> type) {
		return AstrixBeanKey.create(type, null);
	}
	
	
	abstract static class SimpleAstrixFactoryBean<T> implements StandardFactoryBean<T> {

		private final AstrixBeanKey<T> key;
		int creationCount = 0;
		
		SimpleAstrixFactoryBean(Class<T> type) {
			this.key = AstrixBeanKey.create(type, null);
		}

		@Override
		public AstrixBeanKey<T> getBeanKey() {
			return this.key;
		}
		
	}

}
