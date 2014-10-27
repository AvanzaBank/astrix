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


public final class AsterixConfigLocator {
	
	private AsterixPlugins plugins;
	private String configUrl;
	

	public AsterixConfigLocator(AsterixPlugins plugins, AsterixSettings settings) {
		this.plugins = plugins;
		this.configUrl = settings.getString(AsterixSettings.ASTERIX_CONFIG_URL);
	}

	public AsterixExternalConfig getConfig() {
		if (configUrl == null) {
			return new InMemoryAsterixConfig();
		}
		int splitAt = configUrl.indexOf(":");
		String configPlugin = this.configUrl;
		String locator = "";
		if (splitAt > 0) {
			configPlugin = configUrl.substring(0, splitAt);
			locator = configUrl.substring(splitAt + 1);
		}
		return plugins.getPlugin(AsterixExternalConfigPlugin.class, configPlugin).getConfig(locator);
	}
	
}
