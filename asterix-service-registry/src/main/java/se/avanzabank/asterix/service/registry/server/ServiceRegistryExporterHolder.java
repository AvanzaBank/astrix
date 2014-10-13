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
package se.avanzabank.asterix.service.registry.server;

import java.util.List;

import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixServicePropertiesBuilder;

public class ServiceRegistryExporterHolder {
	
	// TODO: find better name of this class and possibly even AsterixServicePropertiesBuilder
	
	private final AsterixServicePropertiesBuilder exporter;
	private final String componentName;

	public ServiceRegistryExporterHolder(
			AsterixServicePropertiesBuilder exporter,
			String componentName) {
		this.exporter = exporter;
		this.componentName = componentName;
	}

//	@Deprecated
//	public List<AsterixServiceProperties> getProvidedServices() {
//		return exporter.getProvidedServices();
//	}
	
	public AsterixServicePropertiesBuilder getExporter() {
		return exporter;
	}
	
	public String getComponentName() {
		return componentName;
	}

}
