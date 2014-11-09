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
package com.avanza.asterix.service.registry.client;

import java.util.Arrays;
import java.util.List;

import com.avanza.asterix.context.AsterixApiDescriptor;
import com.avanza.asterix.context.AsterixBeanAware;
import com.avanza.asterix.context.AsterixBeans;
import com.avanza.asterix.context.AsterixFactoryBeanPlugin;
import com.avanza.asterix.context.AsterixInject;
import com.avanza.asterix.context.AsterixServiceComponent;
import com.avanza.asterix.context.AsterixServiceComponents;
import com.avanza.asterix.context.AsterixServiceProperties;

public class ServiceRegistryLookupFactory<T> implements AsterixFactoryBeanPlugin<T>, AsterixBeanAware {

	private Class<T> api;
	private AsterixApiDescriptor descriptor;
	private AsterixBeans beans;
	private AsterixServiceRegistryLeaseManager leaseManager;
	private AsterixServiceComponents serviceComponents;

	public ServiceRegistryLookupFactory(AsterixApiDescriptor descriptor,
										Class<T> api) {
		this.descriptor = descriptor;
		this.api = api;
	}

	@Override
	public T create(String qualifier) {
		AsterixServiceRegistryClient serviceRegistry = beans.getBean(AsterixServiceRegistryClient.class);
		AsterixServiceProperties serviceProperties = serviceRegistry.lookup(api, qualifier);
		T service = create(qualifier, serviceProperties);
		return leaseManager.startManageLease(service, serviceProperties, qualifier, this);
	}
	
	public T create(String qualifier, AsterixServiceProperties serviceProperties) {
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("Misssing entry in service-registry api=%s qualifier=%s: ", api.getName(), qualifier));
		}
		AsterixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
		return serviceComponent.createService(descriptor, api, serviceProperties);
	}
	
	private AsterixServiceComponent getServiceComponent(AsterixServiceProperties serviceProperties) {
		String componentName = serviceProperties.getComponent();
		if (componentName == null) {
			throw new IllegalArgumentException("Expected a componentName to be set on serviceProperties: " + serviceProperties);
		}
		return serviceComponents.getComponent(componentName);
	}
	
	@Override
	public List<Class<?>> getBeanDependencies() {
		return Arrays.<Class<?>>asList(AsterixServiceRegistryClient.class);
	}

	@Override
	public Class<T> getBeanType() {
		return this.api;
	}

	@Override
	public void setAsterixBeans(AsterixBeans beans) {
		this.beans = beans;
	}
	
	@AsterixInject
	public void setServiceComponents(AsterixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AsterixInject
	public void setServiceComponents(AsterixServiceRegistryLeaseManager leaseManager) {
		this.leaseManager = leaseManager;
	}

}
