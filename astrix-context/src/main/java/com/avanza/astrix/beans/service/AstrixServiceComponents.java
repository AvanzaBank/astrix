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
			throw new MissingAstrixServiceComponentException(String.format("AstrixServiceComponent not found: name=%s. Did you forget to put the jar containing the given AstrixServiceComponent on the classpath?", name));
		}
		return serviceComponent;
	}
	
	public <T extends AstrixServiceComponent> T getComponent(Class<T> componentType) {
		for (AstrixServiceComponent component : componentsByName.values()) {
			if (component.getClass().equals(componentType)) {
				return componentType.cast(component);
			}
		}
		throw new MissingAstrixServiceComponentException(String.format("AstrixServiceComponent instance not found: type=%s. Did you forget to put the jar containing the given AstrixServiceComponent on the classpath?", componentType));
	}
	
	public Collection<AstrixServiceComponent> getAll() {
		return this.componentsByName.values();
	}


}
