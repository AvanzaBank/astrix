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
package com.avanza.astrix.ft.metrics;

import java.util.HashMap;
import java.util.Map;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixMetricsCollectorPlugin;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandMetrics;
import com.netflix.hystrix.HystrixCommandMetrics.HealthCounts;
import com.netflix.hystrix.HystrixThreadPoolMetrics;

/**
 * @author Andreas Skoog (andsko)
 * @author Kristoffer Erlandsson (krierl)
 */
@MetaInfServices(AstrixMetricsCollectorPlugin.class)
public final class HystrixMetricsCollector implements AstrixMetricsCollectorPlugin {
	
	@Override
	public Map<String, Number> getMetrics() {
		Map<String, Number> metrics = new HashMap<>();
		for (HystrixCommandMetrics commandMetrics : HystrixCommandMetrics.getInstances()) {
			addCommandMetrics(commandMetrics, metrics);
		}
		for (HystrixThreadPoolMetrics threadPoolMetrics : HystrixThreadPoolMetrics.getInstances()) {
			addThreadPoolMetrics(threadPoolMetrics, metrics);
		}
		return metrics;
	}

	// Most of the code in this methods is based on code in HystrixMetricsPoller
	private void addCommandMetrics(HystrixCommandMetrics commandMetrics, Map<String, Number> metrics) {
		HystrixCommandKey key = commandMetrics.getCommandKey();
		HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(key);

		String keyPrefix = String.format("hystrix.${host}.%s.%s", commandMetrics.getCommandGroup().name(), key.name());

		if (circuitBreaker != null) {
			addMetric(metrics, keyPrefix, "isCircuitBreakerOpen", circuitBreaker.isOpen() ? 1 : 0);
		}
		HealthCounts healthCounts = commandMetrics.getHealthCounts();
		addMetric(metrics, keyPrefix, "errorCount", healthCounts.getErrorCount());
		addMetric(metrics, keyPrefix, "requestCount", healthCounts.getTotalRequests());

		addMetric(metrics, keyPrefix,
				"currentConcurrentExecutionCount", commandMetrics.getCurrentConcurrentExecutionCount());

		// latency percentiles
		addMetric(metrics, keyPrefix, "latencyExecute_mean", commandMetrics.getExecutionTimeMean());
		addMetric(metrics, keyPrefix, "latencyExecute.0", commandMetrics.getExecutionTimePercentile(0));
		addMetric(metrics, keyPrefix, "latencyExecute.25", commandMetrics.getExecutionTimePercentile(25));
		addMetric(metrics, keyPrefix, "latencyExecute.50", commandMetrics.getExecutionTimePercentile(50));
		addMetric(metrics, keyPrefix, "latencyExecute.75", commandMetrics.getExecutionTimePercentile(75));
		addMetric(metrics, keyPrefix, "latencyExecute.90", commandMetrics.getExecutionTimePercentile(90));
		addMetric(metrics, keyPrefix, "latencyExecute.95", commandMetrics.getExecutionTimePercentile(95));
		addMetric(metrics, keyPrefix, "latencyExecute.99", commandMetrics.getExecutionTimePercentile(99));
		addMetric(metrics, keyPrefix, "latencyExecute.99_5", commandMetrics.getExecutionTimePercentile(99.5));
		addMetric(metrics, keyPrefix, "latencyExecute.100", commandMetrics.getExecutionTimePercentile(100));
		//
		addMetric(metrics, keyPrefix, "latencyTotal_mean", commandMetrics.getTotalTimeMean());
		addMetric(metrics, keyPrefix, "latencyTotal.0", commandMetrics.getTotalTimePercentile(0));
		addMetric(metrics, keyPrefix, "latencyTotal.25", commandMetrics.getTotalTimePercentile(25));
		addMetric(metrics, keyPrefix, "latencyTotal.50", commandMetrics.getTotalTimePercentile(50));
		addMetric(metrics, keyPrefix, "latencyTotal.75", commandMetrics.getTotalTimePercentile(75));
		addMetric(metrics, keyPrefix, "latencyTotal.90", commandMetrics.getTotalTimePercentile(90));
		addMetric(metrics, keyPrefix, "latencyTotal.95", commandMetrics.getTotalTimePercentile(95));
		addMetric(metrics, keyPrefix, "latencyTotal.99", commandMetrics.getTotalTimePercentile(99));
		addMetric(metrics, keyPrefix, "latencyTotal.99_5", commandMetrics.getTotalTimePercentile(99.5));
		addMetric(metrics, keyPrefix, "latencyTotal.100", commandMetrics.getTotalTimePercentile(100));
	}
	
	private void addThreadPoolMetrics(HystrixThreadPoolMetrics threadPoolMetrics, Map<String, Number> metrics) {
		String keyPrefix = String.format("hystrix.${host}.%s", threadPoolMetrics.getThreadPoolKey().name());
        addMetric(metrics, keyPrefix, "currentActiveCount", threadPoolMetrics.getCurrentActiveCount().intValue());
        addMetric(metrics, keyPrefix, "currentCompletedTaskCount", threadPoolMetrics.getCurrentCompletedTaskCount().longValue());
        addMetric(metrics, keyPrefix, "currentCorePoolSize", threadPoolMetrics.getCurrentCorePoolSize().intValue());
        addMetric(metrics, keyPrefix, "currentLargestPoolSize", threadPoolMetrics.getCurrentLargestPoolSize().intValue());
        addMetric(metrics, keyPrefix, "currentMaximumPoolSize", threadPoolMetrics.getCurrentMaximumPoolSize().intValue());
        addMetric(metrics, keyPrefix, "currentPoolSize", threadPoolMetrics.getCurrentPoolSize().intValue());
        addMetric(metrics, keyPrefix, "currentQueueSize", threadPoolMetrics.getCurrentQueueSize().intValue());
        addMetric(metrics, keyPrefix, "currentTaskCount", threadPoolMetrics.getCurrentTaskCount().longValue());
        addMetric(metrics, keyPrefix, "rollingCountThreadsExecuted", threadPoolMetrics.getRollingCountThreadsExecuted());
        addMetric(metrics, keyPrefix, "rollingMaxActiveThreads", threadPoolMetrics.getRollingMaxActiveThreads());

        addMetric(metrics, keyPrefix, "propertyValue_queueSizeRejectionThreshold", threadPoolMetrics.getProperties().queueSizeRejectionThreshold().get());
        addMetric(metrics, keyPrefix, "propertyValue_metricsRollingStatisticalWindowInMilliseconds", threadPoolMetrics.getProperties().metricsRollingStatisticalWindowInMilliseconds().get());
	}

	private void addMetric(Map<String, Number> metrics, String keyPrefix, String metricsKey, long value) {
		metrics.put(String.format("%s.%s", keyPrefix, metricsKey), value);
	}

	@Override
	public String toString() {
		return "HystrixMetricsCollector";
	}
	
	

}
