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
package com.avanza.astrix.beans.registry;

import java.util.HashMap;
import java.util.Map;

import com.avanza.astrix.core.AstrixRouting;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceRegistryEntry {
	
	/*
	 * Intentionally designed to avoid typing properties in order to simplify service versioning.
	 */
	
	private Map<String, String> serviceProperties = new HashMap<>();
	private Map<String, String> serviceMetadata = new HashMap<>();
	private String serviceBeanType;
	
	public Map<String, String> getServiceProperties() {
		return serviceProperties;
	}
	
	public void setServiceProperties(Map<String, String> serviceProperties) {
		this.serviceProperties = serviceProperties;
	}
	
	public Map<String, String> getServiceMetadata() {
		return serviceMetadata;
	}
	
	public void setServiceMetadata(Map<String, String> serviceMetadata) {
		this.serviceMetadata = serviceMetadata;
	}
	
	@AstrixRouting
	public String getServiceBeanType() {
		return serviceBeanType;
	}
	
	public void setServiceBeanType(String serviceBeanType) {
		this.serviceBeanType = serviceBeanType;
	}

	public static AstrixServiceRegistryEntry template() {
		AstrixServiceRegistryEntry result = new AstrixServiceRegistryEntry();
		result.serviceProperties = null;
		result.serviceMetadata = null;
		return result;
	}
	
}
