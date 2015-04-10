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

import java.io.Serializable;
import java.util.Objects;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class ServiceProviderKey implements Serializable {

	private static final long serialVersionUID = 1L;
	

	private final ServiceKey serviceKey;
	private final String applicationInstanceId;

	private ServiceProviderKey(ServiceKey serviceKey,
							   String applicationInstanceId) {
		this.serviceKey = Objects.requireNonNull(serviceKey);
		this.applicationInstanceId = Objects.requireNonNull(applicationInstanceId);
	}
	
	public static ServiceProviderKey create(ServiceKey serviceKey,
											String applicationInstanceId) {
		return new ServiceProviderKey(serviceKey, applicationInstanceId);
	}
	
	@Override
	public String toString() {
		return serviceKey.toString() + "#" + applicationInstanceId;
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.applicationInstanceId, this.serviceKey);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServiceProviderKey other = (ServiceProviderKey) obj;
		return Objects.equals(this.applicationInstanceId, other.applicationInstanceId) 
				&& Objects.equals(this.serviceKey, other.serviceKey);
	}

}
