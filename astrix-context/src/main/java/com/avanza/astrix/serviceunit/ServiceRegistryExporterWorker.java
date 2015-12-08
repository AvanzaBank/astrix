/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.serviceunit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryFactory;
import com.avanza.astrix.beans.registry.ServiceRegistryExporterClient;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.util.AstrixFrameworkThread;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.core.ServiceUnavailableException;
/**
 * The service registry worker is a server-side component responsible for continuously publishing 
 * all exported services from the current application onto the service registry.
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
public class ServiceRegistryExporterWorker extends AstrixFrameworkThread {
	
	private final List<ServiceRegistryExportedService> exportedServices = new CopyOnWriteArrayList<>();
	private ServiceRegistryExporterClient serviceRegistryProviderClient;
	private final Logger log = LoggerFactory.getLogger(ServiceRegistryExporterWorker.class);
	private final DynamicLongProperty exportIntervallMillis;		  
	private final DynamicLongProperty serviceLeaseTimeMillis;
	private final DynamicLongProperty retryIntervallMillis;
	private final AstrixConfig config;
	private final Timer timer = new Timer();
	private final AstrixServiceRegistryFactory serviceRegistryFactory;

	public ServiceRegistryExporterWorker(AstrixConfig config, AstrixServiceRegistryFactory serviceRegistryFactory) {
		super("ServiceRegistryExporter");
		this.config = config;
		this.serviceRegistryFactory = serviceRegistryFactory;
		this.exportIntervallMillis = config.get(AstrixSettings.SERVICE_REGISTRY_EXPORT_INTERVAL);
		this.retryIntervallMillis = config.get(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL);
		this.serviceLeaseTimeMillis = config.get(AstrixSettings.SERVICE_REGISTRY_LEASE);
	}
	
	public void startServiceExporter() {
		if (exportedServices.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry");
			return;
		}
		createServiceRegistryExporterClient();
		start();
	}

	private void createServiceRegistryExporterClient() {
		/*
		 * The ServiceRegistryExporterClient is created lazily to
		 * avoid that the service registry is attempting to create an 
		 * AstrixServiceRegistry instance against itself.
		 */
		String subsystem = config.get(AstrixSettings.SUBSYSTEM_NAME).get();
		String applicationInstanceId = config.get(AstrixSettings.APPLICATION_INSTANCE_ID).get();
		String applicationTag = config.get(AstrixSettings.APPLICATION_TAG).get();
		String zone = subsystem;
		if (applicationTag != null) {
			zone = subsystem + "#"  + applicationTag;
		}
		this.serviceRegistryProviderClient = new ServiceRegistryExporterClient(serviceRegistryFactory.createServiceRegistry(), subsystem, applicationInstanceId, zone);
	}
	
	@PreDestroy
	public void destroy() {
		interrupt();
	}
	
	public void triggerServiceExport() {
		this.timer.wakeup();
	}
	
	@Override
	public void run() {
		while (!interrupted()) {
			long sleepTimeUntilNextAttempt = this.exportIntervallMillis.get();
			try {
				exportProvidedServcies();
			} catch (ServiceUnavailableException e) {
				// Not bound to service registry
				sleepTimeUntilNextAttempt = this.retryIntervallMillis.get();
				log.info(String.format("Failed to export services to registry. Sleeping %s millis until next attempt.", sleepTimeUntilNextAttempt), e);
			} catch (Exception e) {
				log.info(String.format("Failed to export services to registry. Sleeping %s millis until next attempt.", sleepTimeUntilNextAttempt), e);
			} 
			try {
				this.timer.sleep(sleepTimeUntilNextAttempt);
			} catch (InterruptedException e) {
				interrupt();
			}
		}
		log.info("ServiceRegistryExporterWorker is interrupted, won't publish to service registry anymore.");
	}

	private void exportProvidedServcies() {
		for (ServiceRegistryExportedService exportedService : exportedServices) {
			ServiceProperties serviceProperties = exportedService.exportServiceProperties();
			serviceRegistryProviderClient.register(serviceProperties.getApi(), serviceProperties, serviceLeaseTimeMillis.get());
			log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
		}
	}

	public void addServiceBuilder(
			ServiceRegistryExportedService serviceRegistryExportedService) {
		this.exportedServices.add(serviceRegistryExportedService);
	}

	private static class Timer {
		
		private final Object monitor = new Object();
		
		void sleep(long time) throws InterruptedException {
			synchronized (monitor) {
				monitor.wait(time);
			}
		}
		
		void wakeup() {
			synchronized (monitor) {
				monitor.notifyAll();
			}
		}
	}
	

}
