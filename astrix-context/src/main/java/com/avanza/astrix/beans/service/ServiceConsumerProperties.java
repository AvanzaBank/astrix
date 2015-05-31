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
package com.avanza.astrix.beans.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceConsumerProperties implements Serializable {

	private static final long serialVersionUID = 1L;
	
	public static final String CONSUMER_ID = "consumerId";
	public static final String CONSUMER_ZONE = "consumerZone";
	
	private Map<String, String> properties = new HashMap<>();
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public void setProperties(Map<String, String> properties) {
		this.properties = properties;
	}

	public String getProperty(String name) {
		return properties.get(name);
	}
	
	public String setProperty(String name, String value) {
		return properties.put(name, value);
	}
	
	@Override
	public String toString() {
		return properties.toString();
	}

}
