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

public class AsterixServicePropertiesBuilderHolder {
	
	private AsterixServicePropertiesBuilder servicePropertiesBuilder;
	private String componentName;
	private Class<?> exportedService;
	private Class<?> asyncService;
	
	public AsterixServicePropertiesBuilderHolder(AsterixServicePropertiesBuilder serviceBuilder, String componentName, Class<?> exportedService) {
		this.servicePropertiesBuilder = serviceBuilder;
		this.componentName = componentName;
		this.exportedService = exportedService;
		if (this.servicePropertiesBuilder.supportsAsyncApis()) {
			this.asyncService = loadInterfaceIfExists(exportedService.getName() + "Async");
		}
	}

	private Class<?> loadInterfaceIfExists(String interfaceName) {
		try {
			Class<?> c = Class.forName(interfaceName);
			if (c.isInterface()) {
				return c;
			}
		} catch (ClassNotFoundException e) {
			// fall through and return null
		}
		return null;
	}
	
	public boolean exportsAsyncApi() {
		return this.asyncService != null;
	}

	public AsterixServiceProperties exportServiceProperties() {
		AsterixServiceProperties serviceProperties = servicePropertiesBuilder.buildServiceProperties(exportedService);
		serviceProperties.setApi(exportedService);
		serviceProperties.setComponent(componentName);
		return serviceProperties;
	}
	
	public AsterixServiceProperties exportAsyncServiceProperties() {
		AsterixServiceProperties serviceProperties = exportServiceProperties();
		serviceProperties.setApi(asyncService);
		return serviceProperties;
	}

}
