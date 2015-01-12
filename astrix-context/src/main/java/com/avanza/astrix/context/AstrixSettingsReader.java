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

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.config.ConfigSource;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.config.PropertiesConfigSource;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@Deprecated
public class AstrixSettingsReader {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixSettingsReader.class);
	private static final Pattern CONFIG_URI_PATTERN = Pattern.compile("([^:]*)(:(.*))?");
	private final DynamicConfig dynamicConfig;
	
	@Deprecated
	private AstrixSettingsReader(AstrixSettings settings, List<? extends ConfigSource> externalConfigSources) {
		this(settings, externalConfigSources, "META-INF/astrix/settings.properties");
	}
	
	@Deprecated
	AstrixSettingsReader(AstrixSettings settings, AstrixExternalConfig externalConfig, String defaultSettingsOverrideFile) {
		this(settings, Arrays.asList(new ExternalConfigAdapter(externalConfig)), defaultSettingsOverrideFile);
	}
	

	@Deprecated
	AstrixSettingsReader(AstrixSettings settings, List<? extends ConfigSource> externalConfigSources, String defaultSettingsOverrideFile) {
		this.dynamicConfig = DynamicConfig.merged(DynamicConfig.create(externalConfigSources), createDefaultConfiguration(settings, defaultSettingsOverrideFile));
	}
	
	static DynamicConfig createDefaultConfiguration(AstrixSettings settings, String defaultSettingsOverrideFile) {
		return DynamicConfig.create(settings, PropertiesConfigSource.optionalClasspathPropertiesFile(defaultSettingsOverrideFile));
	}
	
	private AstrixSettingsReader(DynamicConfig config) {
		this.dynamicConfig = config;
	}

	public static AstrixSettingsReader create(AstrixSettings settings) {
		return new AstrixSettingsReader(DynamicConfig.create(settings));
	}

	public static AstrixSettingsReader create(AstrixSettings settings, String defaultSettingsOverrideFile, DynamicConfig dynamicConfig) {
		return new AstrixSettingsReader(DynamicConfig.merged(dynamicConfig, createDefaultConfiguration(settings, defaultSettingsOverrideFile)));
	}
	
	static AstrixSettingsReader create(AstrixPlugins plugins, AstrixSettings settings) {
		/*
		 *  NOTE: 
		 *  This behavior might look weird. We must create a AstrixSettingsReader too lookup the ASTRIX_CONFIG_URI setting
		 *  in order to create the AstrixExternalConfig. The reason is that we want the same chain of lookup
		 *  to take place even when reading the external_config_url.
		 */
		String configUrl = new AstrixSettingsReader(settings, Arrays.asList(settings)).getString(AstrixSettings.ASTRIX_CONFIG_URI);
		if (configUrl == null) {
			return new AstrixSettingsReader(settings, Arrays.asList(new AstrixSettings()));
		}
		Matcher matcher = CONFIG_URI_PATTERN.matcher(configUrl);
		matcher.find();
		String externalConfigPluginName = matcher.group(1);
		String optionalPluginSettings = matcher.group(3);
		AstrixExternalConfig externalConfig = plugins.getPlugin(AstrixExternalConfigPlugin.class, externalConfigPluginName).getConfig(optionalPluginSettings);
		return new AstrixSettingsReader(settings, Arrays.asList(new ExternalConfigAdapter(externalConfig)));
	}
	
	public boolean getBoolean(String settingsName, boolean defaultValue) {
		return this.dynamicConfig.getBooleanProperty(settingsName, defaultValue).get();
	}
	
	public String getString(String name) {
		return getString(name, null);
	}
	
	public String getString(String name, String defaultValue) {
		return this.dynamicConfig.getStringProperty(name, defaultValue).get();
	}

	public DynamicBooleanProperty getBooleanProperty(String name, boolean defaultValue) {
		boolean property = getBoolean(name, defaultValue);
		return this.dynamicConfig.getBooleanProperty(name, property);
	}
	
	private static class ExternalConfigAdapter implements ConfigSource {
		private final AstrixExternalConfig externalConfig;

		public ExternalConfigAdapter(AstrixExternalConfig externalConfig) {
			this.externalConfig = externalConfig;
		}

		@Override
		public String get(String propertyName) {
			return externalConfig.lookup(propertyName);
		}
	}

	public DynamicLongProperty getLongProperty(String name, long defaultValue) {
		return this.dynamicConfig.getLongProperty(name, defaultValue);
	}

	public static AstrixSettingsReader create(DynamicConfig dynamicConfig) {
		return new AstrixSettingsReader(dynamicConfig);
	}

}