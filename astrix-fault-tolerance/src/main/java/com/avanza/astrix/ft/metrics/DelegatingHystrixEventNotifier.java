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

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

/**
 * Event notifier that delegates to zero or more notifiers. Allows us to add notifiers after Hystrix has been
 * initialized and allows us to use multiple notifiers.
 * 
 * @author Kristoffer Erlandsson (krierl)
 */
public final class DelegatingHystrixEventNotifier extends HystrixEventNotifier {

	Collection<HystrixEventNotifier> notifiers = new CopyOnWriteArrayList<HystrixEventNotifier>();

	private static DelegatingHystrixEventNotifier registeredInstance;

	private static final Logger log = LoggerFactory.getLogger(DelegatingHystrixEventNotifier.class);

	public synchronized static DelegatingHystrixEventNotifier getRegisteredNotifierOrRegisterNew() {
		if (registeredInstance == null) {
			registeredInstance = new DelegatingHystrixEventNotifier();
			try {
				HystrixPlugins.getInstance().registerEventNotifier(registeredInstance);
			} catch (IllegalStateException e) {
				HystrixEventNotifier alreadyRegisteredNotifier = HystrixPlugins.getInstance().getEventNotifier();
				log.warn(
						"Could not register DelegatingHystrixEventNotifier. Command execution events will not be tracked. Already registered notifier: "
								+ alreadyRegisteredNotifier, e);
				// Return the delegating event notifier anyways so that clients not need to adapt. It will never be called though.
			}
		}
		return registeredInstance;
	}

	private DelegatingHystrixEventNotifier() {
	}

	@Override
	public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
		for (HystrixEventNotifier notifier : notifiers) {
			notifier.markEvent(eventType, key);
		}
	}

	@Override
	public void markCommandExecution(HystrixCommandKey key, ExecutionIsolationStrategy isolationStrategy, int duration,
			List<HystrixEventType> eventsDuringExecution) {
		for (HystrixEventNotifier notifier : notifiers) {
			notifier.markCommandExecution(key, isolationStrategy, duration, eventsDuringExecution);
		}
	}

	public void addEventNotifier(HystrixEventNotifier notifier) {
		this.notifiers.add(notifier);
	}

}
