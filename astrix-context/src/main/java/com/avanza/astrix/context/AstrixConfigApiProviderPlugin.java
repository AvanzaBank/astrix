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

import com.avanza.astrix.provider.core.AstrixConfigApi;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(AstrixApiProviderPlugin.class)
public class AstrixConfigApiProviderPlugin implements AstrixApiProviderPlugin, AstrixSettingsAware {

	private AstrixSettingsReader settings;

	@Override
	public List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptor) {
		AstrixConfigApi configApi = descriptor.getAnnotation(AstrixConfigApi.class);
		String entryName = configApi.entryName();
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Class<?> beanType : configApi.exportedApis()) {
			AstrixConfigFactoryBean<?> factory = new AstrixConfigFactoryBean<>(entryName, descriptor, beanType, settings);
			result.add(factory);
		}
		return result;
	}

	@Override
	public List<Class<?>> getProvidedBeans(AstrixApiDescriptor descriptor) {
		return Arrays.<Class<?>>asList(descriptor.getAnnotation(AstrixConfigApi.class).exportedApis());
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixConfigApi.class;
	}

	@Override
	public boolean isLibraryProvider() {
		return false;
	}
	
	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}

}
