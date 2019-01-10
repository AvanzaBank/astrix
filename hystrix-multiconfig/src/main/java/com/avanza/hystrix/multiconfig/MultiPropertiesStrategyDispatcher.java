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

import com.netflix.hystrix.*;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class MultiPropertiesStrategyDispatcher extends HystrixPropertiesStrategy implements Dispatcher<HystrixPropertiesStrategy> {

    private final Map<MultiConfigId, HystrixPropertiesStrategy> strategies = new ConcurrentHashMap<>();
    private final AtomicReference<HystrixPropertiesStrategy> underlying = new AtomicReference<>();

    @Override
    public HystrixCommandProperties getCommandProperties(HystrixCommandKey qualifiedCommandKey, com.netflix.hystrix.HystrixCommandProperties.Setter builder) {
        if (MultiConfigId.hasMultiSourceId(qualifiedCommandKey)) {
            return strategies.get(MultiConfigId.readFrom(qualifiedCommandKey))
                             .getCommandProperties(MultiConfigId.decode(qualifiedCommandKey), builder);
        }
        else {
            return underlying().map(strategy -> strategy.getCommandProperties(qualifiedCommandKey, builder))
                               .orElseGet(() -> super.getCommandProperties(qualifiedCommandKey, builder));
        }
    }

    @Override
    public HystrixThreadPoolProperties getThreadPoolProperties(HystrixThreadPoolKey qualifiedThreadPoolKey, com.netflix.hystrix.HystrixThreadPoolProperties.Setter builder) {
        if (MultiConfigId.hasMultiSourceId(qualifiedThreadPoolKey)) {
            return strategies.get(MultiConfigId.readFrom(qualifiedThreadPoolKey))
                             .getThreadPoolProperties(MultiConfigId.decode(qualifiedThreadPoolKey), builder);
        }
        else {
            return underlying().map(strategy -> strategy.getThreadPoolProperties(qualifiedThreadPoolKey, builder))
                               .orElseGet(() -> super.getThreadPoolProperties(qualifiedThreadPoolKey, builder));
        }
    }

    @Override
    public HystrixCollapserProperties getCollapserProperties(HystrixCollapserKey qualifiedCollapserKey, com.netflix.hystrix.HystrixCollapserProperties.Setter builder) {
        if (MultiConfigId.hasMultiSourceId(qualifiedCollapserKey)) {
            return strategies.get(MultiConfigId.readFrom(qualifiedCollapserKey))
                             .getCollapserProperties(MultiConfigId.decode(qualifiedCollapserKey), builder);
        }
        else {
            return underlying().map(strategy -> strategy.getCollapserProperties(qualifiedCollapserKey, builder))
                               .orElseGet(() -> super.getCollapserProperties(qualifiedCollapserKey, builder));
        }
    }

    @Override
    public String getCommandPropertiesCacheKey(final HystrixCommandKey commandKey, final HystrixCommandProperties.Setter builder) {
        return underlying().map(strategy -> strategy.getCommandPropertiesCacheKey(commandKey, builder))
                           .orElseGet(() -> super.getCommandPropertiesCacheKey(commandKey, builder));
    }

    @Override
    public String getThreadPoolPropertiesCacheKey(final HystrixThreadPoolKey threadPoolKey, final HystrixThreadPoolProperties.Setter builder) {
        return underlying().map(strategy -> strategy.getThreadPoolPropertiesCacheKey(threadPoolKey, builder))
                           .orElseGet(() -> super.getThreadPoolPropertiesCacheKey(threadPoolKey, builder));
    }

    @Override
    public String getCollapserPropertiesCacheKey(final HystrixCollapserKey collapserKey, final HystrixCollapserProperties.Setter builder) {
        return underlying().map(strategy -> strategy.getCollapserPropertiesCacheKey(collapserKey, builder))
                           .orElseGet(() -> super.getCollapserPropertiesCacheKey(collapserKey, builder));
    }

    @Override
    public HystrixTimerThreadPoolProperties getTimerThreadPoolProperties() {
        return underlying().map(HystrixPropertiesStrategy::getTimerThreadPoolProperties)
                           .orElseGet(super::getTimerThreadPoolProperties);
    }

    public void register(String id, HystrixPropertiesStrategy strategy) {
        this.strategies.put(MultiConfigId.create(id), strategy);
    }

    public boolean containsMapping(String id) {
        return this.strategies.containsKey(MultiConfigId.create(id));
    }

    private Optional<HystrixPropertiesStrategy> underlying() {
        return Optional.ofNullable(underlying.get());
    }

    @Override
    public void setUnderlying(final HystrixPropertiesStrategy underlying) {
        this.underlying.set(underlying);
    }

    @Override
    public HystrixPropertiesStrategy instance() {
        return this;
    }
}
