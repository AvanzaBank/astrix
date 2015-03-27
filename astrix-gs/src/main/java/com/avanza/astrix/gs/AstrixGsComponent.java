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
package com.avanza.astrix.gs;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.AstrixServiceComponent;
import com.avanza.astrix.beans.service.AstrixServiceProperties;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.ft.HystrixCommandSettings;
import com.avanza.astrix.ft.plugin.AstrixFaultTolerance;
import com.avanza.astrix.gs.ClusteredProxyCache.GigaSpaceInstance;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
import com.avanza.astrix.spring.AstrixSpringContext;
/**
 * Service component allowing GigaSpace clustered proxy to be used as a service. 
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(AstrixServiceComponent.class)
public class AstrixGsComponent implements AstrixServiceComponent {

	private GsBinder gsBinder;
	private AstrixFaultTolerance faultTolerance;
	private AstrixSpringContext astrixSpringContext;
	private ClusteredProxyCache proxyCache;
	
	
	@Override
	public <T> BoundServiceBeanInstance<T> bind(ServiceVersioningContext versioningContext, Class<T> type, AstrixServiceProperties serviceProperties) {
		if (!GigaSpace.class.isAssignableFrom(type)) {
			throw new IllegalStateException("Programming error, attempted to create: " + type);
		}
		GigaSpaceInstance gigaSpaceInstance = proxyCache.getProxy(serviceProperties);
		String spaceName = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		HystrixCommandSettings hystrixSettings = new HystrixCommandSettings(spaceName + "_" + GigaSpace.class.getSimpleName(), spaceName);
		return BoundProxyServiceBeanInstance.create(faultTolerance.addFaultTolerance(type, type.cast(gigaSpaceInstance.get()), hystrixSettings), gigaSpaceInstance);
	}
	
	@Override
	public AstrixServiceProperties createServiceProperties(String serviceUri) {
		return gsBinder.createServiceProperties(serviceUri);
	}


	@Override
	public String getName() {
		return AstrixServiceComponentNames.GS;
	}
	

	@Override
	public <T> void exportService(Class<T> providedApi, T provider, ServiceVersioningContext versioningContext) {
		// Intentionally empty
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return false;
	}
	
	@Override
	public boolean supportsAsyncApis() {
		return false;
	}

	@Override
	public <T> AstrixServiceProperties createServiceProperties(Class<T> type) {
		if (!type.equals(GigaSpace.class)) {
			throw new IllegalArgumentException("Can't export: " + type);
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
	public void setFaultTolerance(AstrixFaultTolerance faultTolerance) {
		this.faultTolerance = faultTolerance;
	}

}
