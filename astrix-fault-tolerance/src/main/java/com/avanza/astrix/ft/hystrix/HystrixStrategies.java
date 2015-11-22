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

import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

final class HystrixStrategies {
	
	private HystrixPropertiesStrategy hystrixPropertiesStrategy;
	private HystrixConcurrencyStrategy hystrixConcurrencyStrategy;
	private HystrixEventNotifier hystrixEventNotifier;
	private String id;
	
	public HystrixStrategies(HystrixPropertiesStrategy hystrixPropertiesStrategy,
							 HystrixConcurrencyStrategy concurrencyStrategy,
							 HystrixEventNotifier eventNotifier,
							 String id) {
		this.hystrixPropertiesStrategy = hystrixPropertiesStrategy;
		this.hystrixConcurrencyStrategy = concurrencyStrategy;
		this.hystrixEventNotifier = eventNotifier;
		this.id = id;
	}

	public HystrixPropertiesStrategy getHystrixPropertiesStrategy() {
		return hystrixPropertiesStrategy;
	}
	
	/**
	 * The id of this set of HystrixStrategies. <p>
	 * 
	 * @return
	 */
	public String getId() {
		return this.id;
	}

	public HystrixConcurrencyStrategy getHystrixConcurrencyStrategy() {
		return this.hystrixConcurrencyStrategy;
	}

	public HystrixEventNotifier getHystrixEventNotifier() {
		return this.hystrixEventNotifier;
	}
	

}
