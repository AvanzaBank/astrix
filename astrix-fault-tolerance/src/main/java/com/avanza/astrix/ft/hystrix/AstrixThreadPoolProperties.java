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
import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.config.DynamicIntProperty;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

class AstrixThreadPoolProperties extends HystrixThreadPoolProperties {
	
	static final IntBeanSetting MAX_QUEUE_SIZE = new IntBeanSetting("faultTolerance.queueSize", 1_000_000);
	private final BeanConfiguration beanConfiguration;

	AstrixThreadPoolProperties(BeanConfiguration beanConfiguration, HystrixThreadPoolKey key, HystrixThreadPoolProperties.Setter builder) {
		super(key, builder);
		this.beanConfiguration = beanConfiguration;
	}
	
	@Override
	public HystrixProperty<Integer> queueSizeRejectionThreshold() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD));
	}
	
	@Override
	public HystrixProperty<Integer> coreSize() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(AstrixBeanSettings.CORE_SIZE));
	}
	
	@Override
	public HystrixProperty<Integer> keepAliveTimeMinutes() {
		return new DynamicPropertyAdapter<>(new DynamicIntProperty(1)); 
	}
	
	@Override
	public HystrixProperty<Integer> maxQueueSize() {
		return new DynamicPropertyAdapter<>(beanConfiguration.get(MAX_QUEUE_SIZE));
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingStatisticalWindowBuckets() {
		return new DynamicPropertyAdapter<>(new DynamicIntProperty(10));
	}
	
	@Override
	public HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds() {
		return new DynamicPropertyAdapter<>(new DynamicIntProperty(10_000)); 
	}
	
}