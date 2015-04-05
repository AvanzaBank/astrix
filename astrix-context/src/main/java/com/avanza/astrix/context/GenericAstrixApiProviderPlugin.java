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

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.publish.AstrixApiProviderClass;
import com.avanza.astrix.beans.publish.AstrixApiProviderPlugin;
import com.avanza.astrix.beans.service.AstrixServiceLookup;
import com.avanza.astrix.beans.service.AstrixServiceLookupFactory;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
import com.avanza.astrix.provider.versioning.Versioned;

/**
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(AstrixApiProviderPlugin.class)
public class GenericAstrixApiProviderPlugin implements AstrixApiProviderPlugin {
	
	private AstrixInjector injector;
	private AstrixServiceMetaFactory serviceMetaFactory;
	private AstrixServiceLookupFactory serviceLookupFactory;
	
	@Override
	public List<AstrixFactoryBean<?>> createFactoryBeans(AstrixApiProviderClass apiProviderClass) {
		return getFactoryBeans(apiProviderClass);
	}

	private List<AstrixFactoryBean<?>> getFactoryBeans(AstrixApiProviderClass apiProviderClass) {
		List<AstrixFactoryBean<?>> result = new ArrayList<>();
		// Create library factories
		for (Method astrixBeanDefinitionMethod : apiProviderClass.getProviderClass().getMethods()) {
			AstrixPublishedBeanDefinitionMethod beanDefinition = AstrixPublishedBeanDefinitionMethod.create(astrixBeanDefinitionMethod);
			if (beanDefinition.isLibrary()) {
				Object libraryProviderInstance = getInstanceProvider(apiProviderClass.getProviderClass());
				result.add(new AstrixLibraryFactory<>(libraryProviderInstance, astrixBeanDefinitionMethod, beanDefinition.getQualifier()));
				continue;
			}
			if (beanDefinition.isService()) {
				ServiceVersioningContext versioningContext = createVersioningContext(apiProviderClass, beanDefinition);
				AstrixServiceLookup serviceLookup = serviceLookupFactory.createServiceLookup(astrixBeanDefinitionMethod);
				result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, beanDefinition.getBeanKey()));
				Class<?> asyncInterface = serviceMetaFactory.loadInterfaceIfExists(beanDefinition.getBeanType().getName() + "Async");
				if (asyncInterface != null) {
					result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, AstrixBeanKey.create(asyncInterface, beanDefinition.getQualifier())));
				}
				continue;
			}
		}
		return result;
	}
	
	private ServiceVersioningContext createVersioningContext(AstrixApiProviderClass apiProviderClass, AstrixPublishedBeanDefinitionMethod serviceDefinition) {
		Class<?> declaringApi = getDeclaringApi(apiProviderClass, serviceDefinition.getBeanType());
		if (!(declaringApi.isAnnotationPresent(Versioned.class) || serviceDefinition.isVersioned())) {
			return ServiceVersioningContext.nonVersioned(serviceDefinition.getServiceConfigClass());
		}
		if (!apiProviderClass.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + serviceDefinition.getBeanType().getName() + ", provider=" + apiProviderClass.getName());
		} 
		AstrixObjectSerializerConfig serializerConfig = apiProviderClass.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceVersioningContext.versionedService(serializerConfig.version(), serializerConfig.objectSerializerConfigurer(), serviceDefinition.getServiceConfigClass());
	}

	private Class<?> getDeclaringApi(AstrixApiProviderClass apiProviderClass, Class<?> providedService) {
		for (Method m : apiProviderClass.getProviderClass().getMethods()) {
			if (m.isAnnotationPresent(Service.class) && m.getReturnType().equals(providedService)) {
				return apiProviderClass.getProviderClass();
			}
		}
		throw new IllegalArgumentException(String.format("Descriptor does not provide service. descriptor=%s service=%s", apiProviderClass, providedService.getName()));
	}

	private Object getInstanceProvider(Class<?> provider) {
		return injector.getBean(provider);
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixApiProvider.class;
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixInjector injector) {
		this.injector = injector;
	}
	
	@AstrixInject
	public void setServiceLookupFactory(AstrixServiceLookupFactory serviceLookupFactory) {
		this.serviceLookupFactory = serviceLookupFactory;
	}
	
	@AstrixInject
	public void setServiceMetaFactory(AstrixServiceMetaFactory serviceMetaFactory) {
		this.serviceMetaFactory = serviceMetaFactory;
	}

}