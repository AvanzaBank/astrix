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
package com.avanza.hystrix.multiconfig;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifierDefault;

public class MultiEventNotifierDispatcher extends HystrixEventNotifier {

	private Map<MultiConfigId, HystrixEventNotifier> strategies = new ConcurrentHashMap<>();
	private HystrixEventNotifier defaultStrategy = HystrixEventNotifierDefault.getInstance();
	
	@Override
	public void markCommandExecution(HystrixCommandKey key, ExecutionIsolationStrategy isolationStrategy, int duration,
			List<HystrixEventType> eventsDuringExecution) {
		if (MultiConfigId.hasMultiSourceId(key)) {
			strategies.get(MultiConfigId.readFrom(key))
					.markCommandExecution(MultiConfigId.decode(key), isolationStrategy, duration, eventsDuringExecution);
		} else {
			defaultStrategy.markCommandExecution(key, isolationStrategy, duration, eventsDuringExecution);
		}
	}
	
	@Override
	public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
		if (MultiConfigId.hasMultiSourceId(key)) {
			strategies.get(MultiConfigId.readFrom(key))
				.markEvent(eventType, MultiConfigId.decode(key));
		} else {
			defaultStrategy.markEvent(eventType, key);
		}
	}

	public void register(String id, HystrixEventNotifier strategy) {
		this.strategies.put(MultiConfigId.create(id), strategy);
	}
	
	public boolean containsMapping(String id) {
		return this.strategies.containsKey(MultiConfigId.create(id));
	}
	
}
