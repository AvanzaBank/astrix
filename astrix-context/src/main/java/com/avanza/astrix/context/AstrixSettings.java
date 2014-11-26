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

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.avanza.astrix.provider.core.AstrixPluginQualifier;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixSettings implements AstrixExternalConfig {
	
	public static final String BEAN_REBIND_ATTEMPT_INTERVAL = "StatefulAstrixBean.beanRebindAttemptInterval";
	public static final String SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL = "AstrixServiceRegistryLeaseManager.leaseRenewInterval";
	public static final String ENFORCE_SUBSYSTEM_BOUNDARIES = "AstrixContext.enforceSubsystemBoundaries";
	public static final String ASTRIX_CONFIG_URI = "AstrixConfig.uri";
	public static final String ASTRIX_SERVICE_REGISTRY_URI = "AstrixServiceRegistry.serviceUri";
	
	/**
	 * All services provided will be registered in the service-registry on a regular interval. This Setting defines
	 * the interval (in millis) on which provided services will be registered in the service-registry
	 * 
	 * Defaults to 30 seconds (30 000 ms)
	 */
	public static final String SERVICE_REGISTRY_EXPORT_INTERVAL = "AstrixServiceRegistryExporterWorker.exportIntervalMillis";
	
	/**
	 * When registration in the service registry fails, there is an option to wait a shorter time then the regular time 
	 * defined by {@link #SERVICE_REGISTRY_EXPORT_INTERVAL} 
	 * 
	 * Defaults to 5 seconds (5 000 ms)
	 */
	public static final String SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL = "AstrixServiceRegistryExporterWorker.retryIntervallMillis";
	
	public static final String SERVICE_REGISTRY_LEASE = "AstrixServiceRegistryExporterWorker.serviceLeaseTimeMillis";
	public static final String API_DESCRIPTOR_SCANNER_BASE_PACKAGE = "AstrixApiDescriptorScanner.basePackage";
	
	public static final String SUBSYSTEM_NAME = "AstrixContext.subsystem";
	
	public static final String ENABLE_FAULT_TOLERANCE = "AstrixContext.enableFaultTolerance";
	public static final String ENABLE_VERSIONING = "AstrixContext.enableVersioning";
	public static final String ENABLE_MONITORING = "AstrixContext.enableMonitoring";
	public static final String EXPORT_GIGASPACE = "AstrixGsComponent.exportGigaSpace";
	
	private final Map<String, String> settings = new ConcurrentHashMap<>();
	private final String serviceId;
	
	public AstrixSettings() {
		this.serviceId = AstrixDirectComponent.register(AstrixExternalConfig.class, this);
	}

	/**
	 * @deprecated replaces by getExternalConfigUri
	 * @return
	 */
	@Deprecated
	public final String getExternalConfigUrl() {
		return getExternalConfigUri();
	}
	
	public final String getExternalConfigUri() {
		return AstrixSettingsExternalConfigPlugin.class.getAnnotation(AstrixPluginQualifier.class).value() + ":" + this.serviceId;
	}
	
	public final String getAstrixConfigUrl() {
		return getString(ASTRIX_CONFIG_URI);
	}
	
	public final void setServiceRegistryUri(String serviceRegistryUri) {
		set(ASTRIX_SERVICE_REGISTRY_URI, serviceRegistryUri);
	}
	
	public final String getServiceRegistryUri() {
		return getString(ASTRIX_SERVICE_REGISTRY_URI);
	}

	public final void set(String settingName, long value) {
		this.settings.put(settingName, Long.toString(value));
	}
	
	public final void set(String settingName, String value) {
		this.settings.put(settingName, value);
	}

	public static AstrixSettings from(Map<String, String> settings) {
		AstrixSettings result = new AstrixSettings();
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

	public final void setAll(AstrixSettings settings) {
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
