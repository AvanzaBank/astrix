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

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;


class ServiceComponents implements ServiceComponentRegistry {

	private final Logger log = org.slf4j.LoggerFactory.getLogger(ServiceComponents.class);
	private final Map<String, ServiceComponent> componentsByName = new ConcurrentHashMap<>();
	
	public ServiceComponents(List<ServiceComponent> serviceComponents) {
		log.debug("Creating ServiceComponentRegistry with " + serviceComponents.size() + " service components");
		for (ServiceComponent serviceComponent : serviceComponents) {
			log.debug("Registering service component: " + serviceComponent.getName());
			componentsByName.put(serviceComponent.getName(), serviceComponent);
		}
	}
	
	@Override
	public ServiceComponent getComponent(String name) {
		ServiceComponent serviceComponent = componentsByName.get(name);
		if (serviceComponent == null) {
			throw new MissingServiceComponentException(String.format("ServiceComponent not found: name=%s. Did you forget to put the jar containing the given ServiceComponent on the classpath?", name));
		}
		return serviceComponent;
	}
	
	public Collection<ServiceComponent> getAll() {
		return this.componentsByName.values();
	}


}
