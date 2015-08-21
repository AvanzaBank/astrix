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

import com.avanza.astrix.beans.ft.CheckedCommand;
import com.avanza.astrix.beans.ft.CommandSettings;
import com.avanza.astrix.beans.ft.FaultToleranceSpi;
import com.avanza.astrix.beans.ft.IsolationStrategy;
import com.avanza.astrix.core.function.Supplier;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.HystrixThreadPoolProperties;

import rx.Observable;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class HystrixFaultTolerance implements FaultToleranceSpi {
	
	@Override
	public <T> Observable<T> observe(Supplier<Observable<T>> observable, CommandSettings settings) {
		Setter setter = Setter.withGroupKey(getGroupKey(settings))
				  .andCommandKey(getCommandKey(settings))
				  .andCommandPropertiesDefaults(createCommandProperties(settings));
		return HystrixObservableCommandFacade.observe(observable, setter);
	}

	@Override
	public <T> T execute(final CheckedCommand<T> command, CommandSettings settings) throws Throwable {
		return HystrixCommandFacade.execute(command, createHystrixConfiguration(settings));
	}
	
	private com.netflix.hystrix.HystrixCommand.Setter createHystrixConfiguration(CommandSettings settings) {
		HystrixCommandProperties.Setter commandPropertiesDefault = createCommandProperties(settings);
						
		// MaxQueueSize must be set to a non negative value in order for QueueSizeRejectionThreshold to have any effect.
		// We use a high value for MaxQueueSize in order to allow QueueSizeRejectionThreshold to change dynamically using archaius.
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults = createThreadPoolProperties(settings);

		return com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(getGroupKey(settings))
				.andCommandKey(getCommandKey(settings))
				.andCommandPropertiesDefaults(commandPropertiesDefault)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}

	private HystrixThreadPoolProperties.Setter createThreadPoolProperties(CommandSettings settings) {
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults =
				HystrixThreadPoolProperties.Setter()
						.withMaxQueueSize(settings.getMaxQueueSize())
						.withQueueSizeRejectionThreshold(settings.getQueueSizeRejectionThreshold())
						.withCoreSize(settings.getCoreSize());
		return threadPoolPropertiesDefaults;
	}

	private HystrixCommandProperties.Setter createCommandProperties(CommandSettings settings) {
		HystrixCommandProperties.Setter commandPropertiesDefault =
				HystrixCommandProperties.Setter()
						.withExecutionIsolationSemaphoreMaxConcurrentRequests(settings.getSemaphoreMaxConcurrentRequests())
						.withExecutionIsolationStrategy(getHystrixIsolationStrategy(settings))
						.withExecutionTimeoutInMilliseconds(settings.getInitialTimeoutInMilliseconds());
		return commandPropertiesDefault;
	}
	
	private HystrixCommandGroupKey getGroupKey(CommandSettings settings) {
		return HystrixCommandGroupKey.Factory.asKey(settings.getGroupName());
	}

	private HystrixCommandKey getCommandKey(CommandSettings settings) {
		return HystrixCommandKey.Factory.asKey(settings.getCommandName());
	}

	private ExecutionIsolationStrategy getHystrixIsolationStrategy(CommandSettings settings) {
		return settings.getExecutionIsolationStrategy() == IsolationStrategy.SEMAPHORE 
				? ExecutionIsolationStrategy.SEMAPHORE
				: ExecutionIsolationStrategy.THREAD;
	}
	
}
