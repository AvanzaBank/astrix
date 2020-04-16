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

import java.util.Objects;
import java.util.function.Supplier;

import com.avanza.astrix.beans.async.ContextPropagation;
import com.avanza.astrix.beans.ft.BeanFaultTolerance;
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
final class HystrixBeanFaultTolerance implements BeanFaultTolerance {

	private final Setter observableSettings;
	private final com.netflix.hystrix.HystrixCommand.Setter commandSettings;
	private final ContextPropagation contextPropagators;

	/**
	 * @deprecated please use {@link #HystrixBeanFaultTolerance(HystrixCommandKey, HystrixCommandGroupKey, ContextPropagation)}
	 */
	@Deprecated
	public HystrixBeanFaultTolerance(HystrixCommandKey commandKey, HystrixCommandGroupKey groupKey) {
		this(commandKey, groupKey, ContextPropagation.NONE);
	}

	public HystrixBeanFaultTolerance(HystrixCommandKey commandKey, HystrixCommandGroupKey groupKey, ContextPropagation contextPropagation) {
		observableSettings = Setter.withGroupKey(groupKey)
				.andCommandKey(commandKey)
				.andCommandPropertiesDefaults(
						HystrixCommandProperties.Setter().withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE));
		commandSettings = com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(groupKey)
				.andCommandKey(commandKey)
				.andCommandPropertiesDefaults(
						HystrixCommandProperties.Setter().withExecutionIsolationStrategy(ExecutionIsolationStrategy.THREAD));
		this.contextPropagators = Objects.requireNonNull(contextPropagation);
	}

	@Override
	public <T> Observable<T> observe(Supplier<Observable<T>> observable) {
		return HystrixObservableCommandFacade.observe(observable, observableSettings);
	}

	@Override
	public <T> T execute(final CheckedCommand<T> command) throws Throwable {
		return HystrixCommandFacade.execute(command, commandSettings, contextPropagators);
	}

}
