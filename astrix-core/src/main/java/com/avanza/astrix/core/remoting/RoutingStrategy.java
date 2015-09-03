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
package com.avanza.astrix.core.remoting;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
/**
 * A RoutingStrategy is a factory for creating a Router for a service method.
 * 
 * @author Elias Lindholm
 *
 */
public interface RoutingStrategy {
	
	/**
	 * Creates a router for a given service Method.
	 * 
	 * This method is only called once for a given service method.
	 * 
	 * @param serviceMethod
	 * @return
	 */
	Router create(Method serviceMethod);
	
	public static class RoundRobin implements RoutingStrategy {
		private final AtomicInteger next = new AtomicInteger();
		@Override
		public Router create(Method serviceMethod) {
			return new Router() {
				@Override
				public RoutingKey getRoutingKey(Object... args) throws Exception {
					return RoutingKey.create(next.incrementAndGet());
				}
			};
		}
	}
	
	public static class Static implements RoutingStrategy {
		@Override
		public Router create(Method serviceMethod) {
			return new Router() {
				@Override
				public RoutingKey getRoutingKey(Object... args) throws Exception {
					return RoutingKey.create(0);
				}
			};
		}
	}

}
