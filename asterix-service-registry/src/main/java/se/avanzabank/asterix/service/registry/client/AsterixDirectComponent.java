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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.kohsuke.MetaInfServices;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.provider.component.AsterixServiceRegistryComponentNames;
import se.avanzabank.asterix.service.registry.server.ServiceRegistryExporter;

@MetaInfServices(AsterixServiceRegistryComponent.class)
public class AsterixDirectComponent implements AsterixServiceRegistryComponent {
	
	private final static AtomicLong idGen = new AtomicLong();
	private final static Map<String, Object> providerByName = new ConcurrentHashMap<String, Object>();
	
	@Override
	public <T> T createService(AsterixApiDescriptor apiDescriptor, Class<T> type, AsterixServiceProperties serviceProperties) {
		String providerName = serviceProperties.getProperty("providerName");
		Object result = providerByName.get(providerName);
		if (result == null) {
			throw new IllegalStateException("Cant find provider for with name="  + providerName + " and type=" + type);
		}
		return type.cast(result);
	}

	public void register(String name, Object provider) {
		providerByName.put(name, provider);
	}

	@Override
	public String getName() {
		return AsterixServiceRegistryComponentNames.DIRECT;
	}

	@Override
	public Class<? extends ServiceRegistryExporter> getServiceExporterClass() {
		return null; // NOT USED. Client side component only 
	}

	@Override
	public List<String> getComponentDepenencies() {
		return Collections.emptyList(); // NOT USED. Client side component only 
	}

	@Override
	public void registerBeans(BeanDefinitionRegistry registry) {
		 // NOT USED. Client side component only 
	}

	public String register(Object provider) {
		// TODO: detect multiple registrations of same instance
		String id = String.valueOf(idGen.incrementAndGet());
		providerByName.put(id, provider);
		return id;
	}
}