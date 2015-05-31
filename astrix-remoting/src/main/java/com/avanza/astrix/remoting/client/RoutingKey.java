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
package com.avanza.astrix.remoting.client;

import java.io.Serializable;
/**
 * To avoid serializing the routing key object we use this as a placeholder for the hash of the real routing key. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class RoutingKey implements Serializable {
	
	private static final long serialVersionUID = 1L;
	private final int hash;

	private RoutingKey(int hash) {
		this.hash = hash;
	}
	
	public static RoutingKey create(Object object) {
		return new RoutingKey(object.hashCode());
	}
	
	public static RoutingKey create(int hash) {
		return new RoutingKey(hash);
	}
	
	@Override
	public int hashCode() {
		return this.hash;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		RoutingKey other = (RoutingKey) obj;
		if (hash != other.hash)
			return false;
		return true;
	}
	
	@Override
	public String toString() {
		return this.hash + "";
	}

}
