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
package com.avanza.asterix.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.asterix.provider.library.AsterixExport;
import com.avanza.asterix.provider.library.AsterixLibraryProvider;

@MetaInfServices(AsterixApiProviderPlugin.class)
public class AsterixLibraryProviderPlugin implements AsterixApiProviderPlugin {
	
	private InstanceCache instanceCache;
	
	@Override
	public List<AsterixFactoryBeanPlugin<?>> createFactoryBeans(AsterixApiDescriptor descriptorHolder) {
		Object libraryProviderInstance = initInstanceProvider(descriptorHolder);
		List<AsterixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Method m : descriptorHolder.getDescriptorClass().getMethods()) {
			if (m.isAnnotationPresent(AsterixExport.class)) {
				result.add(new AsterixLibraryFactory<>(libraryProviderInstance, m));
			}
		}
		return result;
	}

	private Object initInstanceProvider(AsterixApiDescriptor descriptor) {
		return instanceCache.getInstance(descriptor.getDescriptorClass());
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AsterixLibraryProvider.class;
	}
	
	@Override
	public boolean isLibraryProvider() {
		return true;
	}
	
	@AsterixInject
	public void setInstanceCache(InstanceCache instanceCache) {
		this.instanceCache = instanceCache;
	}
	
	@Override
	public List<Class<?>> getProvidedBeans(AsterixApiDescriptor descriptor) {
		List<Class<?>> result = new ArrayList<>();
		for (Method m : descriptor.getDescriptorClass().getMethods()) {
			if (m.isAnnotationPresent(AsterixExport.class)) {
				result.add(m.getReturnType());
			}
		}
		return result;
	}
	
}
