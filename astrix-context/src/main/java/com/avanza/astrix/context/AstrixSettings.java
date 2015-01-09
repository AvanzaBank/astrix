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

import com.avanza.astrix.config.DynamicConfigSource;
import com.avanza.astrix.config.DynamicPropertyListener;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.provider.core.AstrixPluginQualifier;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixSettings implements AstrixExternalConfig, DynamicConfigSource {
	
	public static final String BEAN_BIND_ATTEMPT_INTERVAL = "StatefulAstrixBean.beanBindAttemptInterval";
	/**
	 * @deprecated - replaced by BEAN_BIND_ATTEMPT_INTERVAL
	 */
	@Deprecated
	public static final String BEAN_REBIND_ATTEMPT_INTERVAL = BEAN_BIND_ATTEMPT_INTERVAL;
	public static final String SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL = "AstrixServiceLeaseManager.leaseRenewInterval";
	public static final String ENFORCE_SUBSYSTEM_BOUNDARIES = "AstrixContextImpl.enforceSubsystemBoundaries";
	/**
	 * @deprecated AstrixExternalConfigPlugin is replaced by AstrixConfigPlugin and corresponding
	 * {@link AstrixSettings#ASTRIX_CONFIG_PLUGIN_SETTINGS} property.
	 */
	@Deprecated
	public static final String ASTRIX_CONFIG_URI = "AstrixConfig.uri";
	public static final String ASTRIX_CONFIG_PLUGIN_SETTINGS = "AstrixConfigPlugin.settings";
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
	
	public static final String SUBSYSTEM_NAME = "AstrixContextImpl.subsystem";
	
	public static final String ENABLE_FAULT_TOLERANCE = "AstrixContextImpl.enableFaultTolerance";
	public static final String ENABLE_VERSIONING = "AstrixContextImpl.enableVersioning";
	@Deprecated
	public static final String EXPORT_GIGASPACE = "AstrixGsComponent.exportGigaSpace";
	public static final String GIGA_SPACE_BEAN_NAME = "AstrixGsComponent.gigaSpaceBeanName";
	
	private final MapConfigSource config = new MapConfigSource();
	private final String serviceId;
	
	public AstrixSettings() {
		this.serviceId = AstrixDirectComponent.register(AstrixExternalConfig.class, this);
	}
	
	/**
	 * Returns a uri that can be used to use this instance of AstrixSettings as an external config provider.
	 * NOTE: This does not retrieve a setting in this settings. It returns a uri to THIS instance.  
	 * 
	 * @return
	 */
	public final String getExternalConfigUri() {
		return AstrixSettingsExternalConfigPlugin.class.getAnnotation(AstrixPluginQualifier.class).value() + ":" + this.serviceId;
	}
	
	public final void setServiceRegistryUri(String serviceRegistryUri) {
		set(ASTRIX_SERVICE_REGISTRY_URI, serviceRegistryUri);
	}

	public final void set(String settingName, long value) {
		this.config.set(settingName, Long.toString(value));
	}
	
	public final void set(String settingName, String value) {
		this.config.set(settingName, value);
	}
	
	public final void remove(String settingName) {
		this.config.set(settingName, null);
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
			this.config.set(setting.getKey(), setting.getValue());
		}
	}

	public final void set(String settingName, boolean value) {
		this.config.set(settingName, Boolean.toString(value));
	}

	public final String getString(String name) {
		Object result = this.config.get(name);
		if (result == null) {
			return null;
		}
		return result.toString();
	}

	public final void setAll(AstrixSettings settings) {
		this.config.setAll(settings.config);
	}
	
	@Override
	public final String toString() {
		return this.config.toString();
	}

	public final String get(String settingName) {
		return this.config.get(settingName);
	}

	@Override
	public final String lookup(String name) {
		return getString(name);
	}
	
	@Override
	public String get(String propertyName,
			DynamicPropertyListener<String> propertyChangeListener) {
		return this.config.get(propertyName, propertyChangeListener);
	}
	

}
