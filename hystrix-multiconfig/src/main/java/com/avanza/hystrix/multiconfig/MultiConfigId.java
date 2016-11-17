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

import java.util.Objects;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

public class MultiConfigId {
	// TODO: Cache mappings!
	private static Pattern pattern = Pattern.compile("\\[([a-zA-Z0-9-_]+)\\](.+)");
	
	private String propertySourceId;

	private MultiConfigId(String propertySourceId) {
		this.propertySourceId = Objects.requireNonNull(propertySourceId);
	}

	// Factory methods

	public static MultiConfigId create(String id) {
		return new MultiConfigId(id);
	}
	
	public static MultiConfigId readFrom(HystrixCommandKey commandKey) {
		return consumeMatch(commandKey.name(), (matcher) -> create(matcher.group(1)));
	}
	
	public static MultiConfigId readFrom(HystrixCollapserKey collapserKey) {
		return consumeMatch(collapserKey.name(), (matcher) -> create(matcher.group(1)));
	}
	
	public static MultiConfigId readFrom(HystrixThreadPoolKey threadPoolKey) {
		return consumeMatch(threadPoolKey.name(), (matcher) -> create(matcher.group(1)));
	}
	
	// Static helper methods
	
	public static boolean hasMultiSourceId(HystrixCommandKey commandKey) {
		return pattern.matcher(commandKey.name()).find();
	}
	
	public static boolean hasMultiSourceId(HystrixCollapserKey collapserKey) {
		return pattern.matcher(collapserKey.name()).find();
	}
	
	public static boolean hasMultiSourceId(HystrixThreadPoolKey threadPoolKey) {
		return pattern.matcher(threadPoolKey.name()).find();
	}
	
	public static HystrixCommandKey decode(HystrixCommandKey commandKey) {
		return consumeMatch(commandKey.name(), (matcher) -> HystrixCommandKey.Factory.asKey(matcher.group(2)));
	}
	
	public static HystrixCollapserKey decode(HystrixCollapserKey collapserKey) {
		return consumeMatch(collapserKey.name(), (matcher) -> HystrixCollapserKey.Factory.asKey(matcher.group(2)));
	}
	
	public static HystrixThreadPoolKey decode(HystrixThreadPoolKey threadPoolKey) {
		return consumeMatch(threadPoolKey.name(), (matcher) -> HystrixThreadPoolKey.Factory.asKey(matcher.group(2)));
	}
	
	private static <T> T consumeMatch(String name, Function<Matcher, T> mapping) {
		Matcher matcher = pattern.matcher(name);
		if (matcher.find()) {
			return mapping.apply(matcher);
		} else {
			throw new IllegalArgumentException("Could not read id from name=" + name);
		}
	}
	
	// public
	
	public HystrixCommandKey encode(HystrixCommandKey hystrixCommandKey) {
		return createCommandKey(hystrixCommandKey.name());
	}
	
	public HystrixCommandGroupKey encode(HystrixCommandGroupKey hystrixCommandGroupKey) {
		return createCommandGroupKey(hystrixCommandGroupKey.name());
	}
	
	public HystrixCollapserKey encode(HystrixCollapserKey hystrixCollapserKey) {
		return createCollapserKey(hystrixCollapserKey.name());
	}
	
	public HystrixThreadPoolKey encode(HystrixThreadPoolKey hystrixThreadPoolKey) {
		return createThreadPoolKey(hystrixThreadPoolKey.name());
	}
	
	public HystrixCommandKey createCommandKey(String commandKeyName) {
		return HystrixCommandKey.Factory.asKey(getPrefix() + commandKeyName);
	}
	
	public HystrixCommandGroupKey createCommandGroupKey(String commandKeyName) {
		return HystrixCommandGroupKey.Factory.asKey(getPrefix() + commandKeyName);
	}
	
	public HystrixCollapserKey createCollapserKey(String commandGroupName) {
		return HystrixCollapserKey.Factory.asKey(getPrefix() + commandGroupName);
	}
	
	public HystrixThreadPoolKey createThreadPoolKey(String threadPoolKey) {
		return HystrixThreadPoolKey.Factory.asKey(getPrefix() + threadPoolKey);
	}
	
	public String getPrefix() {
		return "[" + propertySourceId + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(propertySourceId);
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (getClass() != obj.getClass()) {
			return false;
		}
		MultiConfigId other = (MultiConfigId) obj;
		return this.propertySourceId.equals(other.propertySourceId);
	}

	@Override
	public String toString() {
		return propertySourceId;
	}
	
}
