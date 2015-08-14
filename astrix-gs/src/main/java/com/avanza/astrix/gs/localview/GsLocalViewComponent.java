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
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.FaultToleranceConfigurator;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.UnsupportedTargetTypeException;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.BeanFaultTolerance;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.ft.CommandSettings;
import com.avanza.astrix.ft.IsolationStrategy;
import com.avanza.astrix.gs.ClusteredProxyBinder;
import com.avanza.astrix.gs.GigaSpaceProxy;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.spring.AstrixSpringContext;
import com.j_spaces.core.IJSpace;
/**
 * Allows publishing a GigaSpace's local-view.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class GsLocalViewComponent implements ServiceComponent, AstrixConfigAware, FaultToleranceConfigurator {

	private static Logger log = LoggerFactory.getLogger(GsLocalViewComponent.class);
	private GsBinder gsBinder;
	private AstrixSpringContext astrixSpringContext;
	private DynamicBooleanProperty disableLocalView;
	private BeanFaultToleranceFactory faultToleranceFactory;
	private DynamicLongProperty maxDisonnectionTime;
	private DynamicIntProperty lookupTimeout;
	private ClusteredProxyBinder clusteredProxyBinder;
	
	
	
	public GsLocalViewComponent(GsBinder gsBinder,
			AstrixSpringContext astrixSpringContext,
			BeanFaultToleranceFactory faultToleranceFactory,
			ClusteredProxyBinder clusteredProxyBinder) {
		this.gsBinder = gsBinder;
		this.astrixSpringContext = astrixSpringContext;
		this.faultToleranceFactory = faultToleranceFactory;
		this.clusteredProxyBinder = clusteredProxyBinder;
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
		IJSpace space = gsSpaceConfigurer.lookupTimeout(this.lookupTimeout.get()).create();
		
		LocalViewSpaceConfigurer gslocalViewSpaceConfigurer = new LocalViewSpaceConfigurer(space);
		gslocalViewSpaceConfigurer.maxDisconnectionDuration(this.maxDisonnectionTime.get());
		localViewConfigurer.configure(new LocalViewSpaceConfigurerAdapter(gslocalViewSpaceConfigurer));
		
		String spaceName = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		String commandKey = spaceName + "_" + GigaSpace.class.getSimpleName();
		String qualifier = serviceProperties.getProperty(ServiceProperties.QUALIFIER);
		if (qualifier != null) {
			commandKey = commandKey + "-" + qualifier;
		}
		
		CommandSettings commandSettings = new CommandSettings();
		commandSettings.setExecutionIsolationStrategy(IsolationStrategy.SEMAPHORE);
		commandSettings.setSemaphoreMaxConcurrentRequests(Integer.MAX_VALUE);
		
		BeanFaultTolerance faultTolerance = faultToleranceFactory.create(serviceDefinition, commandSettings);
		
		IJSpace localViewSpace = gslocalViewSpaceConfigurer.create();
		GigaSpace localViewGigaSpace = GigaSpaceProxy.create(new GigaSpaceConfigurer(localViewSpace).create(), faultTolerance);
		
		BoundLocalViewGigaSpaceBeanInstance localViewGigaSpaceBeanInstance = 
				new BoundLocalViewGigaSpaceBeanInstance(localViewGigaSpace, gslocalViewSpaceConfigurer, gsSpaceConfigurer);
		return (BoundServiceBeanInstance<T>) localViewGigaSpaceBeanInstance;
	}
	
	@Override
	public void configure(CommandSettings commandSettings) {
		commandSettings.setExecutionIsolationStrategy(IsolationStrategy.SEMAPHORE);
		commandSettings.setSemaphoreMaxConcurrentRequests(Integer.MAX_VALUE);
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

		private GigaSpace instance;
		private LocalViewSpaceConfigurer localViewSpaceConfigurer;
		private UrlSpaceConfigurer spaceConfigurer;
		
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
