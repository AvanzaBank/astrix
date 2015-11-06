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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.Hystrix;
import com.netflix.hystrix.strategy.HystrixPlugins;

final class HystrixStrategyDispatcher {
	
	private static final Logger log = LoggerFactory.getLogger(HystrixFaultTolerance.class);
	private static final HystrixStrategyMapping strategyMapping = new HystrixStrategyMapping();
	
	static {
		initHystrixPlugins();
	}


	/**
	 * Registers a set of HystrixStrategies in the dispatcher.
	 * 
	 * @param hystrixStrategies
	 */
	static void registerStrategies(HystrixStrategies hystrixStrategies) {
		strategyMapping.registerStrategies(hystrixStrategies);
		verifyInitialized();
	}
	
	private static void verifyInitialized() {
		// Hystrix already initialized, verify that Astrix-plugins are used
		synchronized (log) {
			if (!HystrixPlugins.getInstance().getPropertiesStrategy().getClass().equals(PropertiesStrategyDispatcher.class)) {
				log.warn("Hystrix is not properly initialized. This means that the current Hystrix configuration was reset outside of Astrix. "
						+ "Astrix will reset Hystrix configuration and register custom Astrix stratgeis");
				HystrixPlugins.reset();
				registerDispatcherStrategies();
			}
		}
	}
	
	private static void initHystrixPlugins() {
		try {
			registerDispatcherStrategies();
		} catch (Exception e) {
			log.warn("Failed to init Hystrix with custom Astrix strategies. Hystrix configuration will be reset and one more registreation attempt will be performed", e);
			Hystrix.reset();
			registerDispatcherStrategies();
		}
	}
	
	private static void registerDispatcherStrategies() {
		HystrixPlugins.getInstance().registerPropertiesStrategy(new PropertiesStrategyDispatcher(strategyMapping));
		HystrixPlugins.getInstance().registerConcurrencyStrategy(new ConcurrencyStrategyDispatcher(strategyMapping));
		HystrixPlugins.getInstance().registerEventNotifier(new EventNotifierDispatcher(strategyMapping));
		log.info("Successfully initialized Hystrix with custom Astrix strategies");
	}


}
