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
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.core.BeanProxyNames;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.core.function.CheckedCommand;

import rx.Observable;

class BeanMetricsProxy implements BeanProxy {

	private final DynamicBooleanProperty beanMetricsEnabledGlobally;
	private final DynamicBooleanProperty beanMetricsEnabled;
	private final Timer timer;
	
	public BeanMetricsProxy(AstrixBeanKey<?> beanKey, Metrics metrics, AstrixConfig astrixConfig) {
		this.beanMetricsEnabledGlobally = astrixConfig.get(AstrixSettings.ENABLE_BEAN_METRICS);
		this.beanMetricsEnabled = astrixConfig.getBeanConfiguration(beanKey).get(AstrixBeanSettings.BEAN_METRICS_ENABLED);
		this.timer = metrics.createTimer();
	}

	@Override
	public <T> CheckedCommand<T> proxyInvocation(CheckedCommand<T> command) {
		return timer.timeCheckedExecution(command);
	}

	@Override
	public <T> Supplier<Observable<T>> proxyReactiveInvocation(Supplier<Observable<T>> command) {
		return timer.timeObservable(command);
	}
	
	Timer getTimer() {
		return timer;
	}
	
	@Override
	public boolean isEnabled() {
		return beanMetricsEnabledGlobally.get() && beanMetricsEnabled.get();
	}

	@Override
	public String name() {
		return BeanProxyNames.METRICS;
	}
	
}
