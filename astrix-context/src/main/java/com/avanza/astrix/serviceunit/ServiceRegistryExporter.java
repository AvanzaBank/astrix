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
package com.avanza.astrix.serviceunit;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.service.AstrixServiceComponent;

public class ServiceRegistryExporter {
	
	private static final Logger log = LoggerFactory.getLogger(ServiceRegistryExporter.class);
	private final List<ServiceRegistryExportedService> exportedServices = new CopyOnWriteArrayList<>();
	private AstrixInjector injector;
	
	public <T> void addExportedService(AstrixBeanKey<T> beanKey, AstrixServiceComponent serviceComponent) {
		exportedServices.add(new ServiceRegistryExportedService(serviceComponent, beanKey));
	}
	
	@AstrixInject
	public void setInjector(AstrixInjector injector) {
		this.injector = injector;
	}
	
	public void startPublishServices() {
		if (exportedServices.isEmpty()) {
			log.info("No ServiceExporters configured. No services will be published to service registry.");
			return;
		}
		ServiceRegistryExporterWorker exporterWorker = injector.getBean(ServiceRegistryExporterWorker.class); 
		for (ServiceRegistryExportedService serviceProperties : this.exportedServices) {
			exporterWorker.addServiceBuilder(serviceProperties);
		}
		exporterWorker.startServiceExporter();
	}
	
}
