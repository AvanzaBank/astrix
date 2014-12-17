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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.library.AstrixExport;
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
	
	private ObjectCache instanceCache;
	private AstrixServiceMetaFactory serviceMetaFactory;
	private AstrixServiceLookupFactory serviceLookupFactory;
	
	@Override
	public List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptorHolder) {
		List<AstrixFactoryBeanPlugin<?>> result = getFactoryBeans(descriptorHolder);
		Set<AstrixBeanKey<?>> providedBeans = new HashSet<>(getProvidedBeans(descriptorHolder));
		for (AstrixFactoryBeanPlugin<?> factory : result) {
			providedBeans.remove(factory.getBeanKey());
		}
		if (!providedBeans.isEmpty()) {
			throw new IllegalAstrixApiProviderException("Not all elements defined in api provided by " + descriptorHolder + ". Missing provider for " + providedBeans);
		}
		return result;
	}

	private List<AstrixFactoryBeanPlugin<?>> getFactoryBeans(AstrixApiDescriptor descriptor) {
		Object libraryProviderInstance = initInstanceProvider(descriptor);
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		// Create library factories
		for (Method possibleLibraryFactory : descriptor.getDescriptorClass().getMethods()) {
			if (possibleLibraryFactory.isAnnotationPresent(AstrixExport.class)) {
				String qualifier = null;
				if (possibleLibraryFactory.isAnnotationPresent(AstrixQualifier.class)) {
					qualifier = possibleLibraryFactory.getAnnotation(AstrixQualifier.class).value();
				}
				result.add(new AstrixLibraryFactory<>(libraryProviderInstance, possibleLibraryFactory, qualifier));
			}
		}
		// Create service factories
		Class<?>[] providedApis = descriptor.getAnnotation(AstrixApiProvider.class).value();
		for (Class<?> providedApi : providedApis) {
			for (Method beanDefinition : providedApi.getMethods()) {
				if (beanDefinition.isAnnotationPresent(Service.class)) {
					Class<?> providedService = beanDefinition.getReturnType();
					ServiceVersioningContext versioningContext = createVersioningContext(descriptor, providedService);
					AstrixServiceLookup serviceLookup = serviceLookupFactory.createServiceLookup(beanDefinition);
					String qualifier = null;
					if (beanDefinition.isAnnotationPresent(AstrixQualifier.class)) {
						qualifier = beanDefinition.getAnnotation(AstrixQualifier.class).value();
					}
					result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, AstrixBeanKey.create(providedService, qualifier)));
					Class<?> asyncInterface = serviceMetaFactory.loadInterfaceIfExists(providedService.getName() + "Async");
					if (asyncInterface != null) {
						result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, AstrixBeanKey.create(asyncInterface, qualifier)));
					}
				}
			}
		}
		return result;
	}
	
	@Override
	public ServiceVersioningContext createVersioningContext(AstrixApiDescriptor descriptor, Class<?> providedService) {
		if (!getDeclaringApi(descriptor, providedService).isAnnotationPresent(Versioned.class)) {
			return ServiceVersioningContext.nonVersioned();
		}
		if (!descriptor.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + providedService.getName() + ", descriptor=" + descriptor.getName());
		} 
		AstrixObjectSerializerConfig serializerConfig = descriptor.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceVersioningContext.versionedService(serializerConfig.version(), serializerConfig.objectSerializerConfigurer());
	}

	private Class<?> getDeclaringApi(AstrixApiDescriptor descriptor, Class<?> providedService) {
		for (Class<?> api : descriptor.getAnnotation(AstrixApiProvider.class).value()) {
			for (Method m : api.getMethods()) {
				if (m.isAnnotationPresent(Service.class) && m.getReturnType().equals(providedService)) {
					return api;
				}
			}
		}
		throw new IllegalArgumentException(String.format("Descriptor does not provide service. descriptor=%s service=%s", descriptor, providedService.getName()));
	}

	private List<AstrixBeanKey<?>> getProvidedBeans(AstrixApiDescriptor descriptor) {
		Class<?>[] providedApis = descriptor.getAnnotation(AstrixApiProvider.class).value();
		List<AstrixBeanKey<?>> result = new ArrayList<>();
		for (Class<?> providedApi : providedApis) {
			for (Method beanDefinition : providedApi.getMethods()) {
				if (beanDefinition.isAnnotationPresent(Library.class) || beanDefinition.isAnnotationPresent(Service.class)) {
					String qualifier = null;
					if (beanDefinition.isAnnotationPresent(AstrixQualifier.class)) {
						qualifier = beanDefinition.getAnnotation(AstrixQualifier.class).value();
					}
					result.add(AstrixBeanKey.create(beanDefinition.getReturnType(), qualifier));
				}
			}
		}
		return result;
	}

	@Override
	public List<AstrixServiceBeanDefinition> getProvidedServices(AstrixApiDescriptor descriptor) {
		Class<?>[] apiDefinitions = descriptor.getAnnotation(AstrixApiProvider.class).value();
		List<AstrixServiceBeanDefinition> result = new ArrayList<>();
		for (Class<?> apiDefinition : apiDefinitions) {
			for (Method declaredBean : apiDefinition.getMethods()) {
				if (declaredBean.isAnnotationPresent(Service.class)) {
					String qualifier = null;
					if (declaredBean.isAnnotationPresent(AstrixQualifier.class)) {
						qualifier = declaredBean.getAnnotation(AstrixQualifier.class).value();
					}
					
					String serviceComponentName = null;
					if (!declaredBean.getAnnotation(Service.class).value().isEmpty()) {
						serviceComponentName = declaredBean.getAnnotation(Service.class).value();
					}
					AstrixBeanKey<?> serviceBeanKey = AstrixBeanKey.create(declaredBean.getReturnType(), qualifier);
					boolean usesServiceRegistry = this.serviceLookupFactory.getLookupStrategy(declaredBean).equals(AstrixServiceRegistryLookup.class);
					result.add(new AstrixServiceBeanDefinition(serviceBeanKey, descriptor, this, usesServiceRegistry, serviceComponentName));
				}
			}
		}
		return result;
	}

	private Object initInstanceProvider(AstrixApiDescriptor descriptor) {
		return instanceCache.getInstance(ObjectId.internalClass(descriptor.getDescriptorClass()));
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixApiProvider.class;
	}
	
	@AstrixInject
	public void setInstanceCache(ObjectCache instanceCache) {
		this.instanceCache = instanceCache;
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