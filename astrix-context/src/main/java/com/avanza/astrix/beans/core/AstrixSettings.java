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
package com.avanza.astrix.beans.core;

import java.util.Map;

import com.avanza.astrix.config.DynamicConfigSource;
import com.avanza.astrix.config.DynamicPropertyListener;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixSettings implements DynamicConfigSource {
	/**
	 * Defines how long to wait between consecutive bind attempts when a service bean is
	 * in UNBOUND state.
	 */
	public static final String BEAN_BIND_ATTEMPT_INTERVAL = "StatefulAstrixBeanInstance.beanBindAttemptInterval";

	/**
	 * Defines how long the service-lease-manager will wait between consecutive lease renewals
	 * for a service bean that is in BOUND sate.
	 */
	public static final String SERVICE_LEASE_RENEW_INTERVAL = "AstrixServiceLeaseManager.leaseRenewInterval";
	
	public static final String ENFORCE_SUBSYSTEM_BOUNDARIES = "AstrixContext.enforceSubsystemBoundaries";
	
	/**
	 * @deprecated Renamed to {@link AstrixSettings#SERVICE_REGISTRY_URI}
	 */
	@Deprecated
	public static final String ASTRIX_SERVICE_REGISTRY_URI = "AstrixServiceRegistry.serviceUri";
	
	/**
	 * Service Uri used to bind to the service-registry.
	 */
	public static final String SERVICE_REGISTRY_URI = "AstrixServiceRegistry.serviceUri";
	
	/**
	 * All services provided will be registered in the service-registry on a regular interval. This Setting defines
	 * the interval (in millis) on which provided services will be registered in the service-registry
	 * 
	 * Defaults to 30 seconds (30 000 ms)
	 */
	public static final String SERVICE_REGISTRY_EXPORT_INTERVAL = "ServiceRegistryExporterWorker.exportIntervalMillis";
	
	/**
	 * When registration in the service registry fails, there is an option to wait a shorter time then the regular time 
	 * defined by {@link #SERVICE_REGISTRY_EXPORT_INTERVAL} 
	 * 
	 * Defaults to 5 seconds (5 000 ms)
	 */
	public static final String SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL = "ServiceRegistryExporterWorker.retryIntervallMillis";
	
	public static final String SERVICE_REGISTRY_LEASE = "ServiceRegistryExporterWorker.serviceLeaseTimeMillis";
	/**
	 * @renamed to {@link #API_PROVIDER_SCANNER_BASE_PACKAGE}  
	 */
	@Deprecated
	public static final String API_DESCRIPTOR_SCANNER_BASE_PACKAGE = "AstrixApiDescriptorScanner.basePackage";
	/**
	 * Defines the basePackage(s) to scan when searching for ApiProvider's. The given package(s)
	 * and all subpackages will be scanned.
	 * 
	 * The packages should be separated by comma (",").
	 */
	public static final String API_PROVIDER_SCANNER_BASE_PACKAGE = "AstrixApiProviderScanner.basePackage";
	
	public static final String SUBSYSTEM_NAME = "AstrixContext.subsystem";
	
	public static final String ENABLE_FAULT_TOLERANCE = "AstrixContext.enableFaultTolerance";
	public static final String ENABLE_VERSIONING = "AstrixContext.enableVersioning";
	public static final String GIGA_SPACE_BEAN_NAME = "AstrixGsComponent.gigaSpaceBeanName";
	public static final String DYNAMIC_CONFIG_FACTORY = "com.avanza.astrix.context.AstrixDynamicConfigFactory";
	/**
	 * When local view is disabled every service that is exported using local-view
	 * ({@link AstrixServiceComponentNames#GS_LOCAL_VIEW}) will use a regular clustered proxy instead.
	 */
	public static final String GS_DISABLE_LOCAL_VIEW = "AstrixGsLocalViewComponent.disableLocalView";
	public static final String DEFAULT_SUBSYSTEM_NAME = "default";
	
	private final MapConfigSource config = new MapConfigSource();
	private final String configSourceId;
	
	public AstrixSettings() {
		this.configSourceId = GlobalConfigSourceRegistry.register(this);
	}
	
	public String getConfigSourceId() {
		return configSourceId;
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
	public String get(String propertyName,
			DynamicPropertyListener<String> propertyChangeListener) {
		return this.config.get(propertyName, propertyChangeListener);
	}
	

}
