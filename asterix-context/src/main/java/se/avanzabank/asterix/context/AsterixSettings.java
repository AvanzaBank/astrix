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
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixSettings {
	
	// TODO: rename to AsterixExternalConfig and rename current AsterixExternalConfig to ExternalAsterixConfig
	
	public static final String BEAN_REBIND_ATTEMPT_INTERVAL = "StatefulAsterixBean.beanRebindAttemptInterval";
	public static final String SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL = "AsterixServiceRegistryLeaseManager.leaseRenewInterval";
	public static final String ENFORCE_SUBSYSTEM_BOUNDARIES = "AsterixContext.enforceSubsystemBoundaries";
	public static final String ASTERIX_CONFIG_URL = "AsterixConfig.url";
	
	private final Map<String, Object> settings = new ConcurrentHashMap<>();
	
	public void setAsterixConfigUrl(String asterixConfigUrl) {
		set(ASTERIX_CONFIG_URL, asterixConfigUrl);
	}
	
	public String getAsterixConfigUrl() {
		return getString(ASTERIX_CONFIG_URL);
	}
	
	public long getLong(String settingsName, long deafualtValue) {
		Object value = settings.get(settingsName);
		if (value == null) {
			return deafualtValue;
		}
		if (value instanceof String) {
			return Long.parseLong((String)value);
		}
		return Long.class.cast(value).longValue();
	}
	
	public boolean getBoolean(String settingsName, boolean deafualtValue) {
		Object value = settings.get(settingsName);
		if (value == null) {
			return deafualtValue;
		}
		if (value instanceof String) {
			return Boolean.parseBoolean((String)value);
		}
		return Boolean.class.cast(value);
	}

	public void set(String settingName, long value) {
		this.settings.put(settingName, Long.valueOf(value));
	}
	
	public void set(String settingName, Properties value) {
		this.settings.put(settingName, value);
	}
	
	public void set(String settingName, String value) {
		this.settings.put(settingName, value);
	}

	public static AsterixSettings from(Map<String, String> settings) {
		AsterixSettings result = new AsterixSettings();
		for (Map.Entry<String, String> setting : settings.entrySet()) {
			result.set(setting.getKey(), setting.getValue());
		}
		return result;
	}

	public void setAll(Map<String, String> settings) {
		for (Map.Entry<String, String> setting : settings.entrySet()) {
			this.settings.put(setting.getKey(), setting.getValue());
		}
	}

	public void set(String settingName, boolean value) {
		this.settings.put(settingName, value);
	}

	public String getString(String name) {
		Object result = this.settings.get(name);
		if (result == null) {
			return null;
		}
		return result.toString();
	}

	public void setAll(AsterixSettings settings) {
		this.settings.putAll(settings.settings);
	}
	
	@Override
	public String toString() {
		return this.settings.toString();
	}

	public Object get(String settingName) {
		return this.settings.get(settingName);
	}

}
