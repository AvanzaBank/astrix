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
	
	private final DynamicConfig dynamicConfig;
	
	private AstrixSettingsReader(DynamicConfig config) {
		this.dynamicConfig = config;
	}

	public static AstrixSettingsReader create(AstrixSettings settings) {
		return new AstrixSettingsReader(DynamicConfig.create(settings));
	}

	public static AstrixSettingsReader create(AstrixSettings settings, String defaultSettingsOverrideFile, DynamicConfig dynamicConfig) {
		return new AstrixSettingsReader(DynamicConfig.merged(dynamicConfig, createDefaultConfiguration(settings, defaultSettingsOverrideFile)));
	}
	
	private static DynamicConfig createDefaultConfiguration(AstrixSettings settings, String defaultSettingsOverrideFile) {
		return DynamicConfig.create(settings, PropertiesConfigSource.optionalClasspathPropertiesFile(defaultSettingsOverrideFile));
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
	
	public DynamicLongProperty getLongProperty(String name, long defaultValue) {
		return this.dynamicConfig.getLongProperty(name, defaultValue);
	}

	public static AstrixSettingsReader create(DynamicConfig dynamicConfig) {
		return new AstrixSettingsReader(dynamicConfig);
	}
	
	@Override
	public String toString() {
		return this.dynamicConfig.toString();
	}

}