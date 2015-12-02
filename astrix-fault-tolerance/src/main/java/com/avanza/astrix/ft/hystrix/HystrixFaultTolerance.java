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
package com.avanza.astrix.ft.hystrix;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.core.function.CheckedCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixObservableCommand.Setter;

import rx.Observable;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class HystrixFaultTolerance implements FaultToleranceSpi {

	/*
	 * Astrix allows multiple AstrixContext within the same JVM, although
	 * more than one AstrixContext within a single JVM is most often only used during unit testing. However,
	 * since Astrix allows multiple AstrixContext's we want those to be isolated from each other. Hystrix only 
	 * allows registering a global strategy for each exposed SPI. Therefore Astrix registers a global "dispatcher"
	 * strategy (see HystrixPluginDispatcher) that dispatches each invocation to a HystrixPlugin to
	 * the HystrixPlugin instance associated with a given AstrixContext.
	 * 
	 * The "id" property is used by the dispatcher to identify the HystrixPlugin instances associated with a given
	 * AstrixContext.
	 */
	
	private static final AtomicInteger idGenerator = new AtomicInteger(0);
	
	private final BeanMapping beanMapping;
	private final HystrixCommandKeyFactory hystrixCommandKeyFactory;
	private final String id;
	
	public HystrixFaultTolerance(HystrixCommandNamingStrategy hystrixCommandNamingStrategy,
								 AstrixConcurrencyStrategy concurrencyStrategy,
								 BeanConfigurationPropertiesStrategy propertiesStrategy,
								 BeanMapping beanMapping) {
		this.id = Integer.toString(idGenerator.incrementAndGet()); 
		this.beanMapping = beanMapping;
		HystrixStrategies hystrixStrategies = new HystrixStrategies(propertiesStrategy, 
																	concurrencyStrategy, 
																	new FailedServiceInvocationLogger(beanMapping), 
																	id);
		HystrixStrategyDispatcher.registerStrategies(hystrixStrategies);
		this.hystrixCommandKeyFactory = new HystrixCommandKeyFactory(id, hystrixCommandNamingStrategy);
	}
	
	@Override
	public <T> Observable<T> observe(Supplier<Observable<T>> observable, AstrixBeanKey<?> beanKey) {
		Setter setter = Setter.withGroupKey(getGroupKey(beanKey))
				  			  .andCommandKey(getCommandKey(beanKey))
				  			  .andCommandPropertiesDefaults(
				  					  HystrixCommandProperties.Setter().withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE));
		return HystrixObservableCommandFacade.observe(observable, setter);
	}

	@Override
	public <T> T execute(final CheckedCommand<T> command, AstrixBeanKey<?> beanKey) throws Throwable {
		com.netflix.hystrix.HystrixCommand.Setter setter =  
				com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(getGroupKey(beanKey))
								.andCommandKey(getCommandKey(beanKey))
								.andCommandPropertiesDefaults(
					  					  HystrixCommandProperties.Setter().withExecutionIsolationStrategy(ExecutionIsolationStrategy.THREAD));
		return HystrixCommandFacade.execute(command, setter);
	}
	
	HystrixCommandGroupKey getGroupKey(AstrixBeanKey<?> beanKey) {
		HystrixCommandGroupKey result = hystrixCommandKeyFactory.createGroupKey(beanKey);
		this.beanMapping.registerBeanKey(result.name(), beanKey);
		return result;
	}

	HystrixCommandKey getCommandKey(AstrixBeanKey<?> beanKey) {
		HystrixCommandKey result =  hystrixCommandKeyFactory.createCommandKey(beanKey);
		this.beanMapping.registerBeanKey(result.name(), beanKey);
		return result;
	}

}
