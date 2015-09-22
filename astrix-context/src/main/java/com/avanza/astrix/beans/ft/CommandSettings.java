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
package com.avanza.astrix.beans.ft;

/**
 * Contains settings for the Hystrix Command. The initial values of the fields are used as defaults.
 * 
 * @author Kristoffer Erlandsson (krierl)
 */
public class CommandSettings {
	
	private int initialQueueSizeRejectionThreshold = 10;
	private int initialCoreSize = 10;
	private int initialSemaphoreMaxConcurrentRequests = 20;
	private int initialTimeoutInMilliseconds = 1000;
	private int metricsRollingStatisticalWindowInMilliseconds = 10_000;
	private int maxQueueSize = 1_000_000;
	private String commandName;
	private String groupName;
	
	public CommandSettings() {
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

	public int getInitialQueueSizeRejectionThreshold() {
		return initialQueueSizeRejectionThreshold;
	}

	public void setInitialQueueSizeRejectionThreshold(int queueSizeRejectionThreshold) {
		this.initialQueueSizeRejectionThreshold = queueSizeRejectionThreshold;
	}

	public int getInitialCoreSize() {
		return initialCoreSize;
	}

	public void setInitialCoreSize(int coreSize) {
		this.initialCoreSize = coreSize;
	}
	
	public int getInitialSemaphoreMaxConcurrentRequests() {
		return initialSemaphoreMaxConcurrentRequests;
	}

	public void setInitialSemaphoreMaxConcurrentRequests(int initialSemaphoreMaxConcurrentRequests) {
		this.initialSemaphoreMaxConcurrentRequests = initialSemaphoreMaxConcurrentRequests;
	}

	public int getInitialTimeoutInMilliseconds() {
		return initialTimeoutInMilliseconds;
	}

	public void setInitialTimeoutInMilliseconds(int initialTimeoutInMilliseconds) {
		this.initialTimeoutInMilliseconds = initialTimeoutInMilliseconds;
	}

	public String getCommandName() {
		return commandName;
	}
	
	public void setCommandName(String commandName) {
		this.commandName = commandName;
	}
	
	public String getGroupName() {
		return groupName;
	}
	
	public void setGroupName(String groupName) {
		this.groupName = groupName;
	}
}
