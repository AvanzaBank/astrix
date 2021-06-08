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

class SimpleAstrixFactoryBeanRegistryTest {
	
	@Test
	void synthesizesFactoryBeanUsingDynamicFactoryBean() {
		AstrixFactoryBeanRegistry registry = new AstrixFactoryBeanRegistry();
		registry.registerFactory(new DynamicFactoryBean<Ping>() {
			@Override
			public Ping create(AstrixBeanKey<Ping> astrixBeanKey) {
				return new Ping(astrixBeanKey.getQualifier());
			}
			@Override
			public Class<Ping> getType() {
				return Ping.class;
			}
		});
		
		StandardFactoryBean<Ping> pingFactory1 = registry.getFactoryBean(beanKey(Ping.class, "qualifier-1"));
		StandardFactoryBean<Ping> pingFactory2 = registry.getFactoryBean(beanKey(Ping.class, "qualifier-2"));
		
		assertEquals("qualifier-1", pingFactory1.create(null).qualifier);
		assertEquals("qualifier-2", pingFactory2.create(null).qualifier);
	}
	
	static class Ping {
		private final String qualifier;
		public Ping(String qualifier) {
			this.qualifier = qualifier;
		}
	}
	
	static <T> AstrixBeanKey<T> beanKey(Class<T> type, String qualifier) {
		return AstrixBeanKey.create(type, qualifier);
	}
	
}
