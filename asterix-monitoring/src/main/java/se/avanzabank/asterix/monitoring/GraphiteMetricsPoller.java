/*
 * Copyright 2014-2015 Avanza Bank AB
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
package se.avanzabank.asterix.monitoring;

import java.util.Collection;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.kohsuke.MetaInfServices;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.avanzabank.asterix.context.AsterixMetricsCollectorPlugin;
import se.avanzabank.asterix.context.AsterixMetricsLoggerPlugin;
import se.avanzabank.asterix.context.AsterixMetricsPollerPlugin;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.core.support.jndi.lookup.BasicJNDILookup;
import se.avanzabank.core.support.jndi.lookup.http.JndiLookupFailedException;
import se.avanzabank.core.util.thread.NamedThreadFactory;

@MetaInfServices(AsterixMetricsPollerPlugin.class)
public class GraphiteMetricsPoller implements AsterixMetricsPollerPlugin, AsterixPluginsAware {

	private static final Integer DEFAULT_DELAY = 5000;
	private AsterixPlugins plugins;
	private AsterixMetricsLoggerPlugin logger;
	private Collection<AsterixMetricsCollectorPlugin> collectors;
	ScheduledExecutorService executor;

	private static final Logger log = LoggerFactory.getLogger(GraphiteMetricsPoller.class);
	
	@Override
	public void start() {
		initializeFromPlugins();
		int delayTime = getDelayTimeFromJndiOrFallback();
		executor = Executors.newScheduledThreadPool(1, new NamedThreadFactory("GraphiteMetricsPoller")); 
		executor.scheduleAtFixedRate(new MetricPollerTask(logger, collectors), 500, delayTime, TimeUnit.MILLISECONDS);
	}
	
	private void initializeFromPlugins() {
		if (logger == null) {
			logger = plugins.getPlugin(AsterixMetricsLoggerPlugin.class);
		}
		if (collectors == null) {
			collectors = plugins.getPlugins(AsterixMetricsCollectorPlugin.class);
		}
	}

	private int getDelayTimeFromJndiOrFallback() {
		try {
			return new BasicJNDILookup().lookup("HYSTRIX_GRAPHITE_DELAY_TIME", DEFAULT_DELAY);
		} catch (JndiLookupFailedException e) {
			// Handle missing JNDI property or servers
			log.warn("Failed to lookup hystrix delay time property. Using default value " + DEFAULT_DELAY);
			return DEFAULT_DELAY;
		}
	}

	/**
	 * Stops the scheduled poller. Does not wait for anything to terminate.
	 */
	@Override
	public void stop() {
		if (executor != null) {
			executor.shutdown();
		}
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
	public void setMetricsLogger(AsterixMetricsLoggerPlugin logger) {
		this.logger = logger;
	}
	
	public void setMetricsCollectors(Collection<AsterixMetricsCollectorPlugin> collectors) {
		this.collectors = collectors;
	}
	
	private static class MetricPollerTask implements Runnable {

		private AsterixMetricsLoggerPlugin logger;
		private Collection<AsterixMetricsCollectorPlugin> collectors;
		
		public MetricPollerTask(AsterixMetricsLoggerPlugin logger, Collection<AsterixMetricsCollectorPlugin> collectors) {
			this.logger = logger;
			this.collectors = collectors;
		}

		@Override
		public void run() {
			for (AsterixMetricsCollectorPlugin collector : collectors) {
				logger.logMetrics(collector.getMetrics());
			}
		}

	}

}
