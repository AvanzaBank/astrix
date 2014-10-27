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

import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import se.avanzabank.asterix.provider.core.AsterixPluginQualifier;

public class InMemoryAsterixConfig implements AsterixExternalConfig {

	private final Map<String, Properties> settings = new ConcurrentHashMap<>();
	private String locator;
	
	public InMemoryAsterixConfig() {
		this.locator = AsterixDirectComponent.register(AsterixExternalConfig.class, this);
	}
	
	public final void set(String name,Properties properties) {
		this.settings.put(name, properties);
	}
	
	@Override
	public Properties lookup(String name) {
		return this.settings.get(name);
	}
	
	public String getConfigUrl() {
		return InMemoryAsterixConfigPlugin.class.getAnnotation(AsterixPluginQualifier.class).value() + ":" + this.locator;
	}
	
	@Override
	public String toString() {
		return "InMemory locator: " + locator + " settings: " + settings;
	}

}
