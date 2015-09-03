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

/**
 * Used to route remote service invocations by extracting a RoutingKey
 * from the arguments to a remote service method. <p>
 * 
 * @author Elias Lindholm
 *
 */
public interface Router {
	
	/**
	 * Calculates the RoutingKey for a given remote service invocation. <p>
	 * 
	 * This method must return a non-null value.
	 * 
	 * @param args
	 * @return
	 * @throws Exception
	 */
	RoutingKey getRoutingKey(Object... args) throws Exception;
}