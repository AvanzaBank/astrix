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
package com.avanza.astrix.service.registry.client;

import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixBeanAware;
import com.avanza.astrix.context.AstrixBeanKey;
import com.avanza.astrix.context.AstrixBeans;
import com.avanza.astrix.context.AstrixServiceLookupPlugin;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.DefaultServiceLookup;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@DefaultServiceLookup
@MetaInfServices(AstrixServiceLookupPlugin.class)
public class AstrixServiceRegistryLookupPlugin implements AstrixServiceLookupPlugin<AstrixServiceRegistryLookup>, AstrixBeanAware {

	private AstrixBeans beans;

	@Override
	public AstrixServiceProperties lookup(Class<?> beanType, String optionalQualifier, AstrixServiceRegistryLookup lookupAnnotation) {
		AstrixServiceRegistryClient serviceRegistryClient = beans.getBean(AstrixServiceRegistryClient.class);
		return serviceRegistryClient.lookup(beanType, optionalQualifier);
	}

	@Override
	public Class<AstrixServiceRegistryLookup> getLookupAnnotationType() {
		return AstrixServiceRegistryLookup.class;
	}

	@Override
	public List<AstrixBeanKey> getBeanDependencies() {
		return Arrays.asList(AstrixBeanKey.create(AstrixServiceRegistryClient.class, null));
	}

	@Override
	public void setAstrixBeans(AstrixBeans beans) {
		this.beans = beans;
	}

}
