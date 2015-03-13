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

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.publish.AstrixPublishedBeans;
import com.avanza.astrix.beans.publish.AstrixPublishedBeansAware;
import com.avanza.astrix.beans.registry.AstrixServiceRegistryClient;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.core.ServiceUnavailableException;
/**
 * The service registry worker is a server-side component responsible for continuously publishing 
 * all exported services from the current application onto the service registry.
 * 
 * @author Elias Lindholm (elilin)
 * 
 */
public class AstrixServiceRegistryExporterWorker extends Thread implements AstrixPublishedBeansAware {
	
	private List<AstrixServicePropertiesBuilderHolder> serviceBuilders = new CopyOnWriteArrayList<>();
	private AstrixServiceRegistryClient serviceRegistryClient;
	private final Logger log = LoggerFactory.getLogger(AstrixServiceRegistryExporterWorker.class);
	private final DynamicLongProperty exportIntervallMillis;		  
	private final DynamicLongProperty serviceLeaseTimeMillis;
	private final DynamicLongProperty retryIntervallMillis;

	public AstrixServiceRegistryExporterWorker(DynamicConfig config) {
		this.exportIntervallMillis = config.getLongProperty(AstrixSettings.SERVICE_REGISTRY_EXPORT_INTERVAL, 30_000L);
		this.retryIntervallMillis = config.getLongProperty(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 5_000L);
		this.serviceLeaseTimeMillis = config.getLongProperty(AstrixSettings.SERVICE_REGISTRY_LEASE, 120_000L);
	}
	
	public void startServiceExporter() {
		if (serviceBuilders.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry");
			return;
		}
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
				log.info(String.format("Failed to export services to registry. Sleeping %s millis until next attempt.", sleepTimeUntilNextAttempt), e);
			} catch (Exception e) {
				log.info(String.format("Failed to export services to registry. Sleeping %s millis until next attempt.", sleepTimeUntilNextAttempt), e);
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
	
	public void addServiceBuilder(
			AstrixServicePropertiesBuilderHolder astrixServicePropertiesBuilderHolder) {
		this.serviceBuilders.add(astrixServicePropertiesBuilderHolder);
	}

	@Override
	public void setAstrixBeans(AstrixPublishedBeans beans) {
		this.serviceRegistryClient = beans.getBean(AstrixBeanKey.create(AstrixServiceRegistryClient.class));
	}
	

}
