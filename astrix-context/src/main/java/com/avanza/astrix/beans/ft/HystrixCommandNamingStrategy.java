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
package com.avanza.astrix.beans.ft;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface HystrixCommandNamingStrategy {
	
	/**
	 * @deprecated - As of 0.39.X Astrix uses distinct groups for each bean, this method is never invoked
	 */
	@Deprecated
	default String getCommandKeyName(PublishedAstrixBean<?> beanDefinition) {
		return getCommandKeyName(beanDefinition.getBeanKey());
	}
	
	/**
	 * @deprecated - As of 0.39.X Astrix uses distinct groups for each bean, this method is never invoked
	 */
	@Deprecated
	default String getGroupKeyName(PublishedAstrixBean<?> beanDefinition) {
		return getCommandKeyName(beanDefinition.getBeanKey());
	}
	
	/**
	 * Returns the Hystrix command key name used to protect invocations to a given Astrix bean.
	 * 
	 * Default implementation uses the toString() representation of the AstrixBeanKey.
	 * 
	 * NOTE: commandKeyNames must be unique, i.e there must be a one-to-one mapping between an AstrixBeanKey and
	 * the commandKeyName. Otherwise the fault-tolerance layer might behave in
	 * unexpected ways when different beans are protected by the same HystrixCommand.
	 * 
	 * @param astrixBeanKey
	 * @return
	 */
	default String getCommandKeyName(AstrixBeanKey<?> astrixBeanKey) {
		return astrixBeanKey.toString();
	}
	
}
