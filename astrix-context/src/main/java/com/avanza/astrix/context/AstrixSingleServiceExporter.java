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
package com.avanza.astrix.context;

import javax.annotation.PostConstruct;

/**
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixSingleServiceExporter {
	
	// TODO: DELETE ME?
	
	private AstrixExportedServiceInfo exportedService;
	private AstrixServiceExporterBean serviceExporterBean;
	private Object provider;

	@PostConstruct
	public void register() {
		// At this stage all services that are required to be exported are identified
		serviceExporterBean.register(provider, exportedService.getApiDescriptor(), exportedService.getProvidedService());
	}

	public AstrixExportedServiceInfo getExportedService() {
		return exportedService;
	}

	public void setExportedService(AstrixExportedServiceInfo exportedService) {
		this.exportedService = exportedService;
	}

	public AstrixServiceExporterBean getServiceExporterBean() {
		return serviceExporterBean;
	}

	public void setServiceExporterBean(
			AstrixServiceExporterBean serviceExporterBean) {
		this.serviceExporterBean = serviceExporterBean;
	}

	public Object getProvider() {
		return provider;
	}

	public void setProvider(Object provider) {
		this.provider = provider;
	}
	
	

}
