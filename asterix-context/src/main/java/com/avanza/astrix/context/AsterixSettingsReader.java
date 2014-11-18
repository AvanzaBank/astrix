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
package com.avanza.astrix.context;

import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixSettingsReader {

	private final Logger log = LoggerFactory.getLogger(AsterixSettingsReader.class);
	
	private AsterixSettings settings;
	private AsterixExternalConfig externalConfig;
	private Properties classpathOverride = new Properties();
	
	private AsterixSettingsReader(AsterixSettings settings, AsterixExternalConfig externalConfig) {
		this(settings, externalConfig, "META-INF/asterix/settings.properties");
	}
	
	AsterixSettingsReader(AsterixSettings settings, AsterixExternalConfig externalConfig, String defaultSettingsOverrideFile) {
		this.settings = settings;
		this.externalConfig = externalConfig;
		try {
			InputStream resourceAsStream = getClass().getClassLoader().getResourceAsStream(defaultSettingsOverrideFile);
			if (resourceAsStream == null) {
				log.info("No asterix classpath-override settings found. (file: " + defaultSettingsOverrideFile + ")");
				return;
			}
			classpathOverride.load(resourceAsStream);
		} catch (Exception e) {
			log.warn("Failed to load classpath-override settings for asterix from file: " + defaultSettingsOverrideFile);
		}
	}
	
	static AsterixSettingsReader create(AsterixPlugins plugins, AsterixSettings settings) {
		/*
		 *  NOTE: 
		 *  This behavior might look weird. We must create a AsterixSettingsReader too lookup the ASTERIX_CONFIG_URI setting
		 *  in order to create the AsterixExternalConfig. The reason is that we want the same chain of lookup
		 *  to take place event when reading the external_config_url.
		 */
		String configUrl = new AsterixSettingsReader(settings, settings).getString(AsterixSettings.ASTERIX_CONFIG_URI);
		if (configUrl == null) {
			return new AsterixSettingsReader(settings, new AsterixSettings());
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
		String value = get(settingsName);
		if (value == null) {
			return deafualtValue;
		}
		return Long.parseLong(value);
	}

	public boolean getBoolean(String settingsName, boolean deafualtValue) {
		String value = get(settingsName);
		if (value == null) {
			return deafualtValue;
		}
		return Boolean.parseBoolean(value);
	}
	
	public String getString(String name) {
		return getString(name, null);
	}
	
	public String getString(String name, String defaultValue) {
		String result = get(name);
		if (result == null) {
			return defaultValue;
		}
		return result;
	}

	private String get(String settingName) {
		String setting = externalConfig.lookup(settingName);
		if (setting != null) {
			log.trace("Resolved setting using external config: name={} value={}", settingName, setting);
			return setting;
		}
		setting = settings.get(settingName);
		if (setting != null) {
			log.trace("Resolved setting using external config: name={} value={}", settingName, setting);
			return setting;
		}
		setting = this.classpathOverride.getProperty(settingName);
		if (setting != null) {
			log.trace("Resolved setting using classpathOverride: name={} value={}", settingName, setting);
		}
		return setting;
	}

}