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
package com.avanza.astrix.beans.registry;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.publish.AstrixPublishedBeansAware;
import com.avanza.astrix.beans.service.ServiceDiscovery;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactoryPlugin;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.provider.core.AstrixServiceRegistryDiscovery;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceRegistryDiscoveryPluginImpl implements ServiceDiscoveryMetaFactoryPlugin<AstrixServiceRegistryDiscovery>, AstrixPublishedBeansAware {

	private AstrixPublishedBeans beans;
	
//	public ServiceRegistryDiscoveryPluginImpl(AstrixPublishedBeans beans) {
//		this.beans = beans;
//	}

	@Override
	public Class<AstrixServiceRegistryDiscovery> getDiscoveryAnnotationType() {
		return AstrixServiceRegistryDiscovery.class;
	}

	@Override
	public ServiceDiscovery create(AstrixBeanKey<?> key, AstrixServiceRegistryDiscovery lookupAnnotation) {
		return new ServiceRegistryDiscovery(key, beans);
	}
	
	private static class ServiceRegistryDiscovery implements ServiceDiscovery {
	
		/*
		 * IMPLEMENTATION NOTE:
		 * 
		 * This class requires an ServiceRegistryClient. Since we can't make sure that
		 * a service-factory for ServiceRegistryClient is registered in the bean factory
		 * (behind the AstrixPublishedBeansAware interface) before an instance of ServiceRegistryDiscovery
		 * is created, we have to inject the AstrixPublishedBeans here and query it for an instance
		 * on each invocation.
		 */
		private AstrixPublishedBeans beans;
		private AstrixBeanKey<?> beanKey;

		public ServiceRegistryDiscovery(AstrixBeanKey<?> key, AstrixPublishedBeans beans) {
			this.beanKey = key;
			this.beans = beans;
		}
		
		@Override
		public String description() {
			return "ServiceRegistry";
		}

		@Override
		public ServiceProperties run() {
			return beans.getBean(AstrixBeanKey.create(ServiceRegistryClient.class, null)).lookup(beanKey);
		}
		
	}

	@Override
	public void setAstrixBeans(AstrixPublishedBeans beans) {
		this.beans = beans;
	}

}
