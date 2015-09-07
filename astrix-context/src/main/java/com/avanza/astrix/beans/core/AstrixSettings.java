/*
 * Copyright 2014 Avanza Bank AB
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

import com.avanza.astrix.config.BooleanSetting;
import com.avanza.astrix.config.IntSetting;
import com.avanza.astrix.config.LongSetting;
import com.avanza.astrix.config.StringSetting;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixSettings {
	/**
	 * Defines how long to wait between consecutive bind attempts when a service bean is
	 * in UNBOUND state.
	 */
	public static final LongSetting BEAN_BIND_ATTEMPT_INTERVAL = LongSetting.create("StatefulAstrixBeanInstance.beanBindAttemptInterval", 10_000L);

	/**
	 * Defines how long to wait between consecutive lease renewals
	 * for a service bean that is in BOUND sate.
	 */
	public static final LongSetting SERVICE_LEASE_RENEW_INTERVAL = LongSetting.create("AstrixServiceLeaseManager.leaseRenewInterval", 30_000L);
	
	public static final BooleanSetting ENFORCE_SUBSYSTEM_BOUNDARIES = BooleanSetting.create("AstrixContext.enforceSubsystemBoundaries", true);
	
	public static final String SERVICE_REGISTRY_URI_PROPERTY_NAME = "AstrixServiceRegistry.serviceUri";
	

	/**
	 * Service Uri used to bind to the service-registry.
	 */
	public static final StringSetting SERVICE_REGISTRY_URI = StringSetting.create(SERVICE_REGISTRY_URI_PROPERTY_NAME, null);
	
	public static final BooleanSetting RUN_SERVICE_REGISTRY_EXPORTER = BooleanSetting.create("AstrixContext.runServiceRegistryExporterWorker", true);

	
	/**
	 * @deprecated Renamed to {@link AstrixSettings#SERVICE_REGISTRY_URI}
	 */
	@Deprecated
	public static final StringSetting ASTRIX_SERVICE_REGISTRY_URI = SERVICE_REGISTRY_URI;
	
	
	/**
	 * All services provided will be registered in the service-registry on a regular interval. This Setting defines
	 * the interval (in millis) on which provided services will be registered in the service-registry
	 * 
	 * Defaults to 30 seconds (30 000 ms)
	 */
	public static final LongSetting SERVICE_REGISTRY_EXPORT_INTERVAL = LongSetting.create("ServiceRegistryExporterWorker.exportIntervalMillis", 30_000L);
	
	/**
	 * When registration in the service registry fails, there is an option to wait a shorter time then the regular time 
	 * defined by {@link #SERVICE_REGISTRY_EXPORT_INTERVAL} 
	 * 
	 * Defaults to 5 seconds (5 000 ms)
	 */
	public static final LongSetting SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL = LongSetting.create("ServiceRegistryExporterWorker.retryIntervallMillis", 5_000);
	
	public static final LongSetting SERVICE_REGISTRY_LEASE = LongSetting.create("ServiceRegistryExporterWorker.serviceLeaseTimeMillis", 120_000L);

	/**
	 * Defines the basePackage(s) to scan when searching for ApiProvider's. The given package(s)
	 * and all subpackages will be scanned.
	 * 
	 * The packages should be separated by comma (",").
	 */
	public static final StringSetting API_PROVIDER_SCANNER_BASE_PACKAGE = StringSetting.create("AstrixApiProviderScanner.basePackage", "");
	
	public static final StringSetting SUBSYSTEM_NAME = StringSetting.create("AstrixContext.subsystem", "default");
	
	public static final BooleanSetting ENABLE_FAULT_TOLERANCE = BooleanSetting.create("AstrixContext.enableFaultTolerance", true);
	
	/**
	 * Whether Astrix should export MBEANS
	 */
	public static final BooleanSetting EXPORT_ASTRIX_MBEANS = BooleanSetting.create("com.avanza.astrix.exportAstrixMBeans", true);
	
	/**
	 * Whether Astrix should collect statistics about each bean invocation.
	 */
	public static final BooleanSetting ENABLE_BEAN_METRICS = BooleanSetting.create("com.avanza.astrix.enableBeanMetrics", true);
	
	
	public static final StringSetting GIGA_SPACE_BEAN_NAME = StringSetting.create("AstrixGsComponent.gigaSpaceBeanName", null);
	public static final StringSetting DYNAMIC_CONFIG_FACTORY = StringSetting.create("com.avanza.astrix.context.AstrixDynamicConfigFactory", null);
	/**
	 * When local view is disabled every service that is exported using local-view
	 * ({@link AstrixServiceComponentNames#GS_LOCAL_VIEW}) will use a regular clustered proxy instead.
	 */
	public static final BooleanSetting GS_DISABLE_LOCAL_VIEW = BooleanSetting.create("AstrixGsLocalViewComponent.disableLocalView", false);
	public static final LongSetting GS_LOCAL_VIEW_MAX_DISCONNECTION_TIME = LongSetting.create("AstrixGsLocalViewComponent.maxDisconnectionTime", 60000L);
	public static final IntSetting GS_LOCAL_VIEW_LOOKUP_TIMEOUT = IntSetting.create("AstrixGsLocalViewComponent.lookupTimeout", 1000);
	
	
	public static final StringSetting APPLICATION_NAME = StringSetting.create("astrix.application.name", null);
	public static final StringSetting APPLICATION_TAG =  StringSetting.create("astrix.application.tag", null);
	public static final StringSetting APPLICATION_INSTANCE_ID =  StringSetting.create("astrix.application.instanceid", null);
	public static final BooleanSetting PUBLISH_SERVICES = BooleanSetting.create("astrix.application.publishServices", true);

	/**
	 * Service component used to export ServiceAdministrator api.
	 */
	public static final StringSetting SERVICE_ADMINISTRATOR_COMPONENT = StringSetting.create("astrix.service.administrator.component", AstrixServiceComponentNames.GS_REMOTING);

	private AstrixSettings() {
	}
}
