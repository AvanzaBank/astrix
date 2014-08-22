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
package se.avanzabank.service.suite.bus.pu;

import com.gigaspaces.annotation.pojo.SpaceId;
import com.gigaspaces.annotation.pojo.SpaceRouting;

import se.avanzabank.service.suite.bus.client.AstrixServiceProperties;

public class ServiceProperitesInfo {

	private AstrixServiceProperties properties = new AstrixServiceProperties();
	private ServiceKey serviceKey;
	private Class<?> apiType;

	public AstrixServiceProperties getProperties() {
		return properties;
	}

	public void setProperties(AstrixServiceProperties properties) {
		this.properties = properties;
	}
	
	@SpaceId(autoGenerate = false)
	public ServiceKey getServiceKey() {
		return serviceKey;
	}
	
	public void setServiceKey(ServiceKey serviceKey) {
		this.serviceKey = serviceKey;
	}
	
	@SpaceRouting
	public Class<?> getApiType() {
		return apiType; 
	}
	
	public void setApiType(Class<?> apiType) {
		this.apiType = apiType;
	}

}
