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
package com.avanza.hystrix.multiconfig;

import org.junit.Test;
import org.mockito.Mockito;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

public class MultiPropertiesStrategyDispatcherTest {

	private final MultiPropertiesStrategyDispatcher dispatcher = new MultiPropertiesStrategyDispatcher();
	
	private final com.netflix.hystrix.HystrixCommandProperties.Setter defaultCommandSetter = com.netflix.hystrix.HystrixCommandProperties.defaultSetter();
	private final com.netflix.hystrix.HystrixThreadPoolProperties.Setter defaultThreadPoolSetter = com.netflix.hystrix.HystrixThreadPoolProperties.defaultSetter();
	private final com.netflix.hystrix.HystrixCollapserProperties.Setter defaultCollapserSetter = com.netflix.hystrix.HystrixCollapserProperties.defaultSetter();
	
	@Test
	public void decodesIncomingCommandKeys() throws Exception {
		HystrixPropertiesStrategy mock = Mockito.mock(HystrixPropertiesStrategy.class);
		MultiConfigId configId = MultiConfigId.create("t1");
		
		dispatcher.register(configId.toString(), mock);
		dispatcher.getCommandProperties(configId.createCommandKey("com.avanza.test.TestApi.TestService"), defaultCommandSetter);
		
		Mockito.verify(mock).getCommandProperties(HystrixCommandKey.Factory.asKey("com.avanza.test.TestApi.TestService"), defaultCommandSetter);
	}
	
	@Test
	public void decodesIncomingThreadPoolKeys() throws Exception {
		HystrixPropertiesStrategy mock = Mockito.mock(HystrixPropertiesStrategy.class);
		MultiConfigId configId = MultiConfigId.create("t1");
		
		dispatcher.register(configId.toString(), mock);
		dispatcher.getThreadPoolProperties(configId.createThreadPoolKey("com.avanza.test.TestApi.TestService"), defaultThreadPoolSetter);
		
		Mockito.verify(mock).getThreadPoolProperties(HystrixThreadPoolKey.Factory.asKey("com.avanza.test.TestApi.TestService"), defaultThreadPoolSetter);
	}
	
	@Test
	public void decodesIncomingCollapserKeys() throws Exception {
		HystrixPropertiesStrategy mock = Mockito.mock(HystrixPropertiesStrategy.class);
		MultiConfigId configId = MultiConfigId.create("t1");
		
		dispatcher.register(configId.toString(), mock);
		dispatcher.getCollapserProperties(configId.createCollapserKey("com.avanza.test.TestApi.TestService"), defaultCollapserSetter);
		
		Mockito.verify(mock).getCollapserProperties(HystrixCollapserKey.Factory.asKey("com.avanza.test.TestApi.TestService"), defaultCollapserSetter);
	}
	
}
