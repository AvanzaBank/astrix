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



/**
 * Used on the client side to bind to service exported over the service-registry. <p>
 * 
 * Used on the server side to export a services using a given mechanism. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface ServiceComponent {
	/**
	 * Creates an bound service-instance for a given service definition.
	 * 
	 * @param serviceDefinition
	 * @param serviceProperties
	 * @return
	 */
	<T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties);
	
	/**
	 * Parses a given servicePropertiesUri.
	 * 
	 * @param serviceProviderUri
	 * @return
	 */
	ServiceProperties parseServiceProviderUri(String serviceProviderUri);
	
	/**
	 * Creates {@link ServiceProperties} for a given exportedService. 
	 * 
	 * Before this method is inoked, the {@link #exportService(Class, Object, ServiceDefinition)} method
	 * will be invoked with the same exported {@link ServiceDefinition}
	 * 
	 * @param exportedServiceDefinition {@link ServiceDefinition} for the service to create {@link ServiceProperties} for.
	 * @return
	 */
	<T> ServiceProperties createServiceProperties(ServiceDefinition<T> exportedServiceDefinition);
	
	/**
	 * The name of this component.
	 * 
	 * @return
	 */
	String getName();
	
	/**
	 * Defines whether this ServiceComponent can be used to bind a bean of a given type.
	 * 
	 * If this ServiceComponent can be used to bind the given type, then {@link #parseServiceProviderUri(Class)}
	 * should create ServiceProperties that can be passed to {@link #bind(Class, ServiceDefinition, ServiceProperties)}
	 * to bind to an instance of the given type.
	 * 
	 * @param type
	 * @return
	 */
	boolean canBindType(Class<?> type);

	/**
	 * Exports a given service on the server side. After a service is exported it should be possible to
	 * call {@link #createServiceProperties(ServiceDefinition)} to receive ServiceProperties for the
	 * ServiceDefinition, and later to call {@link #bind(ServiceDefinition, ServiceProperties)} to
	 * get a proxy that can invoke the passied in provider.
	 * 
	 * @param providedApi
	 * @param provider - The target provider instance
	 * @param serviceDefinition - {@link ServiceDefinition} for service to be exported
	 */
	<T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition);
	
	/**
	 * Whether the api supports an async version based on the following naming
	 * convention:
	 *  
	 * <pre>
	 * MyService
	 * 	MyResult mySyncMethod(Argument)
	 * 
	 * MyServiceAsync
	 * 	Future<MyResult> mySyncMethod(Argument)
	 * </pre>
	 * 
	 * @return
	 */
	boolean supportsAsyncApis();
	
	/**
	 * Defines whether an instance implementing a provided api is required when invoking ServiceComponent.exportService.
	 * 
	 * If true, Astrix will identify an instance that implements the given api and pass it to the exportService method and
	 * never pass null. If this property is false then null will be passed to exportService.
	 * 
	 * @return
	 */
	boolean requiresProviderInstance();

}
