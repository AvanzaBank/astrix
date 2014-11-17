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
package com.avanza.asterix.service.registry.client;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.asterix.context.AsterixApiDescriptor;
import com.avanza.asterix.context.AsterixApiProviderPlugin;
import com.avanza.asterix.context.AsterixFactoryBeanPlugin;
import com.avanza.asterix.provider.core.AsterixServiceRegistryApi;

@MetaInfServices(AsterixApiProviderPlugin.class)
public class AsterixServiceRegistryProviderPlugin implements AsterixApiProviderPlugin {
	
	@Override
	public List<AsterixFactoryBeanPlugin<?>> createFactoryBeans(AsterixApiDescriptor descriptor) {
		List<AsterixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Class<?> exportedApi : getExportedApis(descriptor)) {
			result.add(new ServiceRegistryLookupFactory<>(descriptor, exportedApi));
			Class<?> asyncInterface = loadInterfaceIfExists(exportedApi.getName() + "Async");
			if (asyncInterface != null) {
				result.add(new ServiceRegistryLookupFactory<>(descriptor, asyncInterface));
			}
		}
		return result;
	}
	
	@Override
	public List<Class<?>> getProvidedBeans(AsterixApiDescriptor descriptor) {
		List<Class<?>> result = new ArrayList<>();
		result.addAll(Arrays.asList(getExportedApis(descriptor)));
		return result;
	}

	private Class<?>[] getExportedApis(AsterixApiDescriptor descriptor) {
		Class<?> [] exportedApisDeprecatedWay = descriptor.getAnnotation(AsterixServiceRegistryApi.class).exportedApis();
		Class<?> [] exportedApisNewWay = descriptor.getAnnotation(AsterixServiceRegistryApi.class).value();
		if (exportedApisDeprecatedWay.length > 0 && exportedApisNewWay.length > 0) {
			throw new IllegalArgumentException("Don't combine exportedApis and value. The exportedApis property is replcaed by the value property. Only defina a value property. Descriptor: " + descriptor.getName());
		}
		if (exportedApisDeprecatedWay.length > 0) {
			return exportedApisDeprecatedWay;
		}
		if (exportedApisNewWay.length > 0) {
			return exportedApisNewWay;
		}
		throw new IllegalArgumentException("No exported apis found on AsterixServiceRegistryApi: " + descriptor.getName());	
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

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AsterixServiceRegistryApi.class;
	}
	
	@Override
	public boolean isLibraryProvider() {
		return false;
	}
}
