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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.config.DynamicConfig;

public class ServiceRegistryExporter implements AstrixConfigAware {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceRegistryExporter.class);
	private final List<ServiceRegistryExportedService> exportedServices = new CopyOnWriteArrayList<>();
	private DynamicConfig config;
	private ServiceRegistryExporterWorker exporterWorker;
	
	public ServiceRegistryExporter(ServiceRegistryExporterWorker exporterWorker) {
		this.exporterWorker = exporterWorker;
	}

	public <T> void addExportedService(ExportedServiceBeanDefinition serviceBeanDefinition, ServiceComponent serviceComponent) {
		boolean publishServices = AstrixSettings.PUBLISH_SERVICES.getFrom(config).get();
		exportedServices.add(new ServiceRegistryExportedService(serviceComponent, serviceBeanDefinition, publishServices));
	}
	
	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}
	
	public void startPublishServices() {
		if (!AstrixSettings.RUN_SERVICE_REGISTRY_EXPORTER.getFrom(config).get()) {
			log.info("ServiceRegistryExporterWorker explicitly disabled. No services will be published to service registry");
			return;
		}
		if (exportedServices.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry.");
			return;
		}
		ServiceRegistryExporterWorker exporterWorker = getExporterWorker(); 
		for (ServiceRegistryExportedService serviceProperties : this.exportedServices) {
			exporterWorker.addServiceBuilder(serviceProperties);
		}
		exporterWorker.startServiceExporter();
	}
	
	private ServiceRegistryExporterWorker getExporterWorker() {
		return this.exporterWorker;
	}

	public void setPublished(boolean published) {
		for (ServiceRegistryExportedService serviceProperties : this.exportedServices) {
			serviceProperties.setPublishServices(published);
		}
		getExporterWorker().triggerServiceExport();
	}
}
