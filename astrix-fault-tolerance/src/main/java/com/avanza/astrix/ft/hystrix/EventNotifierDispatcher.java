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

import java.util.List;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

final class EventNotifierDispatcher extends HystrixEventNotifier {
	
	private HystrixStrategyMapping strategymapping;

	public EventNotifierDispatcher(HystrixStrategyMapping strategymapping) {
		this.strategymapping = strategymapping;
	}

	@Override
	public void markCommandExecution(HystrixCommandKey key, 
									 ExecutionIsolationStrategy isolationStrategy, 
									 int duration,
									 List<HystrixEventType> eventsDuringExecution) {
		strategymapping.getHystrixStrategies(key)
					   .getHystrixEventNotifier()
					   .markCommandExecution(key, isolationStrategy, duration, eventsDuringExecution);
	}
	
	@Override
	public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
		strategymapping.getHystrixStrategies(key)
		   			   .getHystrixEventNotifier()
		   			   .markEvent(eventType, key);
	}

}
