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
package com.avanza.astrix.serviceunit;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.publish.ApiProvider;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.service.ObjectSerializerDefinition;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactory;
import com.avanza.astrix.context.AstrixBeanDefinitionMethod;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixServiceRegistryDiscovery;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.Versioned;

/**
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(ServiceProviderPlugin.class)
public class GenericServiceProviderPlugin implements ServiceProviderPlugin {
	
	private ServiceDiscoveryMetaFactory serviceLookupFactory;

	@Override
	public List<ExportedServiceBeanDefinition> getExportedServices(ApiProviderClass apiProvider) {
		List<ExportedServiceBeanDefinition> result = new ArrayList<>();
		for (Method astrixBeanDefinitionMethod : apiProvider.getProviderClass().getMethods()) {
			AstrixBeanDefinitionMethod beanDefinition = AstrixBeanDefinitionMethod.create(astrixBeanDefinitionMethod);
			if (!beanDefinition.isService()) {
				continue;
			}
			boolean usesServiceRegistry = this.serviceLookupFactory.getLookupStrategy(astrixBeanDefinitionMethod).equals(AstrixServiceRegistryDiscovery.class);
			ServiceDefinition<?> serviceDefinition = createServiceDefinition(apiProvider, beanDefinition, beanDefinition.getBeanKey());
			result.add(new ExportedServiceBeanDefinition(beanDefinition.getBeanKey(), serviceDefinition, usesServiceRegistry, beanDefinition.getServiceComponentName()));
		}
		return result;
	}
	

	private <T> ServiceDefinition<T> createServiceDefinition(ApiProviderClass apiProvider, AstrixBeanDefinitionMethod serviceDefinitionMethod, AstrixBeanKey<T> beanKey) {
		Class<?> declaringApi = apiProvider.getProviderClass();
		if (!(declaringApi.isAnnotationPresent(Versioned.class) || serviceDefinitionMethod.isVersioned())) {
			return ServiceDefinition.create(ApiProvider.create(apiProvider.getName()), 
											beanKey, 
											serviceDefinitionMethod.getServiceConfigClass(), 
											ObjectSerializerDefinition.nonVersioned(), 
											serviceDefinitionMethod.isDynamicQualified());
		}
		if (!apiProvider.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + serviceDefinitionMethod.getBeanType().getName() + ", provider=" + apiProvider.getProviderClassName());
		} 
		AstrixObjectSerializerConfig serializerConfig = apiProvider.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceDefinition.create(ApiProvider.create(apiProvider.getName()),
									  beanKey, 
									  serviceDefinitionMethod.getServiceConfigClass(), 
									  ObjectSerializerDefinition.versionedService(serializerConfig.version(), serializerConfig.objectSerializerConfigurer()),
									  serviceDefinitionMethod.isDynamicQualified());
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixApiProvider.class;
	}
	
	@AstrixInject
	public void setServiceLookupFactory(ServiceDiscoveryMetaFactory serviceLookupFactory) {
		this.serviceLookupFactory = serviceLookupFactory;
	}
	
}