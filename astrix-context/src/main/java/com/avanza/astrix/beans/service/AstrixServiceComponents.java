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
package com.avanza.astrix.beans.service;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class AstrixServiceComponents {

	private final Map<String, AstrixServiceComponent> componentsByName = new ConcurrentHashMap<>();
	
	public AstrixServiceComponents(List<AstrixServiceComponent> serviceComponents) {
		for (AstrixServiceComponent serviceComponent : serviceComponents) {
			componentsByName.put(serviceComponent.getName(), serviceComponent);
		}
	}
	
	public AstrixServiceComponent getComponent(String name) {
		AstrixServiceComponent serviceComponent = componentsByName.get(name);
		if (serviceComponent == null) {
			throw new IllegalStateException("No AstrixServiceComponent found with name: " + name);
		}
		return serviceComponent;
	}
	
	public Collection<AstrixServiceComponent> getAll() {
		return this.componentsByName.values();
	}


}
