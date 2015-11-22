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

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixThreadPoolKey;

final class BeanMapping {
	
	private final ConcurrentMap<String, AstrixBeanKey<?>> beanKeyByCommandName = new ConcurrentHashMap<>();
	
	public Optional<AstrixBeanKey<?>> getBeanKey(HystrixCommandKey commandKey) {
		return Optional.ofNullable(this.beanKeyByCommandName.get(commandKey.name()));
	}
	
	public Optional<AstrixBeanKey<?>> getBeanKey(HystrixThreadPoolKey threadPoolKey) {
		/*
		 * Astrix always uses commandKey as threadPoolKey
		 */
		return Optional.ofNullable(this.beanKeyByCommandName.get(threadPoolKey.name()));
	}
	
	public void registerBeanKey(String commandName, AstrixBeanKey<?> beanKey) {
		this.beanKeyByCommandName.putIfAbsent(commandName, beanKey);
	}


}
