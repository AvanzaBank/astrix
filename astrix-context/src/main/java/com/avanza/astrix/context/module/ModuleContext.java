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
package com.avanza.astrix.context.module;

public interface ModuleContext {
	
	<T> void bind(Class<T> type, Class<? extends T> providerType);
	
	<T> void bind(Class<T> type, T provider);
	
	/**
	 * Exports a given type provided by this module. Exported types are
	 * available to be imported by other modules.
	 * 
	 * @param type
	 */
	void export(Class<?> type);
	
	/**
	 * Imports a given type. Imported types are available for consumption by
	 * internal classes in this module.
	 * 
	 * @param type
	 */
	<T> void importType(Class<T> type);
	
}
