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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixServiceExporterBean;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixServiceBuilder;
import se.avanzabank.asterix.context.AsterixServiceTransport;

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
	 * Used on the client side to bind to the service using AsterixServiceProperties received
	 * from the service-registry.
	 * 
	 * @param apiDescriptor
	 * @param type
	 * @param serviceProperties
	 * @return
	 */
	<T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties);

	/**
	 * The name of this component.
	 * 
	 *  1. Used by clients to identify the component to use from AsterixServiceProperies received from the service-registry.
	 *  2. Used by the server to identify plugin when registering with service-registry 
	 *   
	 * @return
	 */
	String getName();
	
	/**
	 * Used on the server side to export the service to the service-registry. <p>
	 * 
	 * @return
	 */
	Class<? extends AsterixServiceBuilder> getServiceExporterClass();
	
	/**
	 * Used on the server-side to express if this component requires other component to
	 * export its service to the service-bus
	 * 
	 * @return
	 */
	List<String> getComponentDepenencies();
	

//	/**
//	 * Used on the server-side register beans required by this component. <p>
//	 * 
//	 * @param registry
//	 */
//	void registerBeans(BeanDefinitionRegistry registry);
	
	// Identified by getName() ???
//	String getServiceTransport();
	
}
