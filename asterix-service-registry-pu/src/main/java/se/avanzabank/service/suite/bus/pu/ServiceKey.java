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

import java.util.Objects;

public class ServiceKey {
	
	private String apiClassName;
	private String qualifier;

	public ServiceKey(String apiClassName, String qualifier) {
		this.apiClassName = Objects.requireNonNull(apiClassName);
		if (qualifier == null) {
			this.qualifier = "-";
		} else {
			this.qualifier = qualifier;
		}
	}
	
	public ServiceKey(String apiClassName) {
		this(apiClassName, "-");
	}

	@Override
	public int hashCode() {
		return Objects.hash(apiClassName, qualifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ServiceKey other = (ServiceKey) obj;
		return Objects.equals(apiClassName, other.apiClassName) 
				&& Objects.equals(qualifier, other.qualifier);
	}
	
	@Override
	public String toString() {
		return apiClassName + "|" + qualifier;
	}
	
	
	
	

}
