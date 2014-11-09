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
package com.avanza.asterix.context;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;

public class AsterixServiceExporterBeans {
	
	private final Map<String, AsterixServiceExporterBean> serviceExporterByComponentName = new ConcurrentHashMap<>();
	
	@Autowired
	public AsterixServiceExporterBeans(List<AsterixServiceExporterBean> serviceExporters) {
		for (AsterixServiceExporterBean serviceExporter : serviceExporters) {
			serviceExporterByComponentName.put(serviceExporter.getComponent(), serviceExporter);
		}
	}

	public AsterixServiceExporterBean getServiceExporter(AsterixExportedService service) {
		AsterixServiceExporterBean asterixServiceExporterBean = serviceExporterByComponentName.get(service.getComponentName());
		if (asterixServiceExporterBean == null) {
			throw new IllegalStateException("No AsterixServiceExporterBean found for component=" + service.getComponentName());
		}
		return asterixServiceExporterBean;
	}
	
	public AsterixServiceExporterBean getServiceExporter(String componentName) {
		AsterixServiceExporterBean asterixServiceExporterBean = serviceExporterByComponentName.get(componentName);
		if (asterixServiceExporterBean == null) {
			throw new IllegalStateException("No AsterixServiceExporterBean found for component=" + componentName);
		}
		return asterixServiceExporterBean;
	}
	
}
