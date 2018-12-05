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

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class MultiConfigs {
    //TODO: Default strategy?

    private static final Logger LOGGER = LoggerFactory.getLogger(MultiConfigs.class);
    private static MultiPropertiesStrategyDispatcher multiPropertiesStrategyDispatcher = new MultiPropertiesStrategyDispatcher();
    private static MultiConcurrencyStrategyDispatcher multiConcurrencyStrategyDispatcher = new MultiConcurrencyStrategyDispatcher();
    private static MultiEventNotifierDispatcher multiEventNotifierDispatcher = new MultiEventNotifierDispatcher();

    static {
        registerWithHystrix();
    }

    public static void register(String id, HystrixPropertiesStrategy strategy) {
        multiPropertiesStrategyDispatcher.register(id, strategy);
        verifyRegistered();
    }

    public static void register(String id, HystrixConcurrencyStrategy strategy) {
        multiConcurrencyStrategyDispatcher.register(id, strategy);
        verifyRegistered();
    }

    public static void register(String id, HystrixEventNotifier strategy) {
        multiEventNotifierDispatcher.register(id, strategy);
        verifyRegistered();
    }

    public static boolean containsAllMappings(String id) {
        return multiPropertiesStrategyDispatcher.containsMapping(id)
                && multiConcurrencyStrategyDispatcher.containsMapping(id)
                && multiEventNotifierDispatcher.containsMapping(id);
    }

    public static void verifyRegistered() {
        synchronized (multiPropertiesStrategyDispatcher) {
            if (!HystrixPlugins.getInstance().getPropertiesStrategy().getClass().equals(MultiPropertiesStrategyDispatcher.class)) {
                LOGGER.warn(MultiPropertiesStrategyDispatcher.class.getName() + " not yet registered with Hystrix, registering...");
                registerWithHystrix();
            }
        }
    }

    private static void registerWithHystrix() {

        final HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance().getEventNotifier();
        final HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
        final HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance().getMetricsPublisher();
        final HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        final HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins.getInstance().getCommandExecutionHook();

        HystrixPlugins.reset();

        reRegister(eventNotifier, multiEventNotifierDispatcher, notifier -> HystrixPlugins.getInstance().registerEventNotifier(notifier));
        reRegister(concurrencyStrategy, multiConcurrencyStrategyDispatcher, strategy -> HystrixPlugins.getInstance().registerConcurrencyStrategy(strategy));
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        reRegister(propertiesStrategy, multiPropertiesStrategyDispatcher, strategy -> HystrixPlugins.getInstance().registerPropertiesStrategy(strategy));
        HystrixPlugins.getInstance().registerCommandExecutionHook(commandExecutionHook);

        LOGGER.info(MultiPropertiesStrategyDispatcher.class.getName() + " registered with Hystrix!");
    }

    private static <T> void reRegister(final T previous, final Dispatcher<T> dispatcher, final Consumer<T> registrar) {
        if (!(previous instanceof Dispatcher)) {
            dispatcher.setUnderlying(previous);
        }
        registrar.accept(dispatcher.instance());
    }

}
