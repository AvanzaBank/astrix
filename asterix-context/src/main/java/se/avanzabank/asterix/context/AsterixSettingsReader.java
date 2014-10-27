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

import java.util.Properties;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixSettingsReader {

	private AsterixSettings settings;
	private AsterixExternalConfig externalConfig;
	
	public AsterixSettingsReader(AsterixSettings settings, AsterixExternalConfig externalConfig) {
		this.settings = settings;
		this.externalConfig = externalConfig;
	}
	
	static AsterixSettingsReader create(AsterixPlugins plugins, AsterixSettings settings) {
		String configUrl = settings.getAsterixConfigUrl();
		if (configUrl == null) {
			return new AsterixSettingsReader(settings, new InMemoryAsterixConfig());
		}
		int splitAt = configUrl.indexOf(":");
		String configPlugin = configUrl;
		String locator = "";
		if (splitAt > 0) {
			configPlugin = configUrl.substring(0, splitAt);
			locator = configUrl.substring(splitAt + 1);
		}
		AsterixExternalConfig externalConfig = plugins.getPlugin(AsterixExternalConfigPlugin.class, configPlugin).getConfig(locator);
		return new AsterixSettingsReader(settings, externalConfig);
	}

	public long getLong(String settingsName, long deafualtValue) {
		Object value = get(settingsName);
		if (value == null) {
			return deafualtValue;
		}
		if (value instanceof String) {
			return Long.parseLong((String)value);
		}
		return Long.class.cast(value).longValue();
	}

	public boolean getBoolean(String settingsName, boolean deafualtValue) {
		Object value = get(settingsName);
		if (value == null) {
			return deafualtValue;
		}
		if (value instanceof String) {
			return Boolean.parseBoolean((String)value);
		}
		return Boolean.class.cast(value);
	}
	
	public String getString(String name) {
		Object result = get(name);
		if (result == null) {
			return null;
		}
		return result.toString();
	}

	private Object get(String settingName) {
		Properties setting = externalConfig.lookup(settingName);
		if (setting != null) {
			return setting;
		}
		return settings.get(settingName);
	}

	public Properties getProperties(String name) {
		Object properties = get(name);
		if (properties == null) {
			return null;
		}
		return Properties.class.cast(properties);
	}

}