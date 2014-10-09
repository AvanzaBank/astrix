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
package se.avanzabank.asterix.service.registry.server;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixServiceExporterBean;
import se.avanzabank.asterix.provider.component.AsterixServiceComponentNames;
import se.avanzabank.asterix.provider.core.AsterixServiceComponent;

public class AsterixServiceRegistryServiceExporterBean implements AsterixServiceExporterBean {
	
	// TODO: if service-registry is decided to be a well-known component, then this class should be removed

	// TODO: let this class register all services that should be exported to service registry
	private AsterixServiceRegistryExporterWorker worker;
	
	@Override
	public void register(Object provider, AsterixApiDescriptor apiDescriptor, Class<?> providedApi) {
		String component = getComponent(provider);
		AsterixServiceExporterBean serviceExporterBean = getServiceExporterBean(component);
		serviceExporterBean.register(provider, apiDescriptor, providedApi);
	}

	private AsterixServiceExporterBean getServiceExporterBean(String component) {
		// TODO Auto-generated method stub
		return null;
	}
	
	@Override
	public String getTransport() {
		return "SERVICE_REGISTRY";
	}

	private String getComponent(Object provider) {
		if (!provider.getClass().isAnnotationPresent(AsterixServiceComponent.class)) {
			throw new IllegalStateException("All services exported using service-registry must annotate provider instanes with " + AsterixServiceComponent.class.getSimpleName());
		}
		AsterixServiceComponent serviceComponent = provider.getClass().getAnnotation(AsterixServiceComponent.class);
		return serviceComponent.value();
	}

}
