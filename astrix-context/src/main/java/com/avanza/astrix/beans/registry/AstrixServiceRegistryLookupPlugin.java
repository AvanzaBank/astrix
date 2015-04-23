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
package com.avanza.astrix.beans.registry;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.publish.AstrixPublishedBeansAware;
import com.avanza.astrix.beans.service.AstrixServiceLookupMetaFactoryPlugin;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.beans.service.ServiceConsumerProperties;
import com.avanza.astrix.beans.service.ServiceLookup;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixServiceLookupMetaFactoryPlugin.class)
public class AstrixServiceRegistryLookupPlugin implements AstrixServiceLookupMetaFactoryPlugin<AstrixServiceRegistryLookup>, AstrixPublishedBeansAware {

	private AstrixPublishedBeans beans;

	@Override
	public Class<AstrixServiceRegistryLookup> getLookupAnnotationType() {
		return AstrixServiceRegistryLookup.class;
	}

	@Override
	public void setAstrixBeans(AstrixPublishedBeans beans) {
		this.beans = beans;
	}

	@Override
	public ServiceLookup create(AstrixBeanKey<?> key, AstrixServiceRegistryLookup lookupAnnotation) {
		return new ServiceRegistryLookup(key, beans);
	}
	
	private static class ServiceRegistryLookup implements ServiceLookup {
	
		/*
		 * IMPLEMENTATION NOTE:
		 * 
		 * This class requires an AstrixServiceRegistryClient. Since we can't make sure that
		 * a service-factory for AstrixServiceRegistryClient is registered in the bean factory
		 * (behind the AstrixPublishedBeansAware interface) before an instance of ServiceRegistryLookup
		 * is created, we have to inject the AstrixPublishedBeans here and query it for an instance
		 * on each invocation.
		 */
		private AstrixPublishedBeans beans;
		private AstrixBeanKey<?> beanKey;

		public ServiceRegistryLookup(AstrixBeanKey<?> key,
				AstrixPublishedBeans beans) {
			this.beanKey = key;
			this.beans = beans;
		}
		
		@Override
		public String description() {
			return "ServiceRegistry";
		}

		@Override
		public AstrixServiceProperties lookup() {
			return beans.getBean(AstrixBeanKey.create(AstrixServiceRegistryClient.class, null)).lookup(beanKey);
		}
		
	}

}
