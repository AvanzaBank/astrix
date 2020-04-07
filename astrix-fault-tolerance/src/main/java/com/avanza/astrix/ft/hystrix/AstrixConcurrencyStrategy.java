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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.PreDestroy;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

final class AstrixConcurrencyStrategy extends HystrixConcurrencyStrategy {
	
	private ConcurrentMap<HystrixThreadPoolKey, ThreadPoolExecutor> threadPoolByKey = new ConcurrentHashMap<>();
	
	@Override
	public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey, HystrixProperty<Integer> corePoolSize,
			HystrixProperty<Integer> maximumPoolSize, HystrixProperty<Integer> keepAliveTime, TimeUnit unit,
			BlockingQueue<Runnable> workQueue) {
		return threadPoolByKey.computeIfAbsent(threadPoolKey, (key) -> super.getThreadPool(key, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue));
	}
	
	@PreDestroy
	public void destroy() {
		/*
		 * Destroy all threads associated with current AstrixContext
		 */
		this.threadPoolByKey.values().stream().forEach(ThreadPoolExecutor::shutdownNow);
	}

}
