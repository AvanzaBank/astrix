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
 * The SPI used to plug in a dynamic configuration source into a {@link DynamicConfig} instance. <p>
 * 
 * A dynamic configuration source allows the client to listen for updates in the underlying configuration 
 * source.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface DynamicConfigSource extends ConfigSource {
	
	
	/**
	 * Returns a configuration property from this configuration source. The {@link DynamicPropertyListener} 
	 * will be updated about all changes to the underlying property. <p>
	 * 
	 * @param propertyName
	 * @param propertyChangeListener - A listener that receives callback for each change in the underlying property value.
	 * @return The current value of the given property, or null if no value exists right now
	 */
	String get(String propertyName, DynamicPropertyListener<String> propertyChangeListener);

}
