/*
 * Copyright 2014-2015 Avanza Bank AB
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
package se.avanzabank.asterix.ft;

/**
 * Contains settings for the Hystrix Command. The initial values of the fields are used as defaults.
 * 
 * @author Kristoffer Erlandsson (krierl)
 */
// Package private since we only want to override settings in tests currently. Configuration through Archaius otherwise.
class HystrixCommandSettings {

	private int queueSizeRejectionThreshold = 10;
	private int coreSize = 10;
	private String commandKey = null; // null means use api name as command key
	private int executionIsolationThreadTimeoutInMilliseconds = 1000;
	private int metricsRollingStatisticalWindowInMilliseconds = 10000;

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

	public String getCommandKey() {
		return commandKey;
	}

	public void setCommandKey(String commandKey) {
		this.commandKey = commandKey;
	}

	public int getExecutionIsolationThreadTimeoutInMilliseconds() {
		return executionIsolationThreadTimeoutInMilliseconds;
	}

	public void setExecutionIsolationThreadTimeoutInMilliseconds(int executionIsolationThreadTimeoutInMilliseconds) {
		this.executionIsolationThreadTimeoutInMilliseconds = executionIsolationThreadTimeoutInMilliseconds;
	}
}
