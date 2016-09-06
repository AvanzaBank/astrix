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
	private final ExecutionIsolationStrategy isolationStrategy;
	private final DynamicPropertyAdapter<Integer> executionTimeoutInMilliseconds;
	private final HystrixProperty<Boolean> circuitBreakerEnabled;
	private final HystrixProperty<Integer> circuitBreakerErrorThresholdPercentage;
	private final HystrixProperty<Boolean> circuitBreakerForceClosed;
	private final HystrixProperty<Boolean> circuitBreakerForceOpen;
	private final HystrixProperty<Integer> circuitBreakerRequestVolumeThreshold;
	private final HystrixProperty<Integer> circuitBreakerSleepWindowInMilliseconds;
	private final HystrixProperty<Integer> executionIsolationSemaphoreMaxConcurrentRequests;
	private final HystrixProperty<Boolean> executionIsolationThreadInterruptOnTimeout;
	private final HystrixProperty<String> executionIsolationThreadPoolKeyOverride;
	private final HystrixProperty<Boolean> executionTimeoutEnabled;
	private final HystrixProperty<Boolean> fallbackEnabled;
	private final HystrixProperty<Integer> metricsHealthSnapshotIntervalInMilliseconds;
	private final HystrixProperty<Integer> metricsRollingPercentileBucketSize;
	private final HystrixProperty<Boolean> metricsRollingPercentileEnabled;
	private final HystrixProperty<Integer> metricsRollingPercentileWindowBuckets;
	private final HystrixProperty<Integer> metricsRollingPercentileWindowInMilliseconds;
	private final HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets;
	private final HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds;
	private final HystrixProperty<Boolean> requestCacheEnabled;
	private final HystrixProperty<Boolean> requestLogEnabled;
	
	
	AstrixCommandProperties(BeanConfiguration beanConfiguration, HystrixCommandKey key, com.netflix.hystrix.HystrixCommandProperties.Setter builder) {
		super(key, builder);
		this.isolationStrategy = builder.getExecutionIsolationStrategy();
		
		// We create all these property adaptors here as each and every one results in creation of several temporary String objects.
		// The alternative to this, to create the adaptors at call-time in the various methods of this class, results in large amounts
		// of temporary objects and thus heavy GC load in systems with many astrix calls.
		this.executionTimeoutInMilliseconds = new DynamicPropertyAdapter<>(beanConfiguration.get(AstrixBeanSettings.TIMEOUT));
		this.circuitBreakerEnabled = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.circuitBreakerEnabled", true)));
		this.circuitBreakerErrorThresholdPercentage = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.circuitBreakerErrorThresholdPercentage", 50)));
		this.circuitBreakerForceClosed = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.circuitBreakerForceClosed", false)));
		this.circuitBreakerForceOpen = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.circuitBreakerForceOpen", false)));
		this.circuitBreakerRequestVolumeThreshold = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.circuitBreakerRequestVolumeThreshold", 20)));
		this.circuitBreakerSleepWindowInMilliseconds = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.circuitBreakerSleepWindowInMilliseconds", 5000)));
		this.executionIsolationSemaphoreMaxConcurrentRequests = new DynamicPropertyAdapter<>(beanConfiguration.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS));
		this.executionIsolationThreadInterruptOnTimeout = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.executionIsolationThreadInterruptOnTimeout", true)));
		this.executionIsolationThreadPoolKeyOverride =  new DynamicPropertyAdapter<>(beanConfiguration.get(new StringBeanSetting("faultTolerance.executionIsolationThreadPoolKeyOverride", null)));
		this.executionTimeoutEnabled = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.executionTimeoutEnabled", true)));
		this.fallbackEnabled = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.fallbackEnabled", true)));
		this.metricsHealthSnapshotIntervalInMilliseconds = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsHealthSnapshotIntervalInMilliseconds", 500)));
		this.metricsRollingPercentileBucketSize = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingPercentileBucketSize", 100)));
		this.metricsRollingPercentileEnabled = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.metricsRollingPercentileEnabled", true)));
		this.metricsRollingPercentileWindowBuckets = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingPercentileWindowBuckets", 6)));
		this.metricsRollingPercentileWindowInMilliseconds = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingPercentileWindowInMilliseconds", 60_000)));
		this.metricsRollingStatisticalWindowBuckets = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingStatisticalWindowBuckets", 10)));
		this.metricsRollingStatisticalWindowInMilliseconds = new DynamicPropertyAdapter<>(beanConfiguration.get(new IntBeanSetting("faultTolerance.metricsRollingStatisticalWindowInMilliseconds", 10_000)));
		this.requestCacheEnabled = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.requestCacheEnabled", false)));
		this.requestLogEnabled = new DynamicPropertyAdapter<>(beanConfiguration.get(new BooleanBeanSetting("faultTolerance.requestLogEnabled", false)));
	}
	
	@Override
	public HystrixProperty<Integer> executionTimeoutInMilliseconds() {
		return executionTimeoutInMilliseconds;
	}
	
	@Override
	public HystrixProperty<Boolean> circuitBreakerEnabled() {
		return circuitBreakerEnabled;
	}
	
	@Override
	public HystrixProperty<Integer> circuitBreakerErrorThresholdPercentage() {
		return circuitBreakerErrorThresholdPercentage;
	}
	
	@Override
	public HystrixProperty<Boolean> circuitBreakerForceClosed() {
		return circuitBreakerForceClosed;
	}
	
	@Override
	public HystrixProperty<Boolean> circuitBreakerForceOpen() {
		return circuitBreakerForceOpen;
	}

	@Override
	public HystrixProperty<Integer> circuitBreakerRequestVolumeThreshold() {
		return circuitBreakerRequestVolumeThreshold;
	}
	
	@Override
	public HystrixProperty<Integer> circuitBreakerSleepWindowInMilliseconds() {
		return circuitBreakerSleepWindowInMilliseconds;
	}
	
	@Override
	public HystrixProperty<Integer> executionIsolationSemaphoreMaxConcurrentRequests() {
		return executionIsolationSemaphoreMaxConcurrentRequests;
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
		return executionIsolationThreadInterruptOnTimeout;
	}
	
	@Override
	public HystrixProperty<String> executionIsolationThreadPoolKeyOverride() {
		return executionIsolationThreadPoolKeyOverride;
	}
	
	@Override
	@Deprecated
	public HystrixProperty<Integer> executionIsolationThreadTimeoutInMilliseconds() {
		return executionTimeoutInMilliseconds();
	}
	
	@Override
	public HystrixProperty<Boolean> executionTimeoutEnabled() {
		return executionTimeoutEnabled;
	}
	
	@Override
	public HystrixProperty<Boolean> fallbackEnabled() {
		return fallbackEnabled;
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
		return metricsHealthSnapshotIntervalInMilliseconds;
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileBucketSize() {
		return metricsRollingPercentileBucketSize;
	}
	
	@Override
	public HystrixProperty<Boolean> metricsRollingPercentileEnabled() {
		return metricsRollingPercentileEnabled;
	}
	
	@Deprecated
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileWindow() {
		return metricsRollingPercentileWindowInMilliseconds();
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileWindowBuckets() {
		return metricsRollingPercentileWindowBuckets;
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingPercentileWindowInMilliseconds() {
		return metricsRollingPercentileWindowInMilliseconds;
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets() {
		return metricsRollingStatisticalWindowBuckets;
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds() {
		return metricsRollingStatisticalWindowInMilliseconds;
	}
	
	@Override
	public HystrixProperty<Boolean> requestCacheEnabled() {
		return requestCacheEnabled;
	}
	
	@Override
	public HystrixProperty<Boolean> requestLogEnabled() {
		return requestLogEnabled;
	}
}
