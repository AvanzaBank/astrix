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
package se.avanzabank.asterix.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.kohsuke.MetaInfServices;

@MetaInfServices(AsterixServiceComponents.class)
public class AsterixServiceComponentsImpl implements AsterixServiceComponents, AsterixPluginsAware {

	private final Map<String, AsterixServiceComponent> transportByName = new ConcurrentHashMap<>();
	
	@Override
	public AsterixServiceComponent getComponent(String name) {
		AsterixServiceComponent serviceTransport = transportByName.get(name);
		if (serviceTransport == null) {
			throw new IllegalStateException("No transport found with name: " + name);
		}
		return serviceTransport;
	}

	@Override
	public AsterixServiceComponent getComponent(AsterixApiDescriptor apiDescriptor) {
		// TODO: avoid iterating over AsterixServiceComponents. In order to do so we must be able to lookup up the descriptorType from a given AsterixApiDescriptor.
		for (AsterixServiceComponent serviceTransport : this.transportByName.values()) {
			if (serviceTransport.getApiDescriptorType() == null) {
				continue;
			}
			if (apiDescriptor.isAnnotationPresent(serviceTransport.getApiDescriptorType())) {
				return serviceTransport;
			}
		}
		throw new IllegalStateException("Can't find transport for apiDescriptor: " + apiDescriptor);
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		for (AsterixServiceComponent serviceTransport : plugins.getPlugins(AsterixServiceComponent.class)) {
			transportByName.put(serviceTransport.getName(), serviceTransport);
		}
	}


}
