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
package se.avanzabank.service.suite.context;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AstrixContext {
	
	private final ConcurrentMap<Class<?>, List<?>> providersByType = new ConcurrentHashMap<Class<?>, List<?>>();
	
	public <T> T getProvider(Class<T> type) {
		List<T> providers = (List<T>) providersByType.get(type);
		if (providers == null) {
			throw new IllegalArgumentException("No provider registered for type: " + type);
		}
		if (providers.size() != 1) {
			throw new IllegalArgumentException("Expected one provider for: " + type + " fount: + "+ providers);
		}
		return providers.get(0);
	}
	
	public <T> List<T> getProviders(Class<T> type) {
		List<T> providers = (List<T>) providersByType.get(type);
		if (providers == null) {
			throw new IllegalArgumentException("No provider registered for type: " + type);
		}
		return providers;
	}

	public <T> void registerProvider(Class<T> type, T provider) {
		this.providersByType.putIfAbsent(type, new ArrayList<>());
		List<T> providers = (List<T>) this.providersByType.get(type);
		providers.add(provider);
	}

}
