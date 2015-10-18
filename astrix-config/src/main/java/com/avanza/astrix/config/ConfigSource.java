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
 * The SPI used to plug in a configuration source into a {@link DynamicConfig} instance.
 * 
 * NOTE: Dynamic configuration sources should implement the {@link DynamicConfigSource} interface
 * rather than this one. 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface ConfigSource {
	
	/**
	 * Reads a property from this ConfigSource.
	 *  
	 * @param propertyName - The name of the property to read
	 * @return The value of the given property, or null if this ConfigSource does not contain a value for the tiven property
	 */
	String get(String propertyName);
	
}
