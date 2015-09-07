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
package com.avanza.astrix.metrics;

import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.context.core.AstrixMBeanExporter;
import com.avanza.astrix.context.metrics.MetricsSpi;
import com.avanza.astrix.core.function.CheckedCommand;
import com.codahale.metrics.JmxReporter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;

import rx.Observable;
import rx.functions.Action0;

public class DropwizardMetrics implements MetricsSpi {

	private static final Logger log = LoggerFactory.getLogger(DropwizardMetrics.class);
	private final MetricRegistry metrics = new MetricRegistry();
	private final AstrixMBeanExporter mbeanExporter;
	private volatile JmxReporter reporter;
	
	public DropwizardMetrics(AstrixMBeanExporter mbeanExporter) {
		this.mbeanExporter = mbeanExporter;
	}
	
	@PostConstruct
	public void init() {
		if (!mbeanExporter.exportMBeans()) {
			log.info("Exporting of Astrix MBeans is disabled, won't export MBean for metrics");
			return;
		}
		reporter = JmxReporter.forRegistry(metrics)
				.inDomain("com.avanza.astrix.context")
				.convertRatesTo(TimeUnit.SECONDS)
				.convertDurationsTo(TimeUnit.MILLISECONDS)
				.createsObjectNamesWith((type, domain, name) -> {
					int split = name.indexOf("#");
					String metricGroup = name.substring(0, split);
					String metricName = name.substring(split + 1, name.length());
					return mbeanExporter.getObjectName(metricGroup, metricName);
				})
				.build();
		reporter.start();
	}

	@PreDestroy
	public void destroy() {
		if (reporter != null) {
			reporter.close();
		}
	}

	@Override
	public <T> CheckedCommand<T> timeExecution(final CheckedCommand<T> execution, final String group, final String name) {
		return () -> {
			Timer timer = metrics.timer(group + "#" + name);
			Context context = timer.time();
			try {
				return execution.call();
			} finally {
				context.stop();
			}
		};
	}

	@Override
	public <T> Supplier<Observable<T>> timeObservable(final Supplier<Observable<T>> observableFactory, final String group, final String name) {
		return () -> {
			Timer timer = metrics.timer(group + "#" + name);
			final Context context = timer.time();
			return observableFactory.get().doOnTerminate(new Action0() {
				@Override
				public void call() {
					context.stop();
				}
			});
		};
	}
	
	// For testing
	MetricRegistry getMetrics() {
		return metrics;
	}

}
