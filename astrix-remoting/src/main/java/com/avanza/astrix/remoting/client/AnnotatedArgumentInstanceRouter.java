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

import com.avanza.astrix.core.remoting.Router;
import com.avanza.astrix.core.remoting.RoutingKey;

public class AnnotatedArgumentInstanceRouter implements Router {
	
	private int argumentIndex;
	private Method routingKeyMethod;
	
	public AnnotatedArgumentInstanceRouter(int argumentIndex, Method routingKeyMethod) {
		this.argumentIndex = argumentIndex;
		this.routingKeyMethod = routingKeyMethod;
	}

	@Override
	public RoutingKey getRoutingKey(Object... args) throws Exception {
		return RoutingKey.create(routingKeyMethod.invoke(args[argumentIndex]));
	}
}