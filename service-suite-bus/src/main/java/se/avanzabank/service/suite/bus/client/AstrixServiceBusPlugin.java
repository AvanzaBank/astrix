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
package se.avanzabank.service.suite.bus.client;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.service.suite.context.AstrixPlugins;
import se.avanzabank.service.suite.context.AstrixPluginsAware;
import se.avanzabank.service.suite.context.AstrixFactoryBean;
import se.avanzabank.service.suite.context.AstrixApiProviderPlugin;
import se.avanzabank.service.suite.provider.core.AstrixServiceBusApi;

@MetaInfServices(AstrixApiProviderPlugin.class)
public class AstrixServiceBusPlugin implements AstrixApiProviderPlugin, AstrixPluginsAware {
	
	private AstrixPlugins plugins;

	@Override
	public List<AstrixFactoryBean<?>> createFactoryBeans(Class<?> descriptorHolder) {
		List<AstrixFactoryBean<?>> result = new ArrayList<>();
		for (AstrixServiceBusComponent component : getAllComponents()) {
			for (Class<?> exportedApi : component.getExportedServices(descriptorHolder)) {
				result.add(new ServiceBusLookupFactory<>(descriptorHolder, exportedApi, component));
			}
		}
		return result;
	}

	private List<AstrixServiceBusComponent> getAllComponents() {
		return plugins.getPlugins(AstrixServiceBusComponent.class);
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixServiceBusApi.class;
	}

	@Override
	public boolean consumes(Class<?> descriptorHolder) {
		return descriptorHolder.isAnnotationPresent(getProviderAnnotationType());
	}
	
	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.plugins = plugins;
	}

}
