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
package se.avanzabank.asterix.provider.context;

import java.util.Collections;
import java.util.List;

import javax.annotation.PostConstruct;

import org.openspaces.core.GigaSpace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.bus.client.AstrixServiceBus;
import se.avanzabank.asterix.bus.client.AstrixServiceBusApiDescriptor;
import se.avanzabank.asterix.bus.client.AstrixServiceProperties;
import se.avanzabank.asterix.context.AstrixPlugins;
import se.avanzabank.asterix.context.AstrixVersioningPlugin;
import se.avanzabank.asterix.remoting.client.AstrixRemotingProxy;
import se.avanzabank.asterix.remoting.client.AstrixRemotingTransport;
import se.avanzabank.space.SpaceLocator;
/**
 * The service bus worker is a server-side component responsible for continiously publishing 
 * all exported services from the current application onto the service bus.
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
public class AstrixServiceBusExporterWorker extends Thread {
	
	private List<ServiceBusExporter> serviceExporters = Collections.emptyList();
	private final AstrixServiceBus serviceBus;
	private final Logger log = LoggerFactory.getLogger(AstrixServiceBusExporterWorker.class);

	@Autowired
	public AstrixServiceBusExporterWorker(
			SpaceLocator sl, // External dependency
			AstrixPlugins astrixPlugins) { // Plugin dependency
		AstrixVersioningPlugin versioningPlugin = astrixPlugins.getPlugin(AstrixVersioningPlugin.class);
		// TODO: AstrixSerivceBus should be retreived from service-framework, not by hard-coding usage of remoting-framework here.
		GigaSpace serviceBusSpace = sl.createClusteredProxy("service-bus-space"); // TODO: fault tolerance, connection mannagment, etc.
		this.serviceBus = AstrixRemotingProxy.create(AstrixServiceBus.class, AstrixRemotingTransport.remoteSpace(serviceBusSpace), versioningPlugin.create(AstrixServiceBusApiDescriptor.class));
	}

	public AstrixServiceBusExporterWorker(List<ServiceBusExporter> serviceProvideres, AstrixServiceBus serviceBus) {
		this.serviceBus = serviceBus;
		this.serviceExporters = serviceProvideres;
	}

	@Autowired(required = false)
	public void setServiceExporters(List<ServiceBusExporter> serviceExporters) {
		this.serviceExporters = serviceExporters;
	}
	

	@PostConstruct
	public void startServiceExporter() {
		if (serviceExporters.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published on service bus");
		}
		start();
	}

	// TODO: proper logging and management of exporter thread
	// TODO: For pu's: only run on one primary instance (typically the one with id "1")
	@Override
	public void run() {
		while (!interrupted()) {
			exportProvidedServcies();
			try {
				sleep(60_000L);// TODO: intervall of lease renewal
			} catch (InterruptedException e) {
				interrupt();
			} 
		}
	}

	private void exportProvidedServcies() {
		for (ServiceBusExporter provider : serviceExporters) {
			for (AstrixServiceProperties serviceProperties : provider.getProvidedServices()) {
				log.debug("Exporting on service bus. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
				serviceBus.register(serviceProperties.getApi(), serviceProperties);
			}
		}
	}
	

}
