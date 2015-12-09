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
package com.avanza.astrix.config;
/**
 * Listener interface for listening to state changes in all {@link DynamicProperty} instances
 * managed by a single {@link DynamicConfig} instance.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface DynamicConfigListener {
	
	/**
	 * Invoked each time the value of a {@link DynamicProperty} instance
	 * changes.
	 * 
	 * This method will be invoked on the same thread that changes the state
	 * of the {@link DynamicProperty}, typically some thread internal to a
	 * {@link DynamicConfigSource} implementation.
	 * 
	 * Default implementation does nothing
	 * 
	 * @param newValue
	 */
	default void propertyChanged(String propertyName, Object newValue) {}
	
	/**
	 * Invoked the first time a property with a given name is requested from
	 * a {@link DynamicConfig} instance, i.e when it is created.
	 * 
	 * Default implementation does nothing
	 * 
	 * @param propertyName
	 * @param initialValue
	 */
	default void propertyCreated(String propertyName, Object initialValue) {}
	
}
