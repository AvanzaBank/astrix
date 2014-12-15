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
package com.avanza.astrix.context;

import java.util.List;

import com.avanza.astrix.provider.versioning.ServiceVersioningContext;


/**
 * Used on the client side to bind to service exported over the service-registry. <p>
 * 
 * Used on the server side to export a services using a given mechanism. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixServiceComponent {
	
	<T> T createService(ServiceVersioningContext versioningContext, Class<T> type, AstrixServiceProperties serviceProperties);
	
	<T> AstrixServiceProperties createServiceProperties(String serviceUri);
	
	<T> AstrixServiceProperties createServiceProperties(Class<T> exportedService);
	
	/**
	 * The name of this component.
	 * 
	 * @return
	 */
	String getName();

	<T> void exportService(Class<T> providedApi, T provider, ServiceVersioningContext versioningContext);
	
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

	List<AstrixExportedServiceInfo> getImplicitExportedServices();

	
}
