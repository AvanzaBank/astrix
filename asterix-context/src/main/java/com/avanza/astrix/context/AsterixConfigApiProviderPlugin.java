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

import com.avanza.astrix.provider.core.AsterixConfigApi;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AsterixApiProviderPlugin.class)
public class AsterixConfigApiProviderPlugin implements AsterixApiProviderPlugin, AsterixSettingsAware {

	private AsterixSettingsReader settings;

	@Override
	public List<AsterixFactoryBeanPlugin<?>> createFactoryBeans(AsterixApiDescriptor descriptor) {
		AsterixConfigApi configApi = descriptor.getAnnotation(AsterixConfigApi.class);
		String entryName = configApi.entryName();
		List<AsterixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Class<?> beanType : configApi.exportedApis()) {
			AsterixConfigFactoryBean<?> factory = new AsterixConfigFactoryBean<>(entryName, descriptor, beanType, settings);
			result.add(factory);
		}
		return result;
	}

	@Override
	public List<Class<?>> getProvidedBeans(AsterixApiDescriptor descriptor) {
		return Arrays.<Class<?>>asList(descriptor.getAnnotation(AsterixConfigApi.class).exportedApis());
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AsterixConfigApi.class;
	}

	@Override
	public boolean isLibraryProvider() {
		return false;
	}
	
	@Override
	public void setSettings(AsterixSettingsReader settings) {
		this.settings = settings;
	}

}
