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
package com.avanza.astrix.ft;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

import com.avanza.astrix.beans.publish.PublishedAstrixBean;

public class CountingCachingHystrixCommandNamingStrategy implements HystrixCommandNamingStrategy {

	private static final AtomicInteger counter = new AtomicInteger();
	private final ConcurrentMap<String, GroupAndCommandKey> groupAndCommandByBeanDefinition = new ConcurrentHashMap<>();
	
	@Override
	public String getCommandKeyName(PublishedAstrixBean<?> beanDefinition) {
		return getGroupAndCommandKey(beanDefinition).commandKey;
	}

	@Override
	public String getGroupKeyName(PublishedAstrixBean<?> beanDefinition) {
		return getGroupAndCommandKey(beanDefinition).groupKey;
	}
	
	private GroupAndCommandKey getGroupAndCommandKey(PublishedAstrixBean<?> beanDefinition) {
		String key = keyFor(beanDefinition);
		GroupAndCommandKey groupAndCommandKey = this.groupAndCommandByBeanDefinition.get(key);
		if (groupAndCommandKey != null) {
			return groupAndCommandKey;
		}
		this.groupAndCommandByBeanDefinition.putIfAbsent(key, new GroupAndCommandKey(beanDefinition));
		groupAndCommandKey = this.groupAndCommandByBeanDefinition.get(key);
		return groupAndCommandKey;
	}
	
	private String keyFor(PublishedAstrixBean<?> beanDefinition) {
		return beanDefinition.getDefiningApi().getName() + "###" + beanDefinition.getBeanKey().toString();
	}
	
	private static class GroupAndCommandKey {
		private final String groupKey;
		private final String commandKey;
		
		public GroupAndCommandKey(PublishedAstrixBean<?> beanDefinition) {
			groupKey = beanDefinition.getDefiningApi().getName() + "[" + counter.incrementAndGet() + "]";
			commandKey = beanDefinition.getBeanKey().toString() + "[" + counter.incrementAndGet() + "]";
		}
	}

}
