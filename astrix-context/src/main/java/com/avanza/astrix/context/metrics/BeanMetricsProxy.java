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
package com.avanza.astrix.context.metrics;

import java.util.function.Supplier;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.BeanConfigurations;
import com.avanza.astrix.beans.factory.BeanProxy;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.core.function.CheckedCommand;

import rx.Observable;

class BeanMetricsProxy implements BeanProxy {

	private final PublishedAstrixBean<?> beanDefinition;
	private final MetricsSpi metrics;
	private final DynamicBooleanProperty beanMetricsEnabledGlobally;
	private final DynamicBooleanProperty beanMetricsEnabled;
	
	public BeanMetricsProxy(PublishedAstrixBean<?> beanDefinition, MetricsSpi metrics, AstrixConfig astrixConfig, BeanConfigurations beanConfigurations) {
		this.beanDefinition = beanDefinition;
		this.metrics = metrics;
		this.beanMetricsEnabledGlobally = astrixConfig.get(AstrixSettings.ENABLE_BEAN_METRICS);
		this.beanMetricsEnabled = beanConfigurations.getBeanConfiguration(beanDefinition.getBeanKey()).get(AstrixBeanSettings.BEAN_METRICS_ENABLED);
	}

	@Override
	public <T> CheckedCommand<T> proxyInvocation(CheckedCommand<T> command) {
		if (!beanMetricsEnabled()) {
			return command;
		}
		return metrics.timeExecution(command, "ServiceBeanMetrics", getServiceBeanName());
	}

	@Override
	public <T> Supplier<Observable<T>> proxyAsyncInvocation(Supplier<Observable<T>> command) {
		if (!beanMetricsEnabled()) {
			return command;
		}
		return metrics.timeObservable(command, "ServiceBeanMetrics", getServiceBeanName());
	}

	private boolean beanMetricsEnabled() {
		return beanMetricsEnabledGlobally.get() && beanMetricsEnabled.get();
	}
	
	private String getServiceBeanName() {
		return beanDefinition.getBeanKey().toString();
	}

}
