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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategyDefault;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifierDefault;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategyDefault;

final class HystrixStrategyMapping {
	
	private static Pattern pattern = Pattern.compile("\\[(\\d+)\\]$");
	
	private final Map<String, HystrixStrategies> strategiesById = new ConcurrentHashMap<>();

	private Optional<HystrixStrategies> getStrategies(String strategiesId) {
		return Optional.ofNullable(strategiesById.get(strategiesId));
	}
	
	void registerStrategies(HystrixStrategies strategies) {
		strategiesById.put(strategies.getId(), strategies);
	}

	HystrixStrategies getHystrixStrategies(HystrixCommandKey commandKey) {
		String contextId = parseStrategiesId(commandKey.name()).orElse("1");
		return getStrategies(contextId).orElseGet(HystrixStrategyMapping::getDefault);
	}
	
	HystrixStrategies getHystrixStrategies(HystrixThreadPoolKey commandKey) {
		String contextId = parseStrategiesId(commandKey.name()).orElse("1");
		return getStrategies(contextId).orElseGet(HystrixStrategyMapping::getDefault);
	}
	
	Optional<String> parseStrategiesId(String commandKey) {
		Matcher matcher = pattern.matcher(commandKey);
		if (matcher.find()) {
			return Optional.of(matcher.group(1));
		}
		return Optional.empty();
	}
	
	static HystrixStrategies getDefault() {
		return new HystrixStrategies(HystrixPropertiesStrategyDefault.getInstance(),
									 HystrixConcurrencyStrategyDefault.getInstance(),
									 HystrixEventNotifierDefault.getInstance(),
									 "default");
	}
	
}