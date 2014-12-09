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
package com.avanza.astrix.service.registry.client;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.kohsuke.MetaInfServices;

import com.avanza.astrix.context.AstrixApiDescriptor;
import com.avanza.astrix.context.AstrixApiProviderPlugin;
import com.avanza.astrix.context.AstrixFactoryBeanPlugin;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixPlugins;
import com.avanza.astrix.context.AstrixPluginsAware;
import com.avanza.astrix.context.AstrixServiceComponents;
import com.avanza.astrix.context.AstrixServiceFactory;
import com.avanza.astrix.context.AstrixServiceLeaseManager;
import com.avanza.astrix.context.AstrixServiceLookup;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.avanza.astrix.provider.core.AstrixServiceRegistryApi;

@MetaInfServices(AstrixApiProviderPlugin.class)
public class AstrixServiceRegistryProviderPlugin implements AstrixApiProviderPlugin, AstrixPluginsAware, AstrixSettingsAware {
	
	private AstrixServiceComponents serviceComponents;
	private AstrixServiceLeaseManager leaseManager;
	private AstrixSettingsReader settings;
	private AstrixPlugins plugins;

	@Override
	public List<AstrixFactoryBeanPlugin<?>> createFactoryBeans(AstrixApiDescriptor descriptor) {
		List<AstrixFactoryBeanPlugin<?>> result = new ArrayList<>();
		for (Class<?> exportedApi : getProvidedBeans(descriptor)) {
			AstrixServiceLookup serviceLookup = AstrixServiceLookup.create(plugins.getPluginInstance(AstrixServiceRegistryLookupPlugin.class), null);
			result.add(new AstrixServiceFactory<>(descriptor, exportedApi, serviceLookup, serviceComponents, leaseManager, settings));
			Class<?> asyncInterface = loadInterfaceIfExists(exportedApi.getName() + "Async");
			if (asyncInterface != null) {
				result.add(new AstrixServiceFactory<>(descriptor, asyncInterface, serviceLookup, serviceComponents, leaseManager, settings));
			}
		}
		return result;
	}
	
	@Override
	public List<Class<?>> getProvidedBeans(AstrixApiDescriptor descriptor) {
		List<Class<?>> result = new ArrayList<>();
		result.addAll(Arrays.asList(getExportedApis(descriptor)));
		return result;
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

	private Class<?>[] getExportedApis(AstrixApiDescriptor descriptor) {
		Class<?> [] exportedApis = descriptor.getAnnotation(AstrixServiceRegistryApi.class).value();
		if (exportedApis.length > 0) {
			return exportedApis;
		}
		throw new IllegalArgumentException("No exported apis found on AstrixServiceRegistryApi: " + descriptor.getName());	
	}
	
	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixServiceRegistryApi.class;
	}
	
	@Override
	public boolean isLibraryProvider() {
		return false;
	}

	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.plugins = plugins;
	}
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setLeaseManager(AstrixServiceLeaseManager leaseManager) {
		this.leaseManager = leaseManager;
	}

	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}

}
