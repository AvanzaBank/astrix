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

import java.util.List;


/**
 * This is the service-provider part of the service registry.
 * 
 * It's used on the server side to publish a set of services
 * provided to the service registry. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixServiceBuilder {
	
	// TODO: rename this abstraction 
	
	// TODO: remove this method
	@Deprecated
	List<AsterixServiceProperties> getProvidedServices();

	/**
	 * Server side component used by service-registry to export AsterixServiceProperties
	 * for a given service. <p>
	 *  
	 * @param type
	 * @return
	 */
	AsterixServiceProperties exportServiceProperties(Class<?> type);
	
	boolean supportsAsyncApis();
	
}
