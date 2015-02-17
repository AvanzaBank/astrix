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
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.AstrixBeansAware;
import com.avanza.astrix.beans.service.AstrixServiceLookupPlugin;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixServiceLookupPlugin.class)
public class AstrixServiceRegistryLookupPlugin implements AstrixServiceLookupPlugin<AstrixServiceRegistryLookup>, AstrixBeansAware {

	private AstrixBeans beans;

	@Override
	public AstrixServiceProperties lookup(AstrixBeanKey<?> beanKey, AstrixServiceRegistryLookup lookupAnnotation) {
		AstrixServiceRegistryClient serviceRegistryClient = beans.getBean(AstrixBeanKey.create(AstrixServiceRegistryClient.class, null));
		return serviceRegistryClient.lookup(beanKey);
	}

	@Override
	public Class<AstrixServiceRegistryLookup> getLookupAnnotationType() {
		return AstrixServiceRegistryLookup.class;
	}

	@Override
	public void setAstrixBeans(AstrixBeans beans) {
		this.beans = beans;
	}

}
