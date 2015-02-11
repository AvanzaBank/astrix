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
package com.avanza.astrix.service.registry.server;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.context.AstrixContextImpl;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistryClient;
/**
 * The service registry worker is a server-side component responsible for continuously publishing 
 * all exported services from the current application onto the service registry.
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
public class AstrixServiceRegistryExporterWorker extends Thread {
	
	private List<AstrixServicePropertiesBuilderHolder> serviceBuilders = new CopyOnWriteArrayList<>();
	private AstrixServiceRegistryClient serviceRegistryClient;
	private Logger log = LoggerFactory.getLogger(AstrixServiceRegistryExporterWorker.class);
	private DynamicLongProperty exportIntervallMillis;		  
	private DynamicLongProperty serviceLeaseTimeMillis;
	private DynamicLongProperty retryIntervallMillis;
	private AstrixContextImpl astrixContext;

	public AstrixServiceRegistryExporterWorker() {
	}
	
	@Autowired(required = false)
	public void setServiceBuilders(List<AstrixServicePropertiesBuilderHolder> serviceBuilders) {
		this.serviceBuilders = serviceBuilders;
	}
	

	public void startServiceExporter() {
		if (serviceBuilders.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry");
			return;
		}
		this.serviceRegistryClient = astrixContext.getBean(AstrixServiceRegistryClient.class);
		DynamicConfig config = astrixContext.getConfig();
		this.exportIntervallMillis = config.getLongProperty(AstrixSettings.SERVICE_REGISTRY_EXPORT_INTERVAL, 30_000L);
		this.retryIntervallMillis = config.getLongProperty(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 5_000L);
		this.serviceLeaseTimeMillis = config.getLongProperty(AstrixSettings.SERVICE_REGISTRY_LEASE, 120_000L);
		start();
	}
	
	@PreDestroy
	public void destroy() {
		interrupt();
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
		for (AstrixServicePropertiesBuilderHolder serviceBuilder : serviceBuilders) {
			AstrixServiceProperties serviceProperties = serviceBuilder.exportServiceProperties();
			serviceRegistryClient.register(serviceProperties.getApi(), serviceProperties, serviceLeaseTimeMillis.get());
			log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
			if (serviceBuilder.exportsAsyncApi()) {
				serviceProperties = serviceBuilder.exportAsyncServiceProperties();
				serviceRegistryClient.register(serviceProperties.getApi(), serviceProperties, serviceLeaseTimeMillis.get());
				log.debug("Exported to service registry. service={} properties={}", serviceProperties.getApi().getName(), serviceProperties);
			}
		}
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContextImpl astrixContext) {
		this.astrixContext = astrixContext;
	}

	public void addServiceBuilder(
			AstrixServicePropertiesBuilderHolder astrixServicePropertiesBuilderHolder) {
		this.serviceBuilders.add(astrixServicePropertiesBuilderHolder);
	}
	

}
