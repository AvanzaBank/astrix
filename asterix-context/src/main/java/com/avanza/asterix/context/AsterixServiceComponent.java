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
package com.avanza.asterix.context;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Used on the client side to bind to service exported over the service-registry. <p>
 * 
 * Used on the server side to export a services using a given mechanism. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixServiceComponent {
	
	// TODO: move info from api-descriptor to AsterixServiceProperties?
	<T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties);
	
	<T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, String serviceProperties);
	
	/**
	 * The name of this component.
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * Used on server side to register all required spring-beans to use this component.
	 * 
	 * @param registry
	 * @param serviceDescriptor 
	 */
	void registerBeans(BeanDefinitionRegistry registry);
	
	/**
	 * Server side component used to make a given provider using this component invokable
	 * from other processes.
	 * @return
	 */
	Class<? extends AsterixServiceExporterBean> getExporterBean();
	
	/**
	 * Server side component used by service-registry to extract service
	 * properties for services published to service-registry.
	 * @return
	 */
	Class<? extends AsterixServicePropertiesBuilder> getServiceBuilder();
	
}
