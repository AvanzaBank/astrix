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
import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.provider.core.AstrixServiceProvider;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixApiProviderPlugin.class)
public class AstrixServiceProviderPlugin implements AstrixApiProviderPlugin {
	
	private AstrixServiceComponents serviceComponents;
	private AstrixServiceLookupFactory serviceLookupFactory;

	@Override
	public List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptor) {
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Class<?> exportedApi : getProvidedBeans(descriptor)) {
			AstrixServiceLookup serviceLookup = getLookupStrategy(descriptor);
			result.add(new AstrixServiceFactory<>(descriptor, exportedApi, serviceLookup, serviceComponents));
			Class<?> asyncInterface = loadInterfaceIfExists(exportedApi.getName() + "Async");
			if (asyncInterface != null) {
				result.add(new AstrixServiceFactory<>(descriptor, asyncInterface, serviceLookup, serviceComponents));
			}
		}
		return result;
	}

	private AstrixServiceLookup getLookupStrategy(AstrixApiDescriptor descriptor) {
		return serviceLookupFactory.createServiceLookup(descriptor);
	}

	@Override
	public List<Class<?>> getProvidedBeans(AstrixApiDescriptor descriptor) {
		Class<?>[] providedServices = descriptor.getAnnotation(AstrixServiceProvider.class).value();
		return Arrays.asList(providedServices);
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixServiceProvider.class;
	}

	@Override
	public boolean isLibraryProvider() {
		return false;
	}

	private Class<?> loadInterfaceIfExists(String interfaceName) {
		try {
			Class<?> c = Class.forName(interfaceName);
			if (c.isInterface()) {
				return c;
			}
		} catch (ClassNotFoundException e) {
			// fall through and return null
		}
		return null;
	}
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setServiceLookupFactory(AstrixServiceLookupFactory serviceLookupFactory) {
		this.serviceLookupFactory = serviceLookupFactory;
	}

}
