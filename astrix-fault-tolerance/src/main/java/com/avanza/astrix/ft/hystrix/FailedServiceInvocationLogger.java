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

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

final class FailedServiceInvocationLogger extends HystrixEventNotifier {
	
	private static final Logger log = LoggerFactory.getLogger(FailedServiceInvocationLogger.class);
	private final BeanMapping beanMapping;
	private final AstrixConfig astrixConfig;
	private final Map<AstrixBeanKey<?>, FailedBeanInvocationLogger> beanLoggerByBeanKey = new ConcurrentHashMap<>();
	
	public FailedServiceInvocationLogger(BeanMapping beanMapping, AstrixConfig config) {
		this.beanMapping = beanMapping;
		this.astrixConfig = config;
	}

	@Override
	public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
		logEvent(eventType, key);
	}

	private void logEvent(HystrixEventType eventType, HystrixCommandKey commandKey) {
		getBeanInvocationLogger(commandKey).orElse(createNonBeanInvocationCommandLogger(commandKey))
										   .accept(eventType);
	}
	
	private Consumer<HystrixEventType> createNonBeanInvocationCommandLogger(HystrixCommandKey key) {
		return eventType -> {
			switch (eventType) {
			case FAILURE:
			case SEMAPHORE_REJECTED:
			case THREAD_POOL_REJECTED:
			case TIMEOUT:
			case SHORT_CIRCUITED:
				log.info(String.format("Aborted command execution: cause=%s astrixBean=null hystrixCommandKey=%s", eventType, key.name()));
				break;
			default:
				// Do nothing
			}
		};
	}
	
	
	private Optional<Consumer<HystrixEventType>> getBeanInvocationLogger(HystrixCommandKey commandKey) {
		return beanMapping.getBeanKey(commandKey)
						  .map(beanKey -> getOrCreateBeanInvocationLogger(commandKey, beanKey));
	}

	private Consumer<HystrixEventType> getOrCreateBeanInvocationLogger(HystrixCommandKey commandKey,
			AstrixBeanKey<?> beanKey) {
		return beanLoggerByBeanKey.computeIfAbsent(beanKey, k -> createLogger(commandKey, beanKey));
	}

	private FailedBeanInvocationLogger createLogger(HystrixCommandKey commandKey, AstrixBeanKey<?> beanKey) {
		return new FailedBeanInvocationLogger(astrixConfig.getBeanConfiguration(beanKey), beanKey, commandKey);
	}
	
	private static final class FailedBeanInvocationLogger implements Consumer<HystrixEventType> {
		
		private final BeanConfiguration beanConfiguration;
		private final AstrixBeanKey<?> beanKey;
		private final HystrixCommandKey hystrixCommandKey;
		
		public FailedBeanInvocationLogger(BeanConfiguration beanConfiguration, 
				AstrixBeanKey<?> beanKey,
				HystrixCommandKey hystrixCommandKey) {
			this.beanConfiguration = beanConfiguration;
			this.beanKey = beanKey;
			this.hystrixCommandKey = hystrixCommandKey;
		}
		
		@Override
		public void accept(HystrixEventType t) {
			logEvent(t);
		}

		private void logEvent(HystrixEventType eventType) {
			switch (eventType) {
				case FAILURE:
					log.info(String.format("Aborted command execution: cause=%s astrixBean=%s hystrixCommandKey=%s", eventType, beanKey, hystrixCommandKey.name()));
					break;
				case SEMAPHORE_REJECTED:
					logSemaphoreRejectedRequest(eventType);
					break;
				case THREAD_POOL_REJECTED:
					logThreadPoolRejectedRequest(eventType);
					break;
				case TIMEOUT:
					logTimeoutRequest(eventType);
					break;
				case SHORT_CIRCUITED:
					log.info(String.format("Aborted command execution: cause=%s astrixBean=%s hystrixCommandKey=%s", eventType, beanKey, hystrixCommandKey.name()));
					break;
				default:
					// Do nothing
			}
		}

		private void logTimeoutRequest(HystrixEventType eventType) {
			log.info(String.format("Aborted command execution: cause=%s astrixBean=%s hystrixCommandKey=%s TIMEOUT=%s [ms]", 
					eventType, beanKey, hystrixCommandKey.name(), 
					beanConfiguration.get(AstrixBeanSettings.TIMEOUT).get()));
		}

		private void logThreadPoolRejectedRequest(HystrixEventType eventType) {
			log.info(String.format("Aborted command execution: cause=%s astrixBean=%s hystrixCommandKey=%s CORE_SIZE=%s QUEUE_SIZE_REJECTION_THRESHOLD=%s", 
					eventType, beanKey, hystrixCommandKey.name(), 
					beanConfiguration.get(AstrixBeanSettings.CORE_SIZE).get(), beanConfiguration.get(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD).get()));
		}

		private void logSemaphoreRejectedRequest(HystrixEventType eventType) {
			log.info(String.format("Aborted command execution: cause=%s astrixBean=%s hystrixCommandKey=%s MAX_CONCURRENT_REQUESTS=%s", 
					eventType, beanKey, hystrixCommandKey.name(), 
					beanConfiguration.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS).get()));
		}

	}
}