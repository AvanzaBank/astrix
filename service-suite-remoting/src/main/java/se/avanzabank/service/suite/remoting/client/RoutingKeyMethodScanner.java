/*
 * Copyright 2014-2015 Avanza Bank AB
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
package se.avanzabank.service.suite.remoting.client;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicReference;

import com.gigaspaces.annotation.pojo.SpaceRouting;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
final class RoutingKeyMethodScanner {

	/**
	 * Inspects the given Object looking for a routing key method. 
	 * 
	 * @param spaceObject
	 * @return	the routing key method if one exists, null otherwise
	 */
	public Method getRoutingKeyMethod(Class<?> spaceObjectClass) {
		AtomicReference<Method> spaceIdField = new AtomicReference<>();
		for (Method m : spaceObjectClass.getMethods()) {
			if (m.isAnnotationPresent(SpaceRouting.class)) {
				// Found space routing property, return immediately
				// TODO: what if multiple fields are annotated with @SpaceRouting? throw exception?
				return m;
			}
			// TODO: Should we allow @SpaceId as an routing key identifier? 
//			if (m.isAnnotationPresent(SpaceId.class)) {
//				 Store SpaceId property for fall back if no SpaceRouting property defined.
//				spaceIdField.set(m);
//			}
		}
		return spaceIdField.get();
	}

}
