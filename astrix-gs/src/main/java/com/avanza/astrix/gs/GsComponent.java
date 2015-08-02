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
package com.avanza.astrix.gs;

import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.UnsupportedTargetTypeException;
import com.avanza.astrix.ft.BeanFaultTolerance;
import com.avanza.astrix.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.avanza.astrix.gs.ClusteredProxyCacheImpl.GigaSpaceInstance;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.spring.AstrixSpringContext;
/**
 * Service component allowing GigaSpace clustered proxy to be used as a service. 
 * 
 * @author Elias Lindholm
 *
 */
public class GsComponent implements ServiceComponent {

	private GsBinder gsBinder;
	private BeanFaultToleranceFactory faultToleranceFactory;
	private AstrixSpringContext astrixSpringContext;
	private ClusteredProxyCache proxyCache;
	
	
	@Override
	public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties) {
		Class<T> targetType = serviceDefinition.getServiceType();
		if (!GigaSpace.class.isAssignableFrom(targetType)) {
			throw new UnsupportedTargetTypeException(getName(), targetType);
		}
		GigaSpaceInstance gigaSpaceInstance = proxyCache.getProxy(serviceProperties);
		BeanFaultTolerance beanFaultTolerance = faultToleranceFactory.create(serviceDefinition);
		T proxyWithFaultTolerance = targetType.cast(GigaSpaceProxy.create(gigaSpaceInstance.get(), beanFaultTolerance, new HystrixCommandSettings()));
		return BoundProxyServiceBeanInstance.create(proxyWithFaultTolerance, gigaSpaceInstance);
	}
	
	@Override
	public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
		return gsBinder.createServiceProperties(serviceProviderUri);
	}


	@Override
	public String getName() {
		return AstrixServiceComponentNames.GS;
	}
	
	@Override
	public boolean canBindType(Class<?> type) {
		return GigaSpace.class.equals(type);
	}

	@Override
	public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> versioningContext) {
		// Intentionally empty
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return false;
	}
	
	@Override
	public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> definition) {
		if (!definition.getServiceType().equals(GigaSpace.class)) {
			throw new IllegalArgumentException("Can't export: " + definition.getServiceType());
		}
		GigaSpace space = gsBinder.getEmbeddedSpace(astrixSpringContext.getApplicationContext());
		return gsBinder.createProperties(space);
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixSpringContext astrixSpringContext) {
		this.astrixSpringContext = astrixSpringContext;
	}
	
	@AstrixInject
	public void setProxyCache(ClusteredProxyCache proxyCache) {
		this.proxyCache = proxyCache;
	}
	
	@AstrixInject
	public void setGsBinder(GsBinder gsBinder) {
		this.gsBinder = gsBinder;
	}
	
	@AstrixInject
	public void setFaultTolerance(BeanFaultToleranceFactory beanFaultToleranceFactory) {
		this.faultToleranceFactory = beanFaultToleranceFactory;
	}

}
