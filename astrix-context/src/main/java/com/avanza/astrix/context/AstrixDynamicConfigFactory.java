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
package com.avanza.astrix.context;

import com.avanza.astrix.config.DynamicConfig;
/**
 * Used by {@link AstrixConfigurer} to create a {@link DynamicConfig} instance with all custom configuration sources
 * that should be used by a given AstrixContext.
 * 
 * A AstrixDynamicConfigFactory must have a zero-argument constructor.
 * 
 * This factory is located by querying all well-known configuration sources for an property named 
 * "com.avanza.astrix.context.AstrixDynamicConfigFactory" which should point to the class-name of
 * the desired AstrixDynamicConfigFactory to be used by the given AstrixContext, see {@link AstrixConfigurer#setConfig(DynamicConfig)}.
 * 
 *
 * 
 * @author "Elias Lindholm"
 *
 */
public interface AstrixDynamicConfigFactory {
	/**
	 * Creates a {@link DynamicConfig} instance with all custom configuration sources.
	 * 
	 * @return
	 */
	DynamicConfig create();
}
