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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.publish.AstrixApiProviderClass;
import com.avanza.astrix.beans.service.AstrixServiceLookupFactory;
import com.avanza.astrix.context.AstrixPublishedBeanDefinitionMethod;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
import com.avanza.astrix.provider.versioning.Versioned;

/**
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(ServiceProviderPlugin.class)
public class GenericServiceProviderPlugin implements ServiceProviderPlugin {
	
	private AstrixServiceLookupFactory serviceLookupFactory;

	@Override
	public List<ServiceBeanDefinition> getProvidedServices(AstrixApiProviderClass apiProvider) {
		List<ServiceBeanDefinition> result = new ArrayList<>();
		for (Method astrixBeanDefinitionMethod : apiProvider.getProviderClass().getMethods()) {
			AstrixPublishedBeanDefinitionMethod beanDefinition = AstrixPublishedBeanDefinitionMethod.create(astrixBeanDefinitionMethod);
			if (!beanDefinition.isService()) {
				continue;
			}
			boolean usesServiceRegistry = this.serviceLookupFactory.getLookupStrategy(astrixBeanDefinitionMethod).equals(AstrixServiceRegistryLookup.class);
			ServiceVersioningContext versioningContext = createVersioningContext(apiProvider, beanDefinition);
			result.add(new ServiceBeanDefinition(beanDefinition.getBeanKey(), versioningContext, usesServiceRegistry, beanDefinition.getServiceComponentName()));
		}
		return result;
	}
	

	private ServiceVersioningContext createVersioningContext(AstrixApiProviderClass apiProvider, AstrixPublishedBeanDefinitionMethod serviceDefinition) {
		Class<?> declaringApi = apiProvider.getProviderClass();
		if (!(declaringApi.isAnnotationPresent(Versioned.class) || serviceDefinition.isVersioned())) {
			return ServiceVersioningContext.nonVersioned(serviceDefinition.getServiceConfigClass());
		}
		if (!apiProvider.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + serviceDefinition.getBeanType().getName() + ", provider=" + apiProvider.getName());
		} 
		AstrixObjectSerializerConfig serializerConfig = apiProvider.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceVersioningContext.versionedService(serializerConfig.version(), serializerConfig.objectSerializerConfigurer(), serviceDefinition.getServiceConfigClass());
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixApiProvider.class;
	}
	
	@AstrixInject
	public void setServiceLookupFactory(AstrixServiceLookupFactory serviceLookupFactory) {
		this.serviceLookupFactory = serviceLookupFactory;
	}
	
}