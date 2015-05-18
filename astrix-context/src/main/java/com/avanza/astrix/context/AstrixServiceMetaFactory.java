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

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.FactoryBean;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.ServiceComponents;
import com.avanza.astrix.beans.service.ServiceFactory;
import com.avanza.astrix.beans.service.ServiceLeaseManager;
import com.avanza.astrix.beans.service.ServiceContext;
import com.avanza.astrix.beans.service.ServiceLookupFactory;
import com.avanza.astrix.config.DynamicConfig;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixServiceMetaFactory implements AstrixConfigAware {

	private ServiceComponents serviceComponents;
	private ServiceLeaseManager leaseManager;
	private DynamicConfig config;

	public <T> FactoryBean<T> createServiceFactory(ServiceContext versioningContext, ServiceLookupFactory<?> serviceLookup, AstrixBeanKey<T> beanKey, AstrixPublishedBeanDefinitionMethod beanDefinition) {
		if (beanDefinition.isDynamicQualified()) {
			return ServiceFactory.dynamic(versioningContext, beanKey.getBeanType(), serviceLookup, serviceComponents, leaseManager, config);
		}
		return ServiceFactory.standard(versioningContext, beanKey, serviceLookup, serviceComponents, leaseManager, config);
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

	@AstrixInject
	public void setServiceComponents(ServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setLeaseManager(ServiceLeaseManager leaseManager) {
		this.leaseManager = leaseManager;
	}
	
	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}


}
