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
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.library.AstrixExport;
import com.avanza.astrix.provider.versioning.AstrixVersioned;
import com.avanza.astrix.provider.versioning.NonVersioned;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

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
		Set<Class<?>> providedBeans = new HashSet<>(getProvidedBeans(descriptorHolder));
		for (AstrixFactoryBeanPlugin<?> factory : result) {
			providedBeans.remove(factory.getBeanType());
		}
		if (!providedBeans.isEmpty()) {
			throw new IllegalAstrixApiProviderException("Not all elements defined in api provided by " + descriptorHolder + ". Missing provider for " + providedBeans);
		}
		return result;
	}

	private List<AstrixFactoryBeanPlugin<?>> getFactoryBeans(AstrixApiDescriptor descriptor) {
		Object libraryProviderInstance = initInstanceProvider(descriptor);
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Method m : descriptor.getDescriptorClass().getMethods()) {
			if (m.isAnnotationPresent(AstrixExport.class)) {
				result.add(new AstrixLibraryFactory<>(libraryProviderInstance, m));
			}
		}
		Class<?>[] providedApis = descriptor.getAnnotation(AstrixApiProvider.class).value();
		for (Class<?> providedApi : providedApis) {
			for (Method m : providedApi.getMethods()) {
				if (m.isAnnotationPresent(Service.class)) {
					ServiceVersioningContext versioningContext = createVersioningContext(descriptor, providedApi);
					AstrixServiceLookup serviceLookup = serviceLookupFactory.createServiceLookup(m);
					result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, m.getReturnType()));
				}
			}
		}
		return result;
	}
	
	private ServiceVersioningContext createVersioningContext(AstrixApiDescriptor descriptor, Class<?> providedApi) {
		if (providedApi.isAnnotationPresent(NonVersioned.class)) {
			return ServiceVersioningContext.nonVersioned();
		}
		if (descriptor.isAnnotationPresent(AstrixVersioned.class)) {
			return ServiceVersioningContext.versionedService(descriptor.getAnnotation(AstrixVersioned.class));
		} 
		return ServiceVersioningContext.nonVersioned();
	}

	@Override
	public List<Class<?>> getProvidedBeans(AstrixApiDescriptor descriptor) {
		Class<?>[] providedApis = descriptor.getAnnotation(AstrixApiProvider.class).value();
		List<Class<?>> result = new ArrayList<>();
		for (Class<?> providedApi : providedApis) {
			for (Method m : providedApi.getMethods()) {
				if (m.isAnnotationPresent(Library.class) || m.isAnnotationPresent(Service.class)) {
					result.add(m.getReturnType());
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
	
	@Override
	public boolean hasStatefulBeans() {
		return false;
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