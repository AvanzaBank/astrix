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
package com.avanza.hystrix.multiconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

public class MultiConfigs {
	//TODO: Default strategy?
	//TODO: Error handling
	//TODO: Remaining strategies
	
	private static final Logger LOGGER = LoggerFactory.getLogger(MultiConfigs.class);
	private static MultiPropertiesDispatcher multiPropertiesDispatcher = new MultiPropertiesDispatcher();
	
	static {
		registerWithHystrix();
	}
	
	public static void register(String id, HystrixPropertiesStrategy strategy) {
		multiPropertiesDispatcher.register(id, strategy);
		verifyRegistered();
	}
	
	private static void verifyRegistered() {
		synchronized (multiPropertiesDispatcher) {
			if (!HystrixPlugins.getInstance().getPropertiesStrategy().getClass().equals(MultiPropertiesDispatcher.class)) {
				LOGGER.warn(MultiPropertiesDispatcher.class.getName() + " not yet registered with Hystrix, registering...");
				registerWithHystrix();
			}
		}
	}
	
	private static void registerWithHystrix() {
		HystrixPlugins.getInstance().registerPropertiesStrategy(multiPropertiesDispatcher);
//		HystrixPlugins.getInstance().registerConcurrencyStrategy(impl);
//		HystrixPlugins.getInstance().registerEventNotifier(impl);
		LOGGER.info(MultiPropertiesDispatcher.class.getName() + " registered with Hystrix!");
	}
	
}