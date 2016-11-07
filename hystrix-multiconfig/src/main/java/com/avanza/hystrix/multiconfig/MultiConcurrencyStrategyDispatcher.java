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
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

public class MultiConcurrencyStrategyDispatcher extends HystrixConcurrencyStrategy {
	private Map<MultiConfigId, HystrixConcurrencyStrategy> strategies = new ConcurrentHashMap<>();
	
	@Override
	public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixProperty<Integer> corePoolSize,
			HystrixProperty<Integer> maximumPoolSize, HystrixProperty<Integer> keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		return strategies.get(MultiConfigId.readFrom(threadPoolKey))
				.getThreadPool(MultiConfigId.decode(threadPoolKey), corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
	}

	@Override
	public <T> HystrixRequestVariable<T> getRequestVariable(HystrixRequestVariableLifecycle<T> rv) {
		return HystrixConcurrencyStrategyDefault.getInstance().getRequestVariable(rv);
	}
	
	@Override
	public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
		return HystrixConcurrencyStrategyDefault.getInstance().getBlockingQueue(maxQueueSize);
	}

	public void register(String id, HystrixConcurrencyStrategy strategy) {
		this.strategies.put(MultiConfigId.create(id), strategy);
	}

	public boolean containsMapping(String id) {
		return this.strategies.containsKey(MultiConfigId.create(id));
	}
	
}
