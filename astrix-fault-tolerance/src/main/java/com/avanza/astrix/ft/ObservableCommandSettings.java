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
package com.avanza.astrix.ft;

import java.util.Objects;


public class ObservableCommandSettings {
	
	private final String commandKey;
	private final String groupKey;
	private int timeoutMillis = 1_000;
	private int maxConcurrentRequests = 20;
	
	public ObservableCommandSettings(String commandKey, String groupKey) {
		this.commandKey = Objects.requireNonNull(commandKey);
		this.groupKey = Objects.requireNonNull(groupKey);
	}
	
	public final String getGroupKey() {
		return groupKey;
	}
	
	public final String getCommandKey() {
		return commandKey;
	}
	
	public final int getMaxConcurrentRequests() {
		return maxConcurrentRequests;
	}
	
	public void setMaxConcurrentRequests(int maxConcurrentRequests) {
		this.maxConcurrentRequests = maxConcurrentRequests;
	}
	
	public final void setTimeoutMillis(int timeoutMillis) {
		if (timeoutMillis < 1) {
			throw new IllegalArgumentException("Timeout must be positive: " + timeoutMillis);
		}
		this.timeoutMillis = timeoutMillis;
	}
	
	public final int getTimeoutMillis() {
		return timeoutMillis;
	}

}
