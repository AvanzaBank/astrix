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



/**
 * Responsible for building service-properties for a given AstrixBeanType.<p>
 * 
 * It's used on the server side by the AstrixServiceRegistryExporterWorker to build
 * service-properties for each type published to the registry.<p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixServicePropertiesBuilder {
	
	/**
	 * Builds AstrixServiceProperties for a given service. <p>
	 *  
	 * @param type
	 * @return
	 */
	AstrixServiceProperties buildServiceProperties(Class<?> type);
	
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
	
}
