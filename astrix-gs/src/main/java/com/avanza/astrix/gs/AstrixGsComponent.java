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
package com.avanza.astrix.gs;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import com.avanza.astrix.context.AstrixApiDescriptor;
import com.avanza.astrix.context.AstrixServiceComponent;
import com.avanza.astrix.context.AstrixServiceExporterBean;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixServicePropertiesBuilder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;

@MetaInfServices(AstrixServiceComponent.class)
public class AstrixGsComponent implements AstrixServiceComponent {
	
	@Override
	public <T> T createService(AstrixApiDescriptor apiDescriptor, Class<T> type, AstrixServiceProperties serviceProperties) {
		if (!GigaSpace.class.isAssignableFrom(type)) {
			throw new IllegalStateException("Programming error, attempted to create: " + type);
		}
		return type.cast(GsBinder.createGsFactory(serviceProperties).create()); // TODO: fault tolerance
	}
	
	@Override
	public <T> T createService(AstrixApiDescriptor apiDescriptor, Class<T> type, String serviceUrl) {
		return createService(apiDescriptor, type, GsBinder.createServiceProperties(serviceUrl));
	}


	@Override
	public String getName() {
		return AstrixServiceComponentNames.GS;
	}
	
	@Override
	public void registerBeans(BeanDefinitionRegistry registry) {
		// Does not require any spring-beans
	}

	@Override
	public Class<? extends AstrixServiceExporterBean> getExporterBean() {
		return null;
	}

	@Override
	public Class<? extends AstrixServicePropertiesBuilder> getServiceBuilder() {
		return GigaSpaceServiceRegistryExporter.class;
	}
	
}
