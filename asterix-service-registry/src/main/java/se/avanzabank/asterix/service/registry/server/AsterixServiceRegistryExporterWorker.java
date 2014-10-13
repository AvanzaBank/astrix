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

import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixServiceBuilderHolder;
import se.avanzabank.asterix.context.AsterixServiceProperties;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;
import se.avanzabank.asterix.core.ServiceUnavailableException;
import se.avanzabank.asterix.remoting.client.AsterixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AsterixRemotingTransport;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistry;
import se.avanzabank.asterix.service.registry.client.AsterixServiceRegistryApiDescriptor;
import se.avanzabank.space.SpaceLocator;
/**
 * The service registry worker is a server-side component responsible for continuously publishing 
 * all exported services from the current application onto the service registry.
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
public class AsterixServiceRegistryExporterWorker extends Thread {
	
	private List<AsterixServiceBuilderHolder> serviceBuilders = Collections.emptyList();
	private final AsterixServiceRegistry serviceRegistry;
	private final Logger log = LoggerFactory.getLogger(AsterixServiceRegistryExporterWorker.class);
	private volatile long exportIntervallMillis = 60_000;

	@Autowired
	public AsterixServiceRegistryExporterWorker(
			SpaceLocator sl, // External dependency
			AsterixPlugins asterixPlugins) { // Plugin dependency
		AsterixVersioningPlugin versioningPlugin = asterixPlugins.getPlugin(AsterixVersioningPlugin.class);
		// TODO: AsterixServiceRegistry should be retreived from service-framework, not by hard-coding usage of remoting-framework here.
		GigaSpace serviceRegistrySpace = sl.createClusteredProxy("service-registry-space"); // TODO: fault tolerance, connection mannagment, etc.
		AsterixObjectSerializer serializer = versioningPlugin.create(new AsterixApiDescriptor(AsterixServiceRegistryApiDescriptor.class));
		this.serviceRegistry = AsterixRemotingProxy.create(AsterixServiceRegistry.class, AsterixRemotingTransport.remoteSpace(serviceRegistrySpace), serializer);
	}
	
	@Autowired(required = false)
	public void setServiceBuilders(List<AsterixServiceBuilderHolder> serviceBuilders) {
		this.serviceBuilders = serviceBuilders;
	}
	

	@PostConstruct
	public void startServiceExporter() {
		if (serviceBuilders.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry");
		}
		start();
	}

	// TODO: proper logging and management of exporter thread
	// TODO: For pu's: only run on one primary instance (typically the one with id "1")
	@Override
	public void run() {
		while (!interrupted()) {
			try {
				exportProvidedServcies();
				exportIntervallMillis = 60_000;
				sleep(exportIntervallMillis);// TODO: intervall of lease renewal
			} catch (InterruptedException e) {
				interrupt();
			} catch (ServiceUnavailableException e) {
				// No bound to service registry
				log.info("Failed to export serivces to registry", e);
				exportIntervallMillis = 250; // TODO: interval when service registry not available?
			} 
		}
	}

	private void exportProvidedServcies() {
		for (AsterixServiceBuilderHolder serviceBuilder : serviceBuilders) {
			AsterixServiceProperties serviceProperties = serviceBuilder.exportServiceProperties();
			serviceRegistry.register(serviceProperties.getApi(), serviceProperties);
			log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
			if (serviceBuilder.exportsAsyncApi()) {
				serviceProperties = serviceBuilder.exportAsyncServiceProperties();
				serviceRegistry.register(serviceProperties.getApi(), serviceProperties);
				log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
			}
		}
	}
	

}
