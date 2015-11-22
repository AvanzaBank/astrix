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
 * A property that can change state even after it is read
 * from a {@link DynamicConfigSource}. <p>
 * 
 * DynamicProperty instances are designed to be lightweight with no dependencies
 * to the underlying configuration sources, thereby keeping consumers of DynamicProperty
 * instances decoupled from the underlying configuration sources. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface DynamicProperty<T> {
	
	/**
	 * Adds a given listener to the underlying property. The listener
	 * will be notified each time the property value changes.<p>
	 * 
	 * Events are not guaranteed to be received in the same order
	 * as the underlying property is set.<p>
	 * 
	 * The listener will be notified synchronously on the same thread
	 * that mutates the underlying property, so don't do any long-running
	 * work on the thread thats notifies the {@link DynamicPropertyListener}.<p>
	 * 
	 * @param listener
	 */
	void addListener(DynamicPropertyListener<T> listener);
	
	/**
	 * Removes a given listener. After remove the given listener will 
	 * not receive events when this property is changed. 
	 * 
	 * @param listener
	 */
	void removeListener(DynamicPropertyListener<T> listener);

	
	/**
	 * Returns the current value of this DynamicPropery. For primitive
	 * types the boxed version is returned (never null).
	 * 
	 * @return
	 */
	T getCurrentValue();

}
