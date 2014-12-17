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
import java.util.Collections;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.provider.library.AstrixExport;
import com.avanza.astrix.provider.library.AstrixLibraryProvider;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

@MetaInfServices(AstrixApiProviderPlugin.class)
public class AstrixLibraryProviderPlugin implements AstrixApiProviderPlugin {
	
	private ObjectCache instanceCache;
	
	@Override
	public List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptorHolder) {
		Object libraryProviderInstance = initInstanceProvider(descriptorHolder);
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Method m : descriptorHolder.getDescriptorClass().getMethods()) {
			if (m.isAnnotationPresent(AstrixExport.class)) {
				result.add(new AstrixLibraryFactory<>(libraryProviderInstance, m, null));
			}
		}
		return result;
	}
	
	@Override
	public ServiceVersioningContext createVersioningContext(AstrixApiDescriptor descriptor, Class<?> api) {
		return ServiceVersioningContext.nonVersioned();
	}

	private Object initInstanceProvider(AstrixApiDescriptor descriptor) {
		return instanceCache.getInstance(ObjectId.internalClass(descriptor.getDescriptorClass()));
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixLibraryProvider.class;
	}
	
	@AstrixInject
	public void setInstanceCache(ObjectCache instanceCache) {
		this.instanceCache = instanceCache;
	}
	
	@Override
	public List<AstrixServiceBeanDefinition> getProvidedServices(AstrixApiDescriptor descriptor) {
		return Collections.emptyList();
	}
	
}
