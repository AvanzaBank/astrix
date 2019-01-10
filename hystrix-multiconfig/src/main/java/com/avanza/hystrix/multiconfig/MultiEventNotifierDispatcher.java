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

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class MultiEventNotifierDispatcher extends HystrixEventNotifier implements Dispatcher<HystrixEventNotifier> {

    private Map<MultiConfigId, HystrixEventNotifier> strategies = new ConcurrentHashMap<>();
    private final AtomicReference<HystrixEventNotifier> underlying = new AtomicReference<>();

    @Override
    public void markCommandExecution(HystrixCommandKey key, ExecutionIsolationStrategy isolationStrategy, int duration, List<HystrixEventType> eventsDuringExecution) {
        if (MultiConfigId.hasMultiSourceId(key)) {
            strategies.get(MultiConfigId.readFrom(key))
                      .markCommandExecution(MultiConfigId.decode(key), isolationStrategy, duration, eventsDuringExecution);
        }
        else {
            underlying()
                    .map(notifier -> {
                        notifier.markCommandExecution(key, isolationStrategy, duration, eventsDuringExecution);
                        return null;
                    })
                    .orElseGet(() -> {
                        super.markCommandExecution(key, isolationStrategy, duration, eventsDuringExecution);
                        return null;
                    });
        }
    }

    @Override
    public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
        if (MultiConfigId.hasMultiSourceId(key)) {
            strategies.get(MultiConfigId.readFrom(key))
                      .markEvent(eventType, MultiConfigId.decode(key));
        }
        else {
            underlying()
                    .map(notifier -> {
                        notifier.markEvent(eventType, key);
                        return null;
                    })
                    .orElseGet(() -> {
                        super.markEvent(eventType, key);
                        return null;
                    });
        }
    }

    public void register(String id, HystrixEventNotifier strategy) {
        this.strategies.put(MultiConfigId.create(id), strategy);
    }

    public boolean containsMapping(String id) {
        return this.strategies.containsKey(MultiConfigId.create(id));
    }

    private Optional<HystrixEventNotifier> underlying() {
        return Optional.ofNullable(underlying.get());
    }

    @Override
    public void setUnderlying(final HystrixEventNotifier underlying) {
        this.underlying.set(underlying);
    }

    @Override
    public HystrixEventNotifier instance() {
        return this;
    }
}
