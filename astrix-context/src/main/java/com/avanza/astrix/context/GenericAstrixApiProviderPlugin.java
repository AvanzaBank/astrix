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

import com.avanza.astrix.beans.core.AstrixApiDescriptor;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixFactoryBean;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.service.AstrixServiceLookup;
import com.avanza.astrix.beans.service.AstrixServiceLookupFactory;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
import com.avanza.astrix.provider.core.Library;
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
public class GenericAstrixApiProviderPlugin  implements AstrixApiProviderPlugin {
	
//	private AstrixContextImpl astrixContext;
	private AstrixInjector injector;
	private AstrixServiceMetaFactory serviceMetaFactory;
	private AstrixServiceLookupFactory serviceLookupFactory;
	
	@Override
	public List<AstrixFactoryBean<?>> createFactoryBeans(AstrixApiDescriptor descriptor) {
		return getFactoryBeans(descriptor);
	}

	private List<AstrixFactoryBean<?>> getFactoryBeans(AstrixApiDescriptor descriptor) {
		List<AstrixFactoryBean<?>> result = new ArrayList<>();
		// Create library factories
		for (Method astrixBeanDefinitionMethod : descriptor.getDescriptorClass().getMethods()) {
			AstrixBeanDefinition beanDefinition = AstrixBeanDefinition.create(astrixBeanDefinitionMethod);
			if (!beanDefinition.isLibrary()) {
				continue;
			}
			Object libraryProviderInstance = getInstanceProvider(descriptor.getDescriptorClass());
			result.add(new AstrixLibraryFactory<>(libraryProviderInstance, astrixBeanDefinitionMethod, beanDefinition.getQualifier()));
		}
		// Create service factories
		for (Method astrixBeanDefinitionMethod : descriptor.getDescriptorClass().getMethods()) {
			AstrixBeanDefinition beanDefinition = AstrixBeanDefinition.create(astrixBeanDefinitionMethod);
			if (!beanDefinition.isService()) {
				continue;
			}
			ServiceVersioningContext versioningContext = createVersioningContext(descriptor, beanDefinition);
			AstrixServiceLookup serviceLookup = serviceLookupFactory.createServiceLookup(astrixBeanDefinitionMethod);
			result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, beanDefinition.getBeanKey()));
			Class<?> asyncInterface = serviceMetaFactory.loadInterfaceIfExists(beanDefinition.getBeanType().getName() + "Async");
			if (asyncInterface != null) {
				result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, AstrixBeanKey.create(asyncInterface, beanDefinition.getQualifier())));
			}
		}
		return result;
	}
	
	private ServiceVersioningContext createVersioningContext(AstrixApiDescriptor descriptor, AstrixBeanDefinition serviceDefinition) {
		Class<?> declaringApi = getDeclaringApi(descriptor, serviceDefinition.getBeanType());
		if (!(declaringApi.isAnnotationPresent(Versioned.class) || serviceDefinition.isVersioned())) {
			return ServiceVersioningContext.nonVersioned();
		}
		if (!descriptor.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + serviceDefinition.getBeanType().getName() + ", provider=" + descriptor.getName());
		} 
		AstrixObjectSerializerConfig serializerConfig = descriptor.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceVersioningContext.versionedService(serializerConfig.version(), serializerConfig.objectSerializerConfigurer());
	}

	private Class<?> getDeclaringApi(AstrixApiDescriptor descriptor, Class<?> providedService) {
		for (Method m : descriptor.getDescriptorClass().getMethods()) {
			if (m.isAnnotationPresent(Service.class) && m.getReturnType().equals(providedService)) {
				return descriptor.getDescriptorClass();
			}
		}
		throw new IllegalArgumentException(String.format("Descriptor does not provide service. descriptor=%s service=%s", descriptor, providedService.getName()));
	}

	@Override
	public List<AstrixServiceBeanDefinition> getProvidedServices(AstrixApiDescriptor descriptor) {
		List<AstrixServiceBeanDefinition> result = new ArrayList<>();
		for (Method astrixBeanDefinitionMethod : descriptor.getDescriptorClass().getMethods()) {
			AstrixBeanDefinition beanDefinition = AstrixBeanDefinition.create(astrixBeanDefinitionMethod);
			if (!beanDefinition.isService()) {
				continue;
			}
			boolean usesServiceRegistry = this.serviceLookupFactory.getLookupStrategy(astrixBeanDefinitionMethod).equals(AstrixServiceRegistryLookup.class);
			ServiceVersioningContext versioningContext = createVersioningContext(descriptor, beanDefinition);
			result.add(new AstrixServiceBeanDefinition(beanDefinition.getBeanKey(), versioningContext, usesServiceRegistry, beanDefinition.getServiceComponentName()));
		}
		return result;
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
	
	static class AstrixBeanDefinition {
		private Method method;
		
		private AstrixBeanDefinition(Method method) {
			this.method = method;
		}

		public String getServiceComponentName() {
			if (!method.getAnnotation(Service.class).value().isEmpty()) {
				return method.getAnnotation(Service.class).value();
			}
			return null;
		}

		public boolean isLibrary() {
			return method.isAnnotationPresent(Library.class);
		}

		public AstrixBeanKey<?> getBeanKey() {
			return AstrixBeanKey.create(getBeanType(), getQualifier());
		}

		public String getQualifier() {
			if (method.isAnnotationPresent(AstrixQualifier.class)) {
				return method.getAnnotation(AstrixQualifier.class).value();
			}
			return null;
		}

		public boolean isService() {
			return method.isAnnotationPresent(Service.class);
		}

		public boolean isVersioned() {
			return method.isAnnotationPresent(Versioned.class);
		}

		public static AstrixBeanDefinition create(Method astrixBeanDefinition) {
			return new AstrixBeanDefinition(astrixBeanDefinition);
		}

		public Class<?> getBeanType() {
			return method.getReturnType();
		}
	}

}