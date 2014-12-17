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
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.provider.core.AstrixServiceProvider;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
import com.avanza.astrix.provider.versioning.AstrixVersioned;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixApiProviderPlugin.class)
public class AstrixServiceProviderPlugin implements AstrixApiProviderPlugin {
	
	private AstrixServiceLookupFactory serviceLookupFactory;
	private AstrixServiceMetaFactory serviceMetaFactory;

	@Override
	public List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptor) {
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		ServiceVersioningContext versioningContext = createVersioningContext(descriptor, null);
		for (AstrixServiceBeanDefinition serviceBeanDefinition : getProvidedBeans(descriptor)) {
			AstrixServiceLookup serviceLookup = getLookupStrategy(descriptor);
			Class<?> beanType = serviceBeanDefinition.getBeanKey().getBeanType();
			result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, AstrixBeanKey.create(beanType, null)));
			Class<?> asyncInterface = serviceMetaFactory.loadInterfaceIfExists(beanType.getName() + "Async");
			if (asyncInterface != null) {
				result.add(serviceMetaFactory.createServiceFactory(versioningContext, serviceLookup, AstrixBeanKey.create(asyncInterface, null)));
			}
		}
		return result;
	}

	@Override
	public ServiceVersioningContext createVersioningContext(AstrixApiDescriptor descriptor, Class<?> api) {
		if (descriptor.isAnnotationPresent(AstrixVersioned.class)) {
			return ServiceVersioningContext.versionedService(descriptor.getAnnotation(AstrixVersioned.class));
		}
		return ServiceVersioningContext.nonVersioned();
	}

	private AstrixServiceLookup getLookupStrategy(AstrixApiDescriptor descriptor) {
		return serviceLookupFactory.createServiceLookup(descriptor.getDescriptorClass());
	}
	
	private boolean usesServiceRegistry(AstrixApiDescriptor descriptor) { 
		return serviceLookupFactory.getLookupStrategy(descriptor.getDescriptorClass()).equals(AstrixServiceRegistryLookup.class);
	}

	private List<AstrixServiceBeanDefinition> getProvidedBeans(AstrixApiDescriptor descriptor) {
		return getProvidedServices(descriptor);
	}
	
	@Override
	public List<AstrixServiceBeanDefinition> getProvidedServices(AstrixApiDescriptor descriptor) {
		List<AstrixServiceBeanDefinition> result = new ArrayList<>();
		for (Class<?> providedService : descriptor.getAnnotation(AstrixServiceProvider.class).value()) {
			AstrixBeanKey beanKey = AstrixBeanKey.create(providedService, null);
			boolean usesServiceRegistry = usesServiceRegistry(descriptor);
			result.add(new AstrixServiceBeanDefinition(beanKey, descriptor, this, usesServiceRegistry, null));
		}
		return result;
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixServiceProvider.class;
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
