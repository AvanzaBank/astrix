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
package com.avanza.astrix.remoting.client;

import java.lang.reflect.Method;
import java.util.Objects;

import com.avanza.astrix.core.util.ReflectionUtil;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
abstract class PartitionedRouter {
	
	private PartitionedRouter() {
	}
	
	abstract Object getRoutingKey(Object element);
	
	/**
	 * Uses the given argument as routing key.<p>
	 * 
	 * @return
	 */
	public static PartitionedRouter identity() {
		return new Identity();
	}

	public static PartitionedRouter routingMethod(Method routingMethod) {
		return new RoutingMethod(routingMethod);
	}
	
	private static class Identity extends PartitionedRouter {
		@Override
		Object getRoutingKey(Object element) {
			return element;
		}
	}
	private static class RoutingMethod extends PartitionedRouter {
		private final Method method;
		public RoutingMethod(Method method) {
			this.method = Objects.requireNonNull(method);
		}
		@Override
		Object getRoutingKey(Object element) {
			try {
				return ReflectionUtil.invokeMethod(method, element, null);
			} catch (Throwable e) {
				throw new RuntimeException("Failed to invoke routing Method on: " + element, e);
			}
		}
	}
}
