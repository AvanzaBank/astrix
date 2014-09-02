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
package se.avanzabank.asterix.bus.client;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.asterix.context.AsterixApiProviderPlugin;
import se.avanzabank.asterix.context.AsterixFactoryBean;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixPluginsAware;
import se.avanzabank.asterix.provider.core.AsterixServiceBusApi;

@MetaInfServices(AsterixApiProviderPlugin.class)
public class AsterixServiceBusPlugin implements AsterixApiProviderPlugin, AsterixPluginsAware {
	
	private AsterixPlugins plugins;

	@Override
	public List<AsterixFactoryBean<?>> createFactoryBeans(Class<?> descriptorHolder) {
		List<AsterixFactoryBean<?>> result = new ArrayList<>();
		for (AsterixServiceBusComponent component : getAllComponents()) {
			for (Class<?> exportedApi : component.getExportedServices(descriptorHolder)) {
				result.add(new ServiceBusLookupFactory<>(descriptorHolder, exportedApi, component));
			}
		}
		return result;
	}

	private List<AsterixServiceBusComponent> getAllComponents() {
		return plugins.getPlugins(AsterixServiceBusComponent.class);
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AsterixServiceBusApi.class;
	}

	@Override
	public boolean consumes(Class<?> descriptorHolder) {
		return descriptorHolder.isAnnotationPresent(getProviderAnnotationType());
	}
	
	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.plugins = plugins;
	}

}
