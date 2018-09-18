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


import com.netflix.hystrix.*;
import com.netflix.hystrix.HystrixCommandMetrics.HealthCounts;
import com.netflix.hystrix.strategy.properties.HystrixProperty;
import com.netflix.hystrix.util.HystrixRollingNumberEvent;

import java.util.Optional;

public class BeanFaultToleranceMetrics implements BeanFaultToleranceMetricsMBean {
	
	private HystrixCommandKey key;
	private HystrixThreadPoolKey poolKey;
	
	public BeanFaultToleranceMetrics(HystrixCommandKey key, HystrixThreadPoolKey poolKey) {
		this.key = key;
		this.poolKey = poolKey;
	}
	
	@Override
	public long getErrorCount() {
		return getCommandHealthCounts().map(HealthCounts::getErrorCount)
									   .orElse(0L);
	}

	private Optional<HealthCounts> getCommandHealthCounts() {
		return getCommandMetrics().map(HystrixCommandMetrics::getHealthCounts);
	}

	/**
	 * @return
	 */
	private Optional<HystrixCommandMetrics> getCommandMetrics() {
		return Optional.ofNullable(HystrixCommandMetrics.getInstance(key));
	}
	
	@Override
	public int getErrorPercentage() {
		return getCommandHealthCounts().map(HealthCounts::getErrorPercentage)
									   .orElse(0);
	}
	
	@Override
	public int getCurrentConcurrentExecutionCount() {
		return getCommandMetrics().map(HystrixCommandMetrics::getCurrentConcurrentExecutionCount)
								  .orElse(0);
	}
	

	@Override
	public long getRollingMaxConcurrentExecutions() {
		return getCommandMetrics().map(HystrixCommandMetrics::getRollingMaxConcurrentExecutions)
										.orElse(0L);
	}
	
	@Override
	public long getSuccessCount() {
		return getCommandMetrics().map(me -> me.getCumulativeCount(HystrixRollingNumberEvent.SUCCESS))
								  .orElse(0L);
	}
	
	@Override
	public long getSemaphoreRejectedCount() {
		return getCommandMetrics().map(me -> me.getCumulativeCount(HystrixRollingNumberEvent.SEMAPHORE_REJECTED))
								  .orElse(0L);
	}
	
	@Override
	public long getShortCircuitedCount() {
		return getCommandMetrics().map(me -> me.getCumulativeCount(HystrixRollingNumberEvent.SHORT_CIRCUITED))
								  .orElse(0L);
	}
	
	@Override
	public long getTimeoutCount() {
		return getCommandMetrics().map(me -> me.getCumulativeCount(HystrixRollingNumberEvent.TIMEOUT))
								  .orElse(0L);
	}
	
	@Override
	public long getThreadPoolRejectedCount() {
		return getCommandMetrics().map(me -> me.getCumulativeCount(HystrixRollingNumberEvent.THREAD_POOL_REJECTED))
								  .orElse(0L);
	}
	
	@Override
	public int getPoolCurrentActiveCount() {
		return Optional.ofNullable(HystrixThreadPoolMetrics.getInstance(poolKey))
				.map(HystrixThreadPoolMetrics::getCurrentActiveCount)
				.map(Number::intValue)
				.orElse(0);
	}
	
	@Override
	public int getPoolCurrentQueueCount() {
		return Optional.ofNullable(HystrixThreadPoolMetrics.getInstance(poolKey))
				.map(HystrixThreadPoolMetrics::getCurrentQueueSize)
				.map(Number::intValue)
				.orElse(0);
	}
	
	@Override
	public int getPoolRollingMaxActiveThreads() {
		return Optional.ofNullable(HystrixThreadPoolMetrics.getInstance(poolKey))
				.map(HystrixThreadPoolMetrics::getRollingMaxActiveThreads)
				.map(Number::intValue)
				.orElse(0);
	}

	@Override
	public int getIsCircuitBreakerOpen() {
		return Optional.ofNullable(HystrixCircuitBreaker.Factory.getInstance(key))
				.map(cb -> cb.isOpen() ? 1 : 0)
				.orElse(1);
	}

	@Override
	public int getLatencyExecute50() {
		return getCommandMetrics().map(m -> m.getExecutionTimePercentile(50))
				.orElse(0);
	}

	@Override
	public int getLatencyExecute90() {
		return getCommandMetrics().map(m -> m.getExecutionTimePercentile(90))
				.orElse(0);
	}

	@Override
	public int getLatencyExecute99() {
		return getCommandMetrics().map(m -> m.getExecutionTimePercentile(99))
				.orElse(0);
	}

	@Override
	public int getLatencyExecute100() {
		return getCommandMetrics().map(m -> m.getExecutionTimePercentile(100))
				.orElse(0);
	}

	@Override
	public int getPoolCurrentSize() {
		return Optional.ofNullable(HystrixThreadPoolMetrics.getInstance(poolKey))
				.map(HystrixThreadPoolMetrics::getCurrentPoolSize)
				.map(Number::intValue)
				.orElse(0);
	}

	@Override
	public int getPoolQueueSizeRejectionThreshold() {
		return Optional.ofNullable(HystrixThreadPoolMetrics.getInstance(poolKey))
				.map(HystrixThreadPoolMetrics::getProperties)
				.map(HystrixThreadPoolProperties::queueSizeRejectionThreshold)
				.map(HystrixProperty::<Integer>get)
				.orElse(0);
	}

}
