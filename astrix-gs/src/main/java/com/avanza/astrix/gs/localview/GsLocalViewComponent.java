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
package com.avanza.astrix.gs.localview;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;
import org.openspaces.core.GigaSpaceConfigurer;
import org.openspaces.core.space.UrlSpaceConfigurer;
import org.openspaces.core.space.cache.LocalViewSpaceConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.inject.AstrixInjector;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceComponents;
import com.avanza.astrix.beans.service.ServiceContext;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.UnsupportedTargetTypeException;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.AstrixConfigAware;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.AstrixFaultTolerance;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.avanza.astrix.gs.AstrixGigaSpaceProxy;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.spring.AstrixSpringContext;
import com.j_spaces.core.IJSpace;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
/**
 * Allows publishing a GigaSpace's local-view.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@MetaInfServices(ServiceComponent.class)
public class GsLocalViewComponent implements ServiceComponent, AstrixConfigAware {

	private static Logger log = LoggerFactory.getLogger(GsLocalViewComponent.class);
	private GsBinder gsBinder;
	private AstrixSpringContext astrixSpringContext;
	private DynamicBooleanProperty disableLocalView;
	/*
	 * To avoid circular dependency between GsLocalViewComponent and ServiceComponents
	 * we have to use the AstrixInjetor to retrieve ServiceComponents.
	 */
	private AstrixInjector injector;
	private AstrixFaultTolerance faultTolerance;
	
	@Override
	public <T> BoundServiceBeanInstance<T> bind(
			Class<T> type, 
			ServiceContext versioningContext,
			ServiceProperties serviceProperties) {
		if (!GigaSpace.class.isAssignableFrom(type)) {
			throw new UnsupportedTargetTypeException(getName(), type);
		}
		if (disableLocalView.get()) {
			log.info("LocalView is disabled. Creating reqular proxy");
			ServiceComponent gsComponent = injector.getBean(ServiceComponents.class).getComponent(AstrixServiceComponentNames.GS);
			return gsComponent.bind(type, versioningContext, serviceProperties);
		}
		// TODO: protect creation of localView with fault-tolerance?
		Class<LocalViewConfigurer> serviceConfigClass = versioningContext.getServiceConfigClass(LocalViewConfigurer.class);	
		LocalViewConfigurer localViewConfigurer = ReflectionUtil.newInstance(serviceConfigClass);
		UrlSpaceConfigurer gsSpaceConfigurer = new UrlSpaceConfigurer(serviceProperties.getProperty(GsBinder.SPACE_URL_PROPERTY));
		IJSpace space = gsSpaceConfigurer.lookupTimeout(1_000).create();
		
		LocalViewSpaceConfigurer gslocalViewSpaceConfigurer = new LocalViewSpaceConfigurer(space);
		localViewConfigurer.configure(new LocalViewSpaceConfigurerAdapter(gslocalViewSpaceConfigurer));
		
		String spaceName = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		String commandKey = spaceName + "_" + GigaSpace.class.getSimpleName();
		String qualifier = serviceProperties.getProperty(ServiceProperties.QUALIFIER);
		if (qualifier != null) {
			commandKey = commandKey + "-" + qualifier;
		}
		HystrixCommandSettings hystrixSettings = new HystrixCommandSettings(commandKey, spaceName);
		hystrixSettings.setExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE);
		hystrixSettings.setSemaphoreMaxConcurrentRequests(Integer.MAX_VALUE);
		
		IJSpace localViewSpace = gslocalViewSpaceConfigurer.create();
		GigaSpace localViewGigaSpace = AstrixGigaSpaceProxy.create(new GigaSpaceConfigurer(localViewSpace).create(), faultTolerance, hystrixSettings);
		
		BoundLocalViewGigaSpaceBeanInstance localViewGigaSpaceBeanInstance = 
				new BoundLocalViewGigaSpaceBeanInstance(localViewGigaSpace, gslocalViewSpaceConfigurer, gsSpaceConfigurer);
		return (BoundServiceBeanInstance<T>) localViewGigaSpaceBeanInstance;
	}

	@Override
	public ServiceProperties createServiceProperties(String serviceUri) {
		return gsBinder.createServiceProperties(serviceUri);
	}

	@Override
	public <T> ServiceProperties createServiceProperties(Class<T> type) {
		if (!type.equals(GigaSpace.class)) {
			throw new UnsupportedTargetTypeException(getName(), type);
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
								  ServiceContext versioningContext) {
		// Intentionally empty
	}
	
	@Override
	public boolean supportsAsyncApis() {
		return false;
	}

	@Override
	public boolean requiresProviderInstance() {
		return false;
	}
	
	@AstrixInject
	public void setGsBinder(GsBinder gsBinder) {
		this.gsBinder = gsBinder;
	}
	
	@AstrixInject
	public void setGsBinder(AstrixSpringContext astrixSpringContext) {
		this.astrixSpringContext = astrixSpringContext;
	}
	
	@AstrixInject
	public void setInjector(AstrixInjector injector) {
		this.injector = injector;
	}

	@AstrixInject
	public void setFaultTolerance(AstrixFaultTolerance faultTolerance) {
		this.faultTolerance = faultTolerance;
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.disableLocalView = AstrixSettings.GS_DISABLE_LOCAL_VIEW.getFrom(config);
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
				localViewSpaceConfigurer.destroy();
				spaceConfigurer.destroy();
			} catch (Exception e) {
				log.error("Failed to destroy local-view", e);
			}
		}
	}

}
