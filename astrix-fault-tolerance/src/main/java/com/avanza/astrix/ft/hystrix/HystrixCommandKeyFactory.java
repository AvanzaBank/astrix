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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;

final class HystrixCommandKeyFactory {
	
	private String astrixContextId;
	private HystrixCommandNamingStrategy commandNamingStrategy;
	
	HystrixCommandKeyFactory(String astrixContextId, HystrixCommandNamingStrategy commandNamingStrategy) {
		this.astrixContextId = astrixContextId;
		this.commandNamingStrategy = commandNamingStrategy;
	}

	HystrixCommandKey createCommandKey(AstrixBeanKey<?> beanKey) {
		String commandKeyName = this.commandNamingStrategy.getCommandKeyName(beanKey);
		if (!astrixContextId.equals("1")) {
			commandKeyName = commandKeyName + "[" + astrixContextId + "]";
		}
		return HystrixCommandKey.Factory.asKey(commandKeyName);
	}
	
	HystrixCommandGroupKey createGroupKey(AstrixBeanKey<?> beanKey) {
		String commandKeyName = this.commandNamingStrategy.getCommandKeyName(beanKey);
		if (!astrixContextId.equals("1")) {
			commandKeyName = commandKeyName + "[" + astrixContextId + "]";
		}
		return HystrixCommandGroupKey.Factory.asKey(commandKeyName);
	}

	
	
}
