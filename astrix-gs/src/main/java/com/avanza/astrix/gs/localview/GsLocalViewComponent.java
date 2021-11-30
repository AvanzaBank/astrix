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
package com.avanza.astrix.gs.localview;

import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.openspaces.core.space.cache.LocalViewSpaceConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.core.BeanProxy;
import com.avanza.astrix.beans.core.BeanProxyFilter;
import com.avanza.astrix.beans.core.BeanProxyNames;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.UnsupportedTargetTypeException;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.gs.ClusteredProxyBinder;
import com.avanza.astrix.gs.GigaSpaceProxy;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.gs.metrics.GigaspaceMetricsExporter;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.spring.AstrixSpringContext;
import com.j_spaces.core.IJSpace;
/**
 * Allows publishing a GigaSpace's local-view.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class GsLocalViewComponent implements ServiceComponent, AstrixConfigAware, BeanProxyFilter {

	private static final Logger log = LoggerFactory.getLogger(GsLocalViewComponent.class);
	private final GsBinder gsBinder;
	private final AstrixSpringContext astrixSpringContext;
	private final ClusteredProxyBinder clusteredProxyBinder;
	private final GigaspaceMetricsExporter metricsExporter;
	private DynamicBooleanProperty disableLocalView;
	private DynamicLongProperty maxDisonnectionTime;
	private DynamicIntProperty lookupTimeout;

	public GsLocalViewComponent(GsBinder gsBinder,
								AstrixSpringContext astrixSpringContext,
								ClusteredProxyBinder clusteredProxyBinder,
								GigaspaceMetricsExporter metricsExporter) {
		this.gsBinder = gsBinder;
		this.astrixSpringContext = astrixSpringContext;
		this.clusteredProxyBinder = clusteredProxyBinder;
		this.metricsExporter = metricsExporter;
	}

	@Override
	public <T> BoundServiceBeanInstance<T> bind(
			ServiceDefinition<T> serviceDefinition,
			ServiceProperties serviceProperties) {
		Class<T> type = serviceDefinition.getServiceType();
		if (!GigaSpace.class.isAssignableFrom(type)) {
			throw new UnsupportedTargetTypeException(getName(), type);
		}
		if (disableLocalView.get()) {
			log.info("LocalView is disabled. Creating reqular proxy");
			return clusteredProxyBinder.bind(serviceDefinition, serviceProperties);
		}
		log.info("Creating local view. bean={} serviceProperties={}", serviceDefinition.getBeanKey(), serviceProperties.getProperties());
		// TODO: protect creation of localView with fault-tolerance?
		Class<LocalViewConfigurer> serviceConfigClass = serviceDefinition.getServiceConfigClass(LocalViewConfigurer.class);	
		LocalViewConfigurer localViewConfigurer = ReflectionUtil.newInstance(serviceConfigClass);
		UrlSpaceConfigurer gsSpaceConfigurer = new UrlSpaceConfigurer(serviceProperties.getProperty(GsBinder.SPACE_URL_PROPERTY));
		IJSpace space = gsSpaceConfigurer.lookupTimeout(this.lookupTimeout.get())
				// Disable memory shortage check for local view clients
				.addParameter("space-config.engine.memory_usage.enabled", "false").create();
		
		LocalViewSpaceConfigurer gslocalViewSpaceConfigurer = new LocalViewSpaceConfigurer(space);
		gslocalViewSpaceConfigurer.maxDisconnectionDuration(this.maxDisonnectionTime.get());
		localViewConfigurer.configure(new LocalViewSpaceConfigurerAdapter(gslocalViewSpaceConfigurer));
		
		String spaceName = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		String commandKey = spaceName + "_" + GigaSpace.class.getSimpleName();
		String qualifier = serviceProperties.getProperty(ServiceProperties.QUALIFIER);
		if (qualifier != null) {
			commandKey = commandKey + "-" + qualifier;
		}
		
		IJSpace localViewSpace = gslocalViewSpaceConfigurer.create();
		GigaSpace localViewGigaSpace = GigaSpaceProxy.create(new GigaSpaceConfigurer(localViewSpace).create());

		metricsExporter.exportGigaspaceMetrics();

		@SuppressWarnings("unchecked")
		BoundServiceBeanInstance<T> localViewGigaSpaceBeanInstance =
				(BoundServiceBeanInstance<T>) new BoundLocalViewGigaSpaceBeanInstance(localViewGigaSpace, gslocalViewSpaceConfigurer, gsSpaceConfigurer);
		return localViewGigaSpaceBeanInstance;
	}
	
	@Override
	public boolean applyBeanProxy(BeanProxy beanProxy) {
		if (beanProxy.name().equals(BeanProxyNames.FAULT_TOLERANCE)) {
			if (disableLocalView.get()) {
				return true; // Apply faultTolerance proxy if local-view is disabled
			}
			return false; 
		}
		return true;
	}
	
	@Override
	public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
		return gsBinder.createServiceProperties(serviceProviderUri);
	}

	@Override
	public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> definition) {
		if (!definition.getServiceType().equals(GigaSpace.class)) {
			throw new UnsupportedTargetTypeException(getName(), definition.getServiceType());
		}
		GigaSpace space = gsBinder.getEmbeddedSpace(astrixSpringContext.getApplicationContext());
		ServiceProperties properties = gsBinder.createProperties(space);
		return properties;
	}

	@Override
	public String getName() {
		return AstrixServiceComponentNames.GS_LOCAL_VIEW;
	}
	
	@Override
	public boolean canBindType(Class<?> type) {
		return GigaSpace.class.equals(type);
	}

	@Override
	public <T> void exportService(Class<T> providedApi, 
								  T provider, 
								  ServiceDefinition<T> serviceDefintition) {
		// Intentionally empty
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return false;
	}
	
	@Override
	public void setConfig(DynamicConfig config) {
		this.disableLocalView = AstrixSettings.GS_DISABLE_LOCAL_VIEW.getFrom(config);
		this.maxDisonnectionTime = AstrixSettings.GS_LOCAL_VIEW_MAX_DISCONNECTION_TIME.getFrom(config);
		this.lookupTimeout = AstrixSettings.GS_LOCAL_VIEW_LOOKUP_TIMEOUT.getFrom(config);
	}
	
	private static class BoundLocalViewGigaSpaceBeanInstance implements BoundServiceBeanInstance<GigaSpace> {

		private final GigaSpace instance;
		private final LocalViewSpaceConfigurer localViewSpaceConfigurer;
		private final UrlSpaceConfigurer spaceConfigurer;
		
		public BoundLocalViewGigaSpaceBeanInstance(GigaSpace instance,
												   LocalViewSpaceConfigurer localViewSpaceConfigurer,
												   UrlSpaceConfigurer spaceConfigurer) {
			this.instance = instance;
			this.localViewSpaceConfigurer = localViewSpaceConfigurer;
			this.spaceConfigurer = spaceConfigurer;
		}

		@Override
		public GigaSpace get() {
			return instance;
		}

		@Override
		public void release() {
			try {
				localViewSpaceConfigurer.close();
				spaceConfigurer.close();
			} catch (Exception e) {
				log.error("Failed to destroy local-view", e);
			}
		}
	}

}
