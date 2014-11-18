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

import static org.junit.Assert.*;

import org.junit.Test;

import com.avanza.astrix.context.AsterixSettings;
import com.avanza.astrix.context.AsterixSettingsReader;



public class AsterixSettingsReaderTest {

	AsterixSettings settings = new AsterixSettings();
	AsterixSettings externalConfig = new AsterixSettings();
	AsterixSettingsReader asterixSettingsReader = new AsterixSettingsReader(settings, externalConfig, "META-INF/asterix/settings_test.properties");
	
	@Test
	public void externalConfigTakesHighestPrecedence() throws Exception {
		settings.set("mySetting", "programaticSettingsValue");
		settings.set("mySetting", "externalConfigValue");
		String configUrl = asterixSettingsReader.getString("mySetting", "lastFallbackValue");
		assertEquals("externalConfigValue", configUrl);
	}
	
	@Test
	public void programaticSettingsTakeSecondHighestPrecedence() throws Exception {
		settings.set("mySetting", "programaticSettingsValue");
		String configUrl = asterixSettingsReader.getString("mySetting", "lastFallbackValue");
		assertEquals("programaticSettingsValue", configUrl);
	}
	
	@Test
	public void classpathOverrideTakeThirdHighestPrecedence() throws Exception {
		String configUrl = asterixSettingsReader.getString("mySetting", "lastFallbackValue");
		assertEquals("mySettingClasspathValue", configUrl);
	}
	
	@Test
	public void usesLastFallbackValueIfSettingNotDefinedInAnyOtherConfigurationSource() throws Exception {
		String configUrl = asterixSettingsReader.getString("anotherSetting", "lastFallbackValue");
		assertEquals("lastFallbackValue", configUrl);
	}

}
