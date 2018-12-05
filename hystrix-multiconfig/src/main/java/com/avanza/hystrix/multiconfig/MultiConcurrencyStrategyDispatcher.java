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

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariable;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.properties.HystrixProperty;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class MultiConcurrencyStrategyDispatcher extends HystrixConcurrencyStrategy implements Dispatcher<HystrixConcurrencyStrategy> {

    private final Map<MultiConfigId, HystrixConcurrencyStrategy> strategies = new ConcurrentHashMap<>();
    private final AtomicReference<HystrixConcurrencyStrategy> underlying = new AtomicReference<>();

    @Override
    public ThreadPoolExecutor getThreadPool(HystrixThreadPoolKey threadPoolKey,
                                            HystrixProperty<Integer> corePoolSize,
                                            HystrixProperty<Integer> maximumPoolSize,
                                            HystrixProperty<Integer> keepAliveTime, TimeUnit unit,
                                            BlockingQueue<Runnable> workQueue) {
        if (MultiConfigId.hasMultiSourceId(threadPoolKey)) {
            return strategies.get(MultiConfigId.readFrom(threadPoolKey))
                             .getThreadPool(MultiConfigId.decode(threadPoolKey), corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue);
        }
        else {
            return underlying().map(strategy -> strategy.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue))
                               .orElseGet(() -> super.getThreadPool(threadPoolKey, corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue));
        }
    }

    @Override
    public <T> HystrixRequestVariable<T> getRequestVariable(HystrixRequestVariableLifecycle<T> rv) {
        return underlying().map(strategy -> strategy.getRequestVariable(rv))
                           .orElseGet(() -> super.getRequestVariable(rv));
    }

    @Override
    public BlockingQueue<Runnable> getBlockingQueue(int maxQueueSize) {
        return underlying().map(strategy -> strategy.getBlockingQueue(maxQueueSize))
                           .orElseGet(() -> super.getBlockingQueue(maxQueueSize));
    }

    @Override
    public <T> Callable<T> wrapCallable(final Callable<T> callable) {
        return underlying().map(strategy -> strategy.wrapCallable(callable))
                           .orElseGet(() -> super.wrapCallable(callable));
    }

    public void register(String id, HystrixConcurrencyStrategy strategy) {
        this.strategies.put(MultiConfigId.create(id), strategy);
    }

    public boolean containsMapping(String id) {
        return this.strategies.containsKey(MultiConfigId.create(id));
    }

    private Optional<HystrixConcurrencyStrategy> underlying() {
        return Optional.ofNullable(underlying.get());
    }

    @Override
    public void setUnderlying(final HystrixConcurrencyStrategy underlying) {
        this.underlying.set(underlying);
    }

    @Override
    public HystrixConcurrencyStrategy instance() {
        return this;
    }
}
