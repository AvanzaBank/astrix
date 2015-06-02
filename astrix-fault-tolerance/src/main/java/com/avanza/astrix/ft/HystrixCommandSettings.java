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

import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;

/**
 * Contains settings for the Hystrix Command. The initial values of the fields are used as defaults.
 * 
 * @author Kristoffer Erlandsson (krierl)
 */
public class HystrixCommandSettings {
	
	private int queueSizeRejectionThreshold = 10;
	private int coreSize = 10;
	private int semaphoreMaxConcurrentRequests = 10;
	private int executionIsolationThreadTimeoutInMilliseconds = 1000;
	private int metricsRollingStatisticalWindowInMilliseconds = 10_000;
	private int maxQueueSize = 1_000_000;
	private ExecutionIsolationStrategy executionIsolationStrategy = ExecutionIsolationStrategy.THREAD;
	
	public HystrixCommandSettings() {
	}

	public int getMaxQueueSize() {
		return maxQueueSize;
	}
	
	public void setMaxQueueSize(int maxQueueSize) {
		this.maxQueueSize = maxQueueSize;
	}

	public int getMetricsRollingStatisticalWindowInMilliseconds() {
		return metricsRollingStatisticalWindowInMilliseconds;
	}

	public void setMetricsRollingStatisticalWindowInMilliseconds(int metricsRollingStatisticalWindowInMilliseconds) {
		this.metricsRollingStatisticalWindowInMilliseconds = metricsRollingStatisticalWindowInMilliseconds;
	}

	public int getQueueSizeRejectionThreshold() {
		return queueSizeRejectionThreshold;
	}

	public void setQueueSizeRejectionThreshold(int queueSizeRejectionThreshold) {
		this.queueSizeRejectionThreshold = queueSizeRejectionThreshold;
	}

	public int getCoreSize() {
		return coreSize;
	}

	public void setCoreSize(int coreSize) {
		this.coreSize = coreSize;
	}
	
	public int getSemaphoreMaxConcurrentRequests() {
		return semaphoreMaxConcurrentRequests;
	}

	public void setSemaphoreMaxConcurrentRequests(int semaphoreMaxConcurrentRequests) {
		this.semaphoreMaxConcurrentRequests = semaphoreMaxConcurrentRequests;
	}

	public int getExecutionIsolationThreadTimeoutInMilliseconds() {
		return executionIsolationThreadTimeoutInMilliseconds;
	}

	public void setExecutionIsolationThreadTimeoutInMilliseconds(int executionIsolationThreadTimeoutInMilliseconds) {
		this.executionIsolationThreadTimeoutInMilliseconds = executionIsolationThreadTimeoutInMilliseconds;
	}

	public void setExecutionIsolationStrategy(ExecutionIsolationStrategy executionIsolationStrategy) {
		this.executionIsolationStrategy = executionIsolationStrategy;
	}
	
	public ExecutionIsolationStrategy getExecutionIsolationStrategy() {
		return executionIsolationStrategy;
	}
}
