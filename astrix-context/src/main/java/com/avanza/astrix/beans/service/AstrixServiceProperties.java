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
package com.avanza.astrix.beans.service;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixServiceProperties implements Serializable {
	
	private static final long serialVersionUID = 1L;
	
	public static final String QUALIFIER = "_qualifier";
	public static final String API = "_api";
	public static final String COMPONENT = "_component";
	public static final String SUBSYSTEM = "_subsystem";
	public static final String APPLICATION_INSTANCE_ID = "_applicationInstanceId";
	public static final String SERVICE_STATE = "_serviceState"; 
	
	private final Map<String, String> properties = new HashMap<>();
	
	public AstrixServiceProperties(Map<String, String> serviceProperties) {
		this.properties.putAll(serviceProperties);
	}
	
	public AstrixServiceProperties() {
	}

	public String getProperty(String name) {
		return this.properties.get(name);
	}
	
	public Map<String, String> getProperties() {
		return properties;
	}
	
	public void setProperty(String name, String value) {
		this.properties.put(name, value);
	}
	
	public void setApi(Class<?> api) {
		setProperty(API, api.getName());
	}
	
	public Class<?> getApi() {
		try {
			return Class.forName(getProperty(API));
		} catch (ClassNotFoundException e) {
			throw new RuntimeException(e);
		}
				
	}
	
	public void setQualifier(String qualifier) {
		setProperty(QUALIFIER, qualifier);
	}
	
	public String getQualifier() {
		return getProperty(QUALIFIER);
	}
	
	@Override
	public String toString() {
		return properties.toString();
	}

	public String getComponent() {
		return getProperty(COMPONENT);
	}
	
	public void setComponent(String component) {
		setProperty(COMPONENT, component);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.properties);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AstrixServiceProperties other = (AstrixServiceProperties) obj;
		return Objects.equals(this.properties, other.properties);
	}

}
