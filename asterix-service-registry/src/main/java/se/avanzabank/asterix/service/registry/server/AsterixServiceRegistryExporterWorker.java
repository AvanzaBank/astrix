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

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.context.AsterixContext;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixServicePropertiesBuilderHolder;
import se.avanzabank.asterix.context.AsterixSettings;
import se.avanzabank.asterix.context.AsterixSettingsReader;
import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryClient;
/**
 * The service registry worker is a server-side component responsible for continuously publishing 
 * all exported services from the current application onto the service registry.
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
public class AsterixServiceRegistryExporterWorker extends Thread {
	
	private List<AsterixServicePropertiesBuilderHolder> serviceBuilders = Collections.emptyList();
	private final AsterixServiceRegistryClient serviceRegistryClient;
	private final Logger log = LoggerFactory.getLogger(AsterixServiceRegistryExporterWorker.class);
	private final long exportIntervallMillis;		  
	private final long serviceLeaseTimeMillis;
	private final long retryIntervallMillis;

	@Autowired
	public AsterixServiceRegistryExporterWorker(AsterixServiceRegistryClient serviceRegistry, AsterixContext context) {
		this.serviceRegistryClient = serviceRegistry;
		AsterixSettingsReader settings = context.getSettings();
		this.exportIntervallMillis = settings.getLong(AsterixSettings.SERVICE_REGISTRY_EXPORT_INTERVAL, 60_000L);
		this.retryIntervallMillis = settings.getLong(AsterixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 10_000L);
		this.serviceLeaseTimeMillis = settings.getLong(AsterixSettings.SERVICE_REGISTRY_LEASE, 120_000L);
	}
	
	@Autowired(required = false)
	public void setServiceBuilders(List<AsterixServicePropertiesBuilderHolder> serviceBuilders) {
		this.serviceBuilders = serviceBuilders;
	}
	

	@PostConstruct
	public void startServiceExporter() {
		if (serviceBuilders.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry");
		}
		start();
	}
	
	@PreDestroy
	public void destroy() {
		interrupt();
	}

	// TODO: For pu's: only run on one primary instance (typically the one with id "1")
	@Override
	public void run() {
		while (!interrupted()) {
			long sleepTimeUntilNextAttempt = this.exportIntervallMillis;
			try {
				exportProvidedServcies();
			} catch (ServiceUnavailableException e) {
				// Not bound to service registry
				sleepTimeUntilNextAttempt = this.retryIntervallMillis;
				log.info(String.format("Failed to export serivces to registry. Sleeping %s millis until next attempt.", sleepTimeUntilNextAttempt), e);
			} catch (Exception e) {
				log.info(String.format("Failed to export serivces to registry. Sleeping %s millis until next attempt.", sleepTimeUntilNextAttempt), e);
			} 
			try {
				sleep(sleepTimeUntilNextAttempt);
			} catch (InterruptedException e) {
				interrupt();
			}
		}
	}

	private void exportProvidedServcies() {
		for (AsterixServicePropertiesBuilderHolder serviceBuilder : serviceBuilders) {
			AsterixServiceProperties serviceProperties = serviceBuilder.exportServiceProperties();
			serviceRegistryClient.register(serviceProperties.getApi(), serviceProperties, serviceLeaseTimeMillis);
			log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
			if (serviceBuilder.exportsAsyncApi()) {
				serviceProperties = serviceBuilder.exportAsyncServiceProperties();
				serviceRegistryClient.register(serviceProperties.getApi(), serviceProperties, serviceLeaseTimeMillis);
				log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
			}
		}
	}
	

}
