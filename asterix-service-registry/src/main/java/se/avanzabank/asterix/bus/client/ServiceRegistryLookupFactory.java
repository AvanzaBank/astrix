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

import java.util.Arrays;
import java.util.List;

import se.avanzabank.asterix.context.AsterixBeanAware;
import se.avanzabank.asterix.context.AsterixBeans;
import se.avanzabank.asterix.context.AsterixFactoryBean;

public class ServiceRegistryLookupFactory<T> implements AsterixFactoryBean<T>, AsterixBeanAware {

	private Class<T> api;
	private Class<?> descriptorHolder;
	private AsterixServiceRegistryComponent serviceRegistryComponent;
	private AsterixBeans beans;

	public ServiceRegistryLookupFactory(Class<?> descriptorHolder,
			Class<T> api,
			AsterixServiceRegistryComponent serviceRegistryComponent) {
		this.descriptorHolder = descriptorHolder;
		this.api = api;
		this.serviceRegistryComponent = serviceRegistryComponent;
	}

	@Override
	public T create() {
		// TODO: always return a proxy-instance, no matter in which of the steps below that the lookup fails
		AsterixServiceRegistry serviceRegistry = beans.getBean(AsterixServiceRegistry.class); // service dependency
		AsterixServiceProperties serviceProperties = serviceRegistry.lookup(api); // TODO: might fail
		if (serviceProperties == null) {
			// TODO: manage non discovered services
			throw new RuntimeException("Did not discover: " + api);
		}
		return serviceRegistryComponent.createService(descriptorHolder, api, serviceProperties);
	}
	
	@Override
	public List<Class<?>> getBeanDependencies() {
		return Arrays.<Class<?>>asList(AsterixServiceRegistry.class);
	}

	@Override
	public Class<T> getBeanType() {
		return this.api;
	}

	@Override
	public void setAsterixBeans(AsterixBeans beans) {
		this.beans = beans;
	}

	/*
	 * TODO: skapa en mekanism som upprätthåller en GigaSpace-koppling:
	 * 
	 * 1. Lazy uppkoppling om inte aktuellt space är tillgängligt vid uppstart
	 * 2. Återanslutning om ett space försvinner temporärt
	 * 
	 * Vad händer om ett space flyttas permanent? Vem ansvarar för ned-koppling och återanslutning?
	 * 
	 */

}
