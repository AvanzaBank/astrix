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
package com.avanza.astrix.serviceunit;

import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.UnsupportedTargetTypeException;

class ServiceRegistryExportedService {
	
	private final Class<?> asyncService;
	private final ServiceComponent serviceComponent;
	private volatile boolean publishServices;
	private final ExportedServiceBeanDefinition serviceBeanDefinition;
	
	public ServiceRegistryExportedService(ServiceComponent serviceComponent, ExportedServiceBeanDefinition serviceBeanDefinition, boolean publishServices) {
		this.serviceBeanDefinition = serviceBeanDefinition;
		this.publishServices = publishServices;
		if (!serviceComponent.canBindType(serviceBeanDefinition.getBeanType())) {
			throw new UnsupportedTargetTypeException(serviceComponent.getName(), serviceBeanDefinition.getBeanType());
		}
		this.serviceComponent = serviceComponent;
		if (serviceComponent.supportsAsyncApis()) {
			this.asyncService = loadInterfaceIfExists(serviceBeanDefinition.getBeanType().getName() + "Async");
		} else {
			this.asyncService = null;
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

	public ServiceProperties exportServiceProperties() {
		ServiceProperties serviceProperties = serviceComponent.createServiceProperties(serviceBeanDefinition.getServiceDefinition());
		serviceProperties.getProperties().put(ServiceProperties.PUBLISHED, Boolean.toString(isPublished()));
		serviceProperties.setApi(serviceBeanDefinition.getBeanKey().getBeanType());
		serviceProperties.setQualifier(serviceBeanDefinition.getBeanKey().getQualifier());
		serviceProperties.setComponent(serviceComponent.getName());
		return serviceProperties;
	}

	private boolean isPublished() {
		if (serviceBeanDefinition.isAlwaysActive()) {
			return true;
		}
		if (!publishServices) {
			return false;
		}
		return serviceBeanDefinition.getServiceDefinition().isVersioned();
	}
	
	public void setPublishServices(boolean published) {
		this.publishServices = published;
	}

	public ServiceProperties exportAsyncServiceProperties() {
		ServiceProperties serviceProperties = exportServiceProperties();
		serviceProperties.setApi(asyncService);
		return serviceProperties;
	}
	
}
