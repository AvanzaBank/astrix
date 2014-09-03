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
package se.avanzabank.asterix.service.registry.client;

import java.util.List;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.service.registry.server.ServiceRegistryExporter;

/**
 * This is the service-consumer part of the service registry.
 * 
 * It's used on the client side to find what services
 * might be looked up on the service registry, as well as
 * binding to those services given AsterixServiceProperties
 * looked up on the service registry. <p>
 * 
 * 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixServiceRegistryComponent {
	
	/**
	 * Check whether this component exports any services on a given descriptor.
	 * 
	 * @param possibleDescriptorHolder
	 * @return
	 */
	List<Class<?>> getExportedServices(AsterixApiDescriptor apiDescriptor);
	
	<T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties);

	/**
	 * Used on the server side to export services to the service-registry. <p>
	 * 
	 * @return
	 */
	List<Class<? extends ServiceRegistryExporter>> getRequiredExporterClasses();

	/**
	 * Checks whether this component is should be activated on the server side by a given descriptor.
	 * 
	 * If a service descriptor is activated, then it's required ServiceRegistryExport's will be added
	 * to the spring-application-context
	 *  
	 * @param descriptor
	 * @return
	 */
	boolean isActivatedBy(AsterixApiDescriptor descriptor);

	String getName();
	
}
