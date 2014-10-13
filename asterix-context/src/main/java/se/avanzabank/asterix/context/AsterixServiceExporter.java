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
package se.avanzabank.asterix.context;

import java.util.Collection;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Server side component used to export all services provided by a given server as defined by
 * an {@link AsterixServiceDescriptor}. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceExporter implements ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private AsterixServiceExporterBeans asterixServiceExporterBeans;
	private Collection<AsterixExportedServiceInfo> exportedServices;

	@Autowired
	public AsterixServiceExporter(AsterixServiceExporterBeans asterixServiceExporters) {
		this.asterixServiceExporterBeans = asterixServiceExporters;
	}
	
	public Collection<AsterixExportedServiceInfo> getExportedServices() {
		return exportedServices;
	}
	
	public void setExportedServices(Collection<AsterixExportedServiceInfo> exportedServices) {
		this.exportedServices = exportedServices;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	@PostConstruct
	public void register() {
		for (AsterixExportedServiceInfo exportedService : exportedServices) {
			Object provider = applicationContext.getBean(exportedService.getProvidingBeanName());
			AsterixServiceExporterBean serviceExporterBean = asterixServiceExporterBeans.getServiceExporter(exportedService.getComponentName());
			serviceExporterBean.register(provider, exportedService.getApiDescriptor(), exportedService.getProvidedService());
		}
	}

//	@PostConstruct
//	public void register() {
//		Set<AsterixExportedService> exportedServices = getExportedServices();
//		exportedServices.addAll(getTransitiveRequiredServices(exportedServices));
//		// At this stage all services that are required to be exported are identified
//		for (AsterixExportedService exportedService : getExportedServices()) {
//			AsterixServiceExporterBean serviceExporter = getServiceExporter(exportedService);
//			serviceExporter.register(exportedService.getProvider(), exportedService.getApiDescriptor(), exportedService.getProvidedType());
//		}
//	}

//	private Set<? extends AsterixExportedService> getTransitiveRequiredServices(Set<AsterixExportedService> exportedServices) {
//		Set<AsterixExportedService> result = new HashSet<>();
//		for (AsterixExportedService exportedService : exportedServices) {
//			String componentName = exportedService.getComponentName();
//			// ask 'component' for transitive dependencies
//			// For instance: gs-remoting => GigaSpace.class
//		}
//		return result;
//	}
//
//	private Set<AsterixExportedService> getExportedServices() {
//		/*
//		 * Det är inte garanterat att alla tjänster som måste exporteras är annoterade med AsterixServiceExport. Tex
//		 * Kräver gs-remoting att vi exporterar GigaSpace som en tjänst.
//		 * 
//		 * Vi kan anta att alla explicit exporterade tjänster kommer vara annoterade med AsterixServiceExport. Men
//		 * hur ska vi hantera transistiva beroenden (tex GigaSpace)?
//		 * 
//		 *  Är det AsterixServiceExporterBean som ansvarar för att exportera transistiva beroenden? Eller frågar vi
//		 *  AsterixServiceExporterBean efter transistiva beroenden?
//		 */
//		Set<AsterixExportedService> result = new HashSet<>();
//		for (Object service : applicationContext.getBeansWithAnnotation(AsterixServiceExport.class).values()) {
//			AsterixServiceExport serviceExport = service.getClass().getAnnotation(AsterixServiceExport.class);
//			for (Class<?> providedApi : serviceExport.value()) {
//				if (!providedApi.isAssignableFrom(service.getClass())) {
//					throw new IllegalArgumentException("Cannot export: " + service.getClass() + " as " + providedApi);
//				}
//				String componentName = getComponentName(service, providedApi, getApiDescriptor(providedApi));
//				result.add(new AsterixExportedService(service, providedApi, getApiDescriptor(providedApi), componentName));
//			}
//		}
//		return result;
//	}
//
//	private String getComponentName(Object service, Class<?> providedApi, AsterixApiDescriptor apiDescriptor) {
//		throw new UnsupportedOperationException("Implement me");
//	}
//
//	private AsterixApiDescriptor getApiDescriptor(Class<?> providedApi) {
//		return serviceDescriptor.getApiDescriptor(providedApi);
//	}
//
//	private AsterixServiceExporterBean getServiceExporter(AsterixExportedService service) {
//		return asterixServiceExporterBeans.getServiceExporter(service);
//	}
	

}
