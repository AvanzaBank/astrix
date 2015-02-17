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

import java.lang.reflect.Proxy;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixServiceFactory<T> implements AstrixFactoryBean<T> {
	private final Logger log = LoggerFactory.getLogger(AstrixServiceFactory.class);
	private final AstrixBeanKey<T> beanKey;
	private final AstrixServiceComponents serviceComponents;
	private final AstrixServiceLookup serviceLookup;
	private final AstrixServiceLeaseManager leaseManager;
	private final ServiceVersioningContext versioningContext;
	private DynamicConfig config;

	public AstrixServiceFactory(ServiceVersioningContext versioningContext, 
								AstrixBeanKey<T> beanType, 
								AstrixServiceLookup serviceLookup, 
								AstrixServiceComponents serviceComponents, 
								AstrixServiceLeaseManager leaseManager,
								DynamicConfig config) {
		this.config = config;
		this.versioningContext = Objects.requireNonNull(versioningContext);
		this.beanKey = Objects.requireNonNull(beanType);
		this.serviceLookup = Objects.requireNonNull(serviceLookup);
		this.serviceComponents = Objects.requireNonNull(serviceComponents);
		this.leaseManager = Objects.requireNonNull(leaseManager);
	}

	@Override
	public T create(AstrixBeans beans) {
		AstrixServiceBeanInstance<T> serviceBeanInstance = AstrixServiceBeanInstance.create(versioningContext, beanKey, serviceLookup, serviceComponents, config);
		AstrixServiceProperties serviceProperties = null;
		try {
			serviceProperties = serviceLookup.lookup(beanKey);
			if (serviceProperties == null) {
				log.warn("Did not find service using serviceLookup: bean=%s serviceLookup=%s", beanKey, serviceLookup);
			} else {
				serviceBeanInstance.bind(serviceProperties);
			}
		} catch (Exception e) {
			log.warn("Failed to bind service bean=" + this.beanKey, e);
		}
		leaseManager.startManageLease(serviceBeanInstance, serviceProperties, serviceLookup);
		return beanKey.getBeanType().cast(
				Proxy.newProxyInstance(beanKey.getBeanType().getClassLoader(), 
									   new Class[]{beanKey.getBeanType(), StatefulAstrixBean.class}, 
									   serviceBeanInstance));
	}

	@Override
	public AstrixBeanKey<T> getBeanKey() {
		return beanKey;
	}
	
	public T create(AstrixServiceProperties serviceProperties) {
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("Misssing entry in service-registry beanKey=%s", beanKey));
		}
		AstrixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
		return serviceComponent.bind(versioningContext, beanKey.getBeanType(), serviceProperties);
	}
	
	private AstrixServiceComponent getServiceComponent(AstrixServiceProperties serviceProperties) {
		String componentName = serviceProperties.getComponent();
		if (componentName == null) {
			throw new IllegalArgumentException("Expected a componentName to be set on serviceProperties: " + serviceProperties);
		}
		return serviceComponents.getComponent(componentName);
	}
	
}
