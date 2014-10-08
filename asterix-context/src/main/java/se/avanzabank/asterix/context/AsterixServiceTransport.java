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

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Used on the server side to export a services using a given mechanism
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixServiceTransport {
	
	/*
	 * På client sidan (För att kunna binda mot api:er (däribland tjänster))
	 * 
	 * Givet ett api (en deskriptor).
	 *  1. Vilka bönor exponeras? (Avgörs idag av AsterixApiProviderPlugin.createFactoryBeans(AsterixApiDescriptor descriptor))
	 *  2. Hur binder man mot dessa bönor? (Avgörs av de factories som returneras av ovanstående metod)
	 *  
	 *  Om tjänsten använder tjänsteregistret:
	 *   - Steg 2 av bindingen görs då av AsterixServiceRegistryComponent
	 *  
	 *  
	 * På server sidan (För att kunna exponera tjänster)
	 * 
	 * 
	 * 
	 */
	
	// TODO: rename to AsterixServiceComponent? or AsterixServiceBuilder? or AsterixServiceBinder?

	// TODO: move info from api-descriptor to AsterixServiceProperties?
	<T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties);
	
	/**
	 * Client side component used to extract service-properties for a given asterix-bean.
	 * 
	 * The service-properties will be used by a AsterixFactoryBean to bind to a given service
	 */
	<T> AsterixServiceProperties getServiceProperties(AsterixApiDescriptor apiDescriptor, Class<T> type);
	
	/**
	 * The name of this transport.
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * Used on server side to register all required spring-beans to use this transport.
	 * 
	 * @param registry
	 * @param serviceDescriptor 
	 */
	void registerBeans(BeanDefinitionRegistry registry);
	
	/**
	 * Server side component used to make a given provider instance invokable
	 * using this transport mechanism.
	 * @return
	 */
	Class<? extends AsterixServiceExporterBean> getExporterBean();
	
	/**
	 * Server side component used by service-registry to extract service
	 * properties for services published to service-registry.
	 * @return
	 */
	Class<? extends AsterixServiceBuilder> getServiceBuilder();
	
}
