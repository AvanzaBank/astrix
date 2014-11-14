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
package com.avanza.asterix.ft.metrics;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.asterix.context.AsterixEventLogger;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

public final class HystrixEventPublisher extends HystrixEventNotifier {

	private static final Logger log = LoggerFactory.getLogger(HystrixEventPublisher.class);
	private AsterixEventLogger eventLogger;

	public HystrixEventPublisher(AsterixEventLogger eventLogger) {
		this.eventLogger = Objects.requireNonNull(eventLogger);
	}

	@Override
	public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
		logEvent(eventType, key);
		String statsMetric = getMetricName(eventType, key);
		increment(statsMetric);
	}

	private void logEvent(HystrixEventType eventType, HystrixCommandKey key) {
		switch (eventType) {
			case FAILURE:
			case SEMAPHORE_REJECTED:
			case THREAD_POOL_REJECTED:
			case TIMEOUT:
			case SHORT_CIRCUITED:
				log.info(String.format("Aborted command execution: cause=%s circuit=%s", eventType, key.name()));
				break;
			default:
				// Do nothing
		}
	}

	private void increment(String statsMetric) {
		eventLogger.increment(statsMetric);
	}

	private String getMetricName(HystrixEventType eventType,
			HystrixCommandKey key) {
		String commandName = key.name();
		String statsMetric = String.format("hystrix.%s.%s", commandName.replace('_', '.'), eventType);
		return statsMetric;
	}
}	
