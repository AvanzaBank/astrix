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
package com.avanza.astrix.beans.service;

public final class ServiceDefinitionSource {
	
	// TODO: Is this a good abstraction? This class was introduces to break cyclic dependency between beans.service and beans.publish package
	// 
	private final String name;
	
	private ServiceDefinitionSource(String name) {
		this.name = name;
	}

	public String getName() {
		return name;
	}

	public static ServiceDefinitionSource create(String name) {
		return new ServiceDefinitionSource(name);
	}

}
