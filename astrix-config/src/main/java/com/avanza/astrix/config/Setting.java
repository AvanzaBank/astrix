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
 * This is an abstraction for an application/framework setting. A Setting
 * can be read from a {@link DynamicConfig} instance. It has a name and also
 * an associated default value which is used when a DynamicConfig instance
 * does not hold a value for this Setting. <p> 
 * 
 * The Setting abstraction is parameterized with the type of the given setting.
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface Setting<T> {
	
	/**
	 * The name of this Setting. Used when reading this Setting from a DynamicConfig instance. <p>
	 *  
	 * @return
	 */
	String name();
	
	/**
	 * Reads this Setting from the given {@link DynamicConfig} instance.
	 * If the DynamicConfig instance does not hold a value for this
	 * Setting, then the default value will be returned.
	 * 
	 * @param config DynamicConfig instance to read this setting from
	 * @return This value of this setting in the given {@link DynamicConfig} instance, 
	 * 		   or the default value if the DynamicConfig instance does not contain a 
	 * 		   value for this Setting
	 */
	DynamicProperty<T> getFrom(DynamicConfig config);
	
	/**
	 * The default value for this setting.
	 * 
	 * For settings of primitive types this method
	 * returns the boxed version of the given type,
	 * and never returns null.
	 * @return
	 */
	T defaultValue();
	
}
