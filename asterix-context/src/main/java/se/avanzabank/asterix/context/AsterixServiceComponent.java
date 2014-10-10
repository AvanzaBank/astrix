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

import java.lang.annotation.Annotation;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * Used on the server side to export a services using a given mechanism
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AsterixServiceComponent {
	
	// TODO: move info from api-descriptor to AsterixServiceProperties?
	<T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties);
	
	/**
	 * Client side component used to extract service-properties for a given asterix-bean.
	 * 
	 * The service-properties will be used by a AsterixFactoryBean to bind to a given service
	 */
	@Deprecated // TODO: remove
	<T> AsterixServiceProperties getServiceProperties(AsterixApiDescriptor apiDescriptor, Class<T> type);
	
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
	 * Server side component used to make a given provider instance invokable
	 * using this component.
	 * @return
	 */
	Class<? extends AsterixServiceExporterBean> getExporterBean();
	
	/**
	 * Server side component used by service-registry to extract service
	 * properties for services published to service-registry.
	 * @return
	 */
	Class<? extends AsterixServiceBuilder> getServiceBuilder();
	

	/**
	 * For service-components that might be used standalone (that is: whithout the serviceregistry).
	 * 
	 * This method defines a unique annotation used to describe the given api.
	 * 

	 * @return
	 */
	Class<? extends Annotation> getApiDescriptorType();
	
}
