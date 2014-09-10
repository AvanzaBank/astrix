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

import java.util.Objects;

public class AsterixBeanKey {
	
	private String beanType;
	private String qualifier;

	private AsterixBeanKey(Class<?> beanType, String qualifier) {
		this.beanType = beanType.getName();
		this.qualifier = qualifier;
	}
	
	public static AsterixBeanKey create(Class<?> beanType, String qualifier) {
		if (qualifier == null) {
			return new AsterixBeanKey(beanType, "-");
		} 
		return new AsterixBeanKey(beanType, qualifier);
	}

	@Override
	public int hashCode() {
		return Objects.hash(beanType, qualifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		AsterixBeanKey other = (AsterixBeanKey) obj;
		return this.beanType.equals(other.beanType) && this.qualifier.equals(other.qualifier);
	}
	
	@Override
	public String toString() {
		return beanType + "-" + beanType;
	}
	
	

}
