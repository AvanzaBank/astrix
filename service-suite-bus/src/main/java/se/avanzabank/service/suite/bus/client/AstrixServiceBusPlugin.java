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

import se.avanzabank.service.suite.context.AstrixContext;
import se.avanzabank.service.suite.context.AstrixPluginDiscovery;
import se.avanzabank.service.suite.context.AstrixServiceFactory;
import se.avanzabank.service.suite.context.AstrixServiceProvider;
import se.avanzabank.service.suite.context.AstrixServiceProviderPlugin;
import se.avanzabank.service.suite.provider.core.AstrixServiceBusApi;

public class AstrixServiceBusPlugin implements AstrixServiceProviderPlugin {
	
	private AstrixContext context;
	
	public AstrixServiceBusPlugin() {
	}

	@Override
	public AstrixServiceProvider create(Class<?> descriptorHolder) {
		List<AstrixServiceFactory<?>> factories = new ArrayList<>();
		// Find component
		
		for (AstrixServiceBusComponent component : getAllComponents()) {
			for (Class<?> exportedApi : component.getExportedServices(descriptorHolder)) {
				// I det här läget vill jag inte slå upp tjänsten, enbart skapa en factory som 'on demand' slår upp
				// tjänsten över tjänstebussen.
				ServiceBusLookupServiceFactory<?> factory = new ServiceBusLookupServiceFactory<>(descriptorHolder, exportedApi, component, context);
				factories.add(factory);
			}
		}
		return new AstrixServiceProvider(factories , descriptorHolder);
	}

	private List<AstrixServiceBusComponent> getAllComponents() {
		return context.getPlugins(AstrixServiceBusComponent.class);
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixServiceBusApi.class;
	}

	@Override
	public void setContext(AstrixContext context) {
		this.context = context;
		AstrixPluginDiscovery.discoverPlugins(AstrixServiceBusComponent.class);
	}

}
