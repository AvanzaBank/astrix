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

import javax.annotation.PostConstruct;

/**
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixSingleServiceExporter {
	
	private AsterixExportedServiceInfo exportedService;
	private AsterixServiceExporterBean serviceExporterBean;
	private Object provider;

	@PostConstruct
	public void register() {
		// At this stage all services that are required to be exported are identified
		serviceExporterBean.register(provider, exportedService.getApiDescriptor(), exportedService.getProvidedService());
	}

	public AsterixExportedServiceInfo getExportedService() {
		return exportedService;
	}

	public void setExportedService(AsterixExportedServiceInfo exportedService) {
		this.exportedService = exportedService;
	}

	public AsterixServiceExporterBean getServiceExporterBean() {
		return serviceExporterBean;
	}

	public void setServiceExporterBean(
			AsterixServiceExporterBean serviceExporterBean) {
		this.serviceExporterBean = serviceExporterBean;
	}

	public Object getProvider() {
		return provider;
	}

	public void setProvider(Object provider) {
		this.provider = provider;
	}
	
	

}
