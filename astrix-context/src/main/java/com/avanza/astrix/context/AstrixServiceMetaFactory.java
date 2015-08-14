/*
 * Copyright 2014 Avanza Bank AB
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

import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.beans.service.ServiceComponentRegistry;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDiscoveryFactory;
import com.avanza.astrix.beans.service.ServiceFactory;
import com.avanza.astrix.beans.service.ServiceLeaseManager;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixServiceMetaFactory {

	private ServiceComponentRegistry serviceComponents;
	private ServiceLeaseManager leaseManager;
	private BeanFaultToleranceFactory beanFaultToleranceFactory;
	
	public AstrixServiceMetaFactory(ServiceComponentRegistry serviceComponents,
									ServiceLeaseManager leaseManager,
									BeanFaultToleranceFactory beanFaultToleranceFactory) {
		this.serviceComponents = serviceComponents;
		this.leaseManager = leaseManager;
		this.beanFaultToleranceFactory = beanFaultToleranceFactory;
	}

	public <T> FactoryBean<T> createServiceFactory(ServiceDefinition<T> serviceDefinition, ServiceDiscoveryFactory<?> serviceDiscoveryFactory) {
		if (serviceDefinition.isDynamicQualified()) {
			return ServiceFactory.dynamic(serviceDefinition, serviceDiscoveryFactory, serviceComponents, leaseManager, beanFaultToleranceFactory);
		}
		return ServiceFactory.standard(serviceDefinition, serviceDefinition.getBeanKey(), serviceDiscoveryFactory, serviceComponents, leaseManager, beanFaultToleranceFactory);
	}
	
	public Class<?> loadInterfaceIfExists(String interfaceName) {
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

}
