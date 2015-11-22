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

import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.StringBeanSetting;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

final class AstrixCommandProperties extends HystrixCommandProperties {
	private final BeanConfiguration beanConfiguration;
	private final ExecutionIsolationStrategy isolationStrategy;
	
	AstrixCommandProperties(BeanConfiguration beanConfiguration, HystrixCommandKey key, com.netflix.hystrix.HystrixCommandProperties.Setter builder) {
		super(key, builder);
		this.beanConfiguration = beanConfiguration;
		this.isolationStrategy = builder.getExecutionIsolationStrategy();
	}
	
	@Override
	public HystrixProperty<Integer> executionTimeoutInMilliseconds() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(AstrixBeanSettings.TIMEOUT));
	}
	
	@Override
	public HystrixProperty<Boolean> circuitBreakerEnabled() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.circuitBreakerEnabled", true)));
	}
	
	@Override
	public HystrixProperty<Integer> circuitBreakerErrorThresholdPercentage() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.circuitBreakerErrorThresholdPercentage", 50)));
	}
	
	@Override
	public HystrixProperty<Boolean> circuitBreakerForceClosed() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.circuitBreakerForceClosed", false)));
	}
	
	@Override
	public HystrixProperty<Boolean> circuitBreakerForceOpen() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.circuitBreakerForceOpen", false)));
	}

	@Override
	public HystrixProperty<Integer> circuitBreakerRequestVolumeThreshold() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.circuitBreakerRequestVolumeThreshold", 20)));
	}
	
	@Override
	public HystrixProperty<Integer> circuitBreakerSleepWindowInMilliseconds() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.circuitBreakerSleepWindowInMilliseconds", 5000)));
	}
	
	@Override
	public HystrixProperty<Integer> executionIsolationSemaphoreMaxConcurrentRequests() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS));
	}
	
	@Override
	public HystrixProperty<ExecutionIsolationStrategy> executionIsolationStrategy() {
		// Don't allow change isolation strategy at runtime
		if (this.isolationStrategy != null) {
			return HystrixProperty.Factory.asProperty(this.isolationStrategy); 
		}
		return HystrixProperty.Factory.asProperty(ExecutionIsolationStrategy.THREAD);
	}
	
	@Override
	public HystrixProperty<Boolean> executionIsolationThreadInterruptOnTimeout() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.executionIsolationThreadInterruptOnTimeout", true)));
	}
	
	@Override
	public HystrixProperty<String> executionIsolationThreadPoolKeyOverride() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new StringBeanSetting("faultTolerance.executionIsolationThreadPoolKeyOverride", null)));
	}
	
	@Override
	@Deprecated
	public HystrixProperty<Integer> executionIsolationThreadTimeoutInMilliseconds() {
		return executionTimeoutInMilliseconds();
	}
	
	@Override
	public HystrixProperty<Boolean> executionTimeoutEnabled() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.executionTimeoutEnabled", true)));
	}
	
	@Override
	public HystrixProperty<Boolean> fallbackEnabled() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.fallbackEnabled", true)));
	}
	@Override
	public HystrixProperty<Integer> fallbackIsolationSemaphoreMaxConcurrentRequests() {
		/*
		 * Astrix does not use the fallback of HystrixCommand to do any unsafe operations, and
		 * we always want to call the getFallback method to get proper handling of a failed
		 * service invocation in Astrix, see HystrixCommandFacade/HystrixObservableCommandFacade.
		 *
		 */
		return HystrixProperty.Factory.asProperty(Integer.MAX_VALUE);
	}
	
	@Override
	public HystrixProperty<Integer> metricsHealthSnapshotIntervalInMilliseconds() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsHealthSnapshotIntervalInMilliseconds", 500)));
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileBucketSize() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingPercentileBucketSize", 100)));
	}
	
	@Override
	public HystrixProperty<Boolean> metricsRollingPercentileEnabled() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.metricsRollingPercentileEnabled", true)));
	}
	
	@Deprecated
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileWindow() {
		return metricsRollingPercentileWindowInMilliseconds();
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileWindowBuckets() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingPercentileWindowBuckets", 6)));
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileWindowInMilliseconds() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingPercentileWindowInMilliseconds", 60_000)));
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingStatisticalWindowBuckets", 10)));
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingStatisticalWindowInMilliseconds", 10_000)));
	}
	
	@Override
	public HystrixProperty<Boolean> requestCacheEnabled() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.requestCacheEnabled", false)));
	}
	
	@Override
	public HystrixProperty<Boolean> requestLogEnabled() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.requestLogEnabled", false)));
	}
	
	
}