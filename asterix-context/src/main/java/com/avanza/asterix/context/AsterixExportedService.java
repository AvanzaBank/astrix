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

import java.util.Objects;

public class AsterixExportedService {
	
	// TODO: delete?

	private Class<?> providedService;
	private AsterixApiDescriptor apiDescriptor;
	private Object provider;
	private String componentName;

	public AsterixExportedService(Object provider, Class<?> providedService, AsterixApiDescriptor apiDescriptor, String componentName) {
		this.providedService = providedService;
		this.apiDescriptor = apiDescriptor;
		this.provider = provider;
		this.componentName = componentName;
	}

	public Object getProvider() {
		return this.provider;
	}

	public AsterixApiDescriptor getApiDescriptor() {
		return this.apiDescriptor;
	}

	public Class<?> getProvidedType() {
		return this.providedService;
	}
	
	public String getComponentName() {
		return componentName;
	}

	@Override
	public int hashCode() {
		return Objects.hash(providedService.getName());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AsterixExportedService other = (AsterixExportedService) obj;
		return Objects.equals(this.providedService.getName(), other.providedService.getName());
	}
	
	
	
}