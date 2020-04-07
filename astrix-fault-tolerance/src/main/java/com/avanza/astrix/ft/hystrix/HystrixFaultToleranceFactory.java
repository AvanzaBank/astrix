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

import java.util.concurrent.atomic.AtomicInteger;

import com.avanza.astrix.beans.async.ContextPropagation;
import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.ft.BeanFaultTolerance;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactorySpi;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.beans.ft.MonitorableFaultToleranceSpi;
import com.avanza.astrix.beans.tracing.AstrixTraceProvider;
import com.avanza.astrix.beans.tracing.DefaultTraceProvider;
import com.avanza.astrix.modules.AstrixInject;
import com.avanza.hystrix.multiconfig.MultiConfigId;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

/**
 * @author Elias Lindholm (elilin)
 */
final class HystrixFaultToleranceFactory implements BeanFaultToleranceFactorySpi, MonitorableFaultToleranceSpi {

    /*
     * Astrix allows multiple AstrixContext within the same JVM, although
     * more than one AstrixContext within a single JVM is most often only used during unit testing. However,
     * since Astrix allows multiple AstrixContext's we want those to be isolated from each other. Hystrix only
     * allows registering a global strategy for each exposed SPI. Therefore Astrix registers a global "dispatcher"
     * strategy (see HystrixPluginDispatcher) that dispatches each invocation to a HystrixPlugin to
     * the HystrixPlugin instance associated with a given AstrixContext.
     *
     * The "id" property is used by the dispatcher to identify the HystrixPlugin instances associated with a given
     * AstrixContext.
     */

    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final BeanMapping beanMapping;
    private final HystrixCommandKeyFactory hystrixCommandKeyFactory;
    private final String id;
    private final ContextPropagation contextPropagation;
    private MultiConfigId multiConfigId = MultiConfigId.create("astrix");

    /**
     * @deprecated please use {@link #HystrixFaultToleranceFactory(HystrixCommandNamingStrategy, AstrixConcurrencyStrategy, BeanConfigurationPropertiesStrategy, BeanMapping, AstrixTraceProvider, AstrixConfig)}
     */
    @Deprecated
    public HystrixFaultToleranceFactory(HystrixCommandNamingStrategy hystrixCommandNamingStrategy,
                                        AstrixConcurrencyStrategy concurrencyStrategy,
                                        BeanConfigurationPropertiesStrategy propertiesStrategy,
                                        BeanMapping beanMapping,
                                        AstrixConfig config) {
        this(
                hystrixCommandNamingStrategy,
                concurrencyStrategy,
                propertiesStrategy,
                beanMapping,
                new DefaultTraceProvider(),
                config
        );
    }

    @AstrixInject
    public HystrixFaultToleranceFactory(
            HystrixCommandNamingStrategy hystrixCommandNamingStrategy,
            AstrixConcurrencyStrategy concurrencyStrategy,
            BeanConfigurationPropertiesStrategy propertiesStrategy,
            BeanMapping beanMapping,
            AstrixTraceProvider astrixTraceProvider,
            AstrixConfig config
    ) {
        this.id = Integer.toString(idGenerator.incrementAndGet());
        this.beanMapping = beanMapping;
        HystrixStrategies hystrixStrategies = new HystrixStrategies(propertiesStrategy,
                concurrencyStrategy,
                new FailedServiceInvocationLogger(beanMapping, config),
                id);
        HystrixStrategyDispatcher.registerStrategies(hystrixStrategies);
        this.hystrixCommandKeyFactory = new HystrixCommandKeyFactory(id, hystrixCommandNamingStrategy);
        this.contextPropagation = ContextPropagation.create(astrixTraceProvider.getContextPropagators());
    }

    @Override
    public BeanFaultTolerance create(AstrixBeanKey<?> beanKey) {
        return new HystrixBeanFaultTolerance(getCommandKey(beanKey), getGroupKey(beanKey), contextPropagation);
    }

    @Override
    public BeanFaultToleranceMetricsMBean createBeanFaultToleranceMetricsMBean(AstrixBeanKey<?> beanKey) {
        return new BeanFaultToleranceMetrics(getCommandKey(beanKey), getThreadPoolKey(beanKey));
    }

    HystrixCommandGroupKey getGroupKey(AstrixBeanKey<?> beanKey) {
        HystrixCommandGroupKey result = hystrixCommandKeyFactory.createGroupKey(beanKey);
        this.beanMapping.registerBeanKey(result.name(), beanKey);
        return multiConfigId.encode(result);
    }

    HystrixCommandKey getCommandKey(AstrixBeanKey<?> beanKey) {
        HystrixCommandKey result = hystrixCommandKeyFactory.createCommandKey(beanKey);
        this.beanMapping.registerBeanKey(result.name(), beanKey);
        return multiConfigId.encode(result);
    }

    HystrixThreadPoolKey getThreadPoolKey(AstrixBeanKey<?> beanKey) {
        HystrixThreadPoolKey result = hystrixCommandKeyFactory.createThreadPoolKey(beanKey);
        this.beanMapping.registerBeanKey(result.name(), beanKey);
        return multiConfigId.encode(result);
    }

}
