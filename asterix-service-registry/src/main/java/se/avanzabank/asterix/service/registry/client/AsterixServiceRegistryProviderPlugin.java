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
package se.avanzabank.asterix.service.registry.client;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixApiProviderPlugin;
import se.avanzabank.asterix.context.AsterixDecorator;
import se.avanzabank.asterix.context.AsterixFactoryBean;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.provider.core.AsterixServiceRegistryApi;

@MetaInfServices(AsterixApiProviderPlugin.class)
public class AsterixServiceRegistryProviderPlugin implements AsterixApiProviderPlugin, AsterixPluginsAware, AsterixDecorator {
	
	private AsterixPlugins plugins;
	private AsterixServiceRegistryLeaseManager leaseManager = new AsterixServiceRegistryLeaseManager();
	
	@Override
	public List<AsterixFactoryBean<?>> createFactoryBeans(AsterixApiDescriptor descriptor) {
		List<AsterixFactoryBean<?>> result = new ArrayList<>();
		for (Class<?> exportedApi : descriptor.getAnnotation(AsterixServiceRegistryApi.class).exportedApis()) {
			result.add(new ServiceRegistryLookupFactory<>(descriptor, exportedApi, plugins, leaseManager));
			Class<?> asyncInterface = loadInterfaceIfExists(exportedApi.getName() + "Async");
			if (asyncInterface != null) {
				result.add(new ServiceRegistryLookupFactory<>(descriptor, asyncInterface, plugins, leaseManager));
			}
		}
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

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AsterixServiceRegistryApi.class;
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}
	
	@Override
	public boolean useStatefulBeanFactory() {
		return true;
	}

	@Override
	public Object getTarget() {
		return leaseManager; // TODO: this is a hack. Introduce possibility for plugins to inject dependencies into objects that the create on their own?
	}
}
