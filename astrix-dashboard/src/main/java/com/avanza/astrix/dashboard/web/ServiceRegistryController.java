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
package com.avanza.astrix.dashboard.web;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.avanza.astrix.beans.registry.AstrixServiceRegistryEntry;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.service.registry.client.AstrixServiceRegistryAdministrator;

@RestController
@RequestMapping("/services")
public class ServiceRegistryController {
	
	private AstrixServiceRegistryAdministrator serviceRegistryAdmin;
	
	@Autowired
	public ServiceRegistryController(AstrixServiceRegistryAdministrator serviceRegistryAdmin) {
		this.serviceRegistryAdmin = Objects.requireNonNull(serviceRegistryAdmin);
	}

	@RequestMapping(value = "/", method = RequestMethod.GET)
	public List<ServiceData> services() {
		List<AstrixServiceRegistryEntry> services = serviceRegistryAdmin.listServices();
		List<ServiceData> result = new ArrayList<>();
		for (AstrixServiceRegistryEntry entry : services) {
			ServiceData s = new ServiceData();
			s.setProvidedApi(entry.getServiceBeanType());
			s.setServiceMetadata(entry.getServiceMetadata());
			s.setServiceProperties(entry.getServiceProperties());
			result.add(s);
		}
		return result;
	}
	
	@RequestMapping(value = "/summary", method = RequestMethod.GET)
	public List<ServiceDataSummary> servicesSummary() {
		List<AstrixServiceRegistryEntry> services = serviceRegistryAdmin.listServices();
		List<ServiceDataSummary> result = new ArrayList<>();
		for (AstrixServiceRegistryEntry entry : services) {
			ServiceProperties serviceProperties = new ServiceProperties(entry.getServiceProperties());
			ServiceDataSummary s = new ServiceDataSummary();
			if (serviceProperties.getQualifier() != null) {
				s.setApiKey(serviceProperties.getProperty(ServiceProperties.API) + ":" + serviceProperties.getQualifier());
			} else {
				s.setApiKey(serviceProperties.getProperty(ServiceProperties.API) + ":-");
			}
			s.setSubsystem(serviceProperties.getProperty(ServiceProperties.SUBSYSTEM));
			StringBuilder serviceUriBuilder = new StringBuilder();
			serviceUriBuilder.append(serviceProperties.getComponent()).append(":");
			boolean prependAmpersand = false;
			for (Map.Entry<String, String> property : serviceProperties.getProperties().entrySet()) {
				if (property.getKey().startsWith("_")) {
					continue;
				}
				if (prependAmpersand) {
					serviceUriBuilder.append("&");
				} else {
					prependAmpersand = true;
				}
				serviceUriBuilder.append(property.getKey()).append("=").append(property.getValue());
			}
			s.setServiceUri(serviceUriBuilder.toString());
			result.add(s);
		}
		return result;
	}

	public static class ServiceData {
		private String providedApi;
		private Map<String, String> serviceProperties;
		private Map<String, String> serviceMetadata;

		public String getProvidedApi() {
			return providedApi;
		}
		
		public void setServiceMetadata(Map<String, String> serviceMetadata) {
			this.serviceMetadata = serviceMetadata;
		}
		public Map<String, String> getServiceMetadata() {
			return serviceMetadata;
		}

		public void setProvidedApi(String providedApi) {
			this.providedApi = providedApi;
		}

		public void setServiceProperties(Map<String, String> serviceProperties) {
			this.serviceProperties = serviceProperties;
		}
		
		public Map<String, String> getServiceProperties() {
			return serviceProperties;
		}
	}
	
	public static class ServiceDataSummary {
		
		private String apiKey;
		private String subsystem;
		private String serviceUri;
		
		public String getApiKey() {
			return apiKey;
		}
		
		public void setApiKey(String apiKey) {
			this.apiKey = apiKey;
		}
		
		public String getSubsystem() {
			return subsystem;
		}
		
		public void setSubsystem(String subsystem) {
			this.subsystem = subsystem;
		}
		
		public String getServiceUri() {
			return serviceUri;
		}

		public void setServiceUri(String serviceUri) {
			this.serviceUri = serviceUri;
		}
	}

}
