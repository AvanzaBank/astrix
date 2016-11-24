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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategyDefault;

public class MultiPropertiesStrategyDispatcher extends HystrixPropertiesStrategy {

	private final Map<MultiConfigId, HystrixPropertiesStrategy> strategies = new ConcurrentHashMap<>();
	private final HystrixPropertiesStrategy defaultStrategy = HystrixPropertiesStrategyDefault.getInstance();
	
	@Override
	public HystrixCommandProperties getCommandProperties(HystrixCommandKey qualifiedCommandKey, com.netflix.hystrix.HystrixCommandProperties.Setter builder) {
		if (MultiConfigId.hasMultiSourceId(qualifiedCommandKey)) {
			return strategies.get(MultiConfigId.readFrom(qualifiedCommandKey))
					.getCommandProperties(MultiConfigId.decode(qualifiedCommandKey), builder);
		} else {
			return defaultStrategy.getCommandProperties(qualifiedCommandKey, builder);
		}
	}
	
	@Override
	public HystrixThreadPoolProperties getThreadPoolProperties(HystrixThreadPoolKey qualifiedThreadPoolKey, com.netflix.hystrix.HystrixThreadPoolProperties.Setter builder) {
		if (MultiConfigId.hasMultiSourceId(qualifiedThreadPoolKey)) {
			return strategies.get(MultiConfigId.readFrom(qualifiedThreadPoolKey))
					.getThreadPoolProperties(MultiConfigId.decode(qualifiedThreadPoolKey), builder);
		} else {
			return defaultStrategy.getThreadPoolProperties(qualifiedThreadPoolKey, builder);
		}
	}
	
	@Override
	public HystrixCollapserProperties getCollapserProperties(HystrixCollapserKey qualifiedCollapserKey, com.netflix.hystrix.HystrixCollapserProperties.Setter builder) {
		if (MultiConfigId.hasMultiSourceId(qualifiedCollapserKey)) {
			return strategies.get(MultiConfigId.readFrom(qualifiedCollapserKey))
					.getCollapserProperties(MultiConfigId.decode(qualifiedCollapserKey), builder);
		} else {
			return defaultStrategy.getCollapserProperties(qualifiedCollapserKey, builder);
		}
	}

	public void register(String id, HystrixPropertiesStrategy strategy) {
		this.strategies.put(MultiConfigId.create(id), strategy);
	}

	public boolean containsMapping(String id) {
		return this.strategies.containsKey(MultiConfigId.create(id));
	}
	
}