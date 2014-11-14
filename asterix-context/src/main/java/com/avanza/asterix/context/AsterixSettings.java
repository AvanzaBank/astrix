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
package com.avanza.asterix.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.avanza.asterix.provider.core.AsterixPluginQualifier;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixSettings implements AsterixExternalConfig {
	
	public static final String BEAN_REBIND_ATTEMPT_INTERVAL = "StatefulAsterixBean.beanRebindAttemptInterval";
	public static final String SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL = "AsterixServiceRegistryLeaseManager.leaseRenewInterval";
	public static final String ENFORCE_SUBSYSTEM_BOUNDARIES = "AsterixContext.enforceSubsystemBoundaries";
	public static final String ASTERIX_CONFIG_URI = "AsterixConfig.uri";
	@Deprecated
	public static final String ASTERIX_CONFIG_URL = ASTERIX_CONFIG_URI;
	public static final String ASTERIX_SERVICE_REGISTRY_URI = "AsterixServiceRegistry.serviceUri";
	/**
	 * All services provided will be registered in the service-registry on a regular interval. This Setting defines
	 * the interval (in millis) on which provided services will be registered in the service-registry
	 * 
	 * Defaults to 30 seconds (30 000 ms)
	 */
	public static final String SERVICE_REGISTRY_EXPORT_INTERVAL = "AsterixServiceRegistryExporterWorker.exportIntervalMillis";
	
	/**
	 * When registration in the service registry fails, there is an option to wait a shorter time then the regular time 
	 * defined by {@link #SERVICE_REGISTRY_EXPORT_INTERVAL} 
	 * 
	 * Defaults to 5 seconds (5 000 ms)
	 */
	public static final String SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL = "AsterixServiceRegistryExporterWorker.retryIntervallMillis";
	public static final String SERVICE_REGISTRY_LEASE = "AsterixServiceRegistryExporterWorker.serviceLeaseTimeMillis";
	public static final String API_DESCRIPTOR_SCANNER_BASE_PACKAGE = "AsterixApiDescriptorScanner.basePackage";
	
	private final Map<String, String> settings = new ConcurrentHashMap<>();
	private String serviceId;
	
	public AsterixSettings() {
		this.serviceId = AsterixDirectComponent.register(AsterixExternalConfig.class, this);
	}

	public final String getExternalConfigUrl() {
		return AsterixSettingsExternalConfigPlugin.class.getAnnotation(AsterixPluginQualifier.class).value() + ":" + this.serviceId;
	}
	
	public final void setExternalConfigUrl(String asterixConfigUrl) {
		set(ASTERIX_CONFIG_URI, asterixConfigUrl);
	}
	
	public final String getAsterixConfigUrl() {
		return getString(ASTERIX_CONFIG_URI);
	}
	
	public final void setServiceRegistryUri(String serviceRegistryUri) {
		set(ASTERIX_SERVICE_REGISTRY_URI, serviceRegistryUri);
	}
	
	public final String getServiceRegistryUri() {
		return getString(ASTERIX_SERVICE_REGISTRY_URI);
	}

	public final void set(String settingName, long value) {
		this.settings.put(settingName, Long.toString(value));
	}
	
	public final void set(String settingName, String value) {
		this.settings.put(settingName, value);
	}

	public static AsterixSettings from(Map<String, String> settings) {
		AsterixSettings result = new AsterixSettings();
		for (Map.Entry<String, String> setting : settings.entrySet()) {
			result.set(setting.getKey(), setting.getValue());
		}
		return result;
	}

	public final void setAll(Map<String, String> settings) {
		for (Map.Entry<String, String> setting : settings.entrySet()) {
			this.settings.put(setting.getKey(), setting.getValue());
		}
	}

	public final void set(String settingName, boolean value) {
		this.settings.put(settingName, Boolean.toString(value));
	}

	public final String getString(String name) {
		Object result = this.settings.get(name);
		if (result == null) {
			return null;
		}
		return result.toString();
	}

	public final void setAll(AsterixSettings settings) {
		this.settings.putAll(settings.settings);
	}
	
	@Override
	public final String toString() {
		return this.settings.toString();
	}

	public final String get(String settingName) {
		return this.settings.get(settingName);
	}

	@Override
	public final String lookup(String name) {
		return getString(name);
	}
	

}
