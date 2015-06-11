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
package com.avanza.astrix.ft;

import java.util.Objects;

import rx.Observable;

import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.BeanConfiguration;
import com.avanza.astrix.beans.publish.AstrixBeanDefinition;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.core.function.Supplier;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand.Setter;
import com.netflix.hystrix.HystrixThreadPoolProperties;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class BeanFaultTolerance {
	
	private final AstrixBeanDefinition<?> beanDefinition;
	private final DynamicBooleanProperty faultToleranceEnabledForBean;
	private final DynamicBooleanProperty faultToleranceEnabled;
	private final BeanFaultToleranceProvider provider;
	private final HystrixCommandNamingStrategy commandNamingStrategy;
	private final DynamicIntProperty initialTimeout;
	
	public BeanFaultTolerance(AstrixBeanDefinition<?> serviceDefinition, BeanConfiguration beanConfiguration, DynamicConfig config, BeanFaultToleranceProvider provider, HystrixCommandNamingStrategy commandNamingStrategy) {
		this.beanDefinition = serviceDefinition;
		this.provider = provider;
		this.initialTimeout = beanConfiguration.get(AstrixBeanSettings.INITIAL_TIMEOUT);
		this.faultToleranceEnabledForBean = beanConfiguration.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED);
		this.faultToleranceEnabled = AstrixSettings.ENABLE_FAULT_TOLERANCE.getFrom(config);
		this.commandNamingStrategy = Objects.requireNonNull(commandNamingStrategy);
	}
	
	public <T> Observable<T> observe(Supplier<Observable<T>> observable, HystrixObservableCommandSettings settings) {
		if (!faultToleranceEnabled()) {
			return observable.get();
		}
		Setter setter = Setter.withGroupKey(getGroupKey())
				  .andCommandKey(getCommandKey())
				  .andCommandPropertiesDefaults(com.netflix.hystrix.HystrixCommandProperties.Setter()
						  .withExecutionTimeoutInMilliseconds(getTimeoutMillis())
						  .withExecutionIsolationSemaphoreMaxConcurrentRequests(settings.getSemaphoreMaxConcurrentRequests()));
		return provider.observe(observable, setter);
	}
	
	public <T> T execute(final CheckedCommand<T> command, HystrixCommandSettings settings) throws Throwable {
		if (!faultToleranceEnabled()) {
			return command.call();
		} 
		return provider.execute(command, createHystrixConfiguration(settings));
	}
	
	private com.netflix.hystrix.HystrixCommand.Setter createHystrixConfiguration(HystrixCommandSettings settings) {
		HystrixCommandProperties.Setter commandPropertiesDefault =
				HystrixCommandProperties.Setter()
						.withExecutionIsolationSemaphoreMaxConcurrentRequests(settings.getSemaphoreMaxConcurrentRequests())
						.withExecutionIsolationStrategy(settings.getExecutionIsolationStrategy())
						.withExecutionTimeoutInMilliseconds(getTimeoutMillis());
						
		// MaxQueueSize must be set to a non negative value in order for QueueSizeRejectionThreshold to have any effect.
		// We use a high value for MaxQueueSize in order to allow QueueSizeRejectionThreshold to change dynamically using archaius.
		HystrixThreadPoolProperties.Setter threadPoolPropertiesDefaults =
				HystrixThreadPoolProperties.Setter()
						.withMaxQueueSize(settings.getMaxQueueSize())
						.withQueueSizeRejectionThreshold(settings.getQueueSizeRejectionThreshold())
						.withCoreSize(settings.getCoreSize());

		return com.netflix.hystrix.HystrixCommand.Setter.withGroupKey(getGroupKey())
				.andCommandKey(getCommandKey())
				.andCommandPropertiesDefaults(commandPropertiesDefault)
				.andThreadPoolPropertiesDefaults(threadPoolPropertiesDefaults);
	}
	
	private <T> boolean faultToleranceEnabled() {
		return faultToleranceEnabled.get() && faultToleranceEnabledForBean.get();
	}


	private int getTimeoutMillis() {
		return this.initialTimeout.get();
	}

	HystrixCommandKey getCommandKey() {
		return HystrixCommandKey.Factory.asKey(commandNamingStrategy.getCommandKeyName(beanDefinition));
	}

	HystrixCommandGroupKey getGroupKey() {
		return HystrixCommandGroupKey.Factory.asKey(commandNamingStrategy.getGroupKeyName(beanDefinition));
	}
	
}
