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
package com.avanza.astrix.gs.remoting;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceContext;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.context.AstrixVersioningPlugin;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.ft.AstrixFaultTolerance;
import com.avanza.astrix.gs.BoundProxyServiceBeanInstance;
import com.avanza.astrix.gs.ClusteredProxyCache;
import com.avanza.astrix.gs.ClusteredProxyCache.GigaSpaceInstance;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.remoting.client.RemotingProxy;
import com.avanza.astrix.remoting.client.RemotingTransport;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.spring.AstrixSpringContext;
/**
 * Provides remoting using a GigaSpace clustered proxy as transport. <p> 
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(ServiceComponent.class)
public class GsRemotingComponent implements ServiceComponent {

	private GsBinder gsBinder;
	private AstrixFaultTolerance faultTolerance;
	private AstrixSpringContext astrixSpringContext;
	private AstrixServiceActivator serviceActivator;
	private AstrixVersioningPlugin versioningPlugin;
	private ClusteredProxyCache proxyCache;
	
	@Override
	public <T> BoundServiceBeanInstance<T> bind(Class<T> api, ServiceContext versioningContext, ServiceProperties serviceProperties) {
		AstrixObjectSerializer objectSerializer = versioningPlugin.create(versioningContext);
		
		GigaSpaceInstance proxyInstance = proxyCache.getProxy(serviceProperties);
		RemotingTransport remotingTransport = GsRemotingTransport.remoteSpace(proxyInstance.get(), faultTolerance);
		
		T proxy = RemotingProxy.create(api, remotingTransport, objectSerializer, new GsRoutingStrategy());
		return BoundProxyServiceBeanInstance.create(proxy, proxyInstance);
	}
	
	@Override
	public ServiceProperties createServiceProperties(String serviceUri) {
		return gsBinder.createServiceProperties(serviceUri);
	}
	
	@Override
	public String getName() {
		return AstrixServiceComponentNames.GS_REMOTING;
	}
	
	@Override
	public boolean canBindType(Class<?> type) {
		return true;
	}
	
	@Override
	public <T> void exportService(Class<T> providedApi, T provider, ServiceContext versioningContext) {
		AstrixObjectSerializer objectSerializer = versioningPlugin.create(versioningContext); 
		this.serviceActivator.register(provider, objectSerializer, providedApi);
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return true;
	}
	
	
	@AstrixInject
	public void setGsBinder(GsBinder gsBinder) {
		this.gsBinder = gsBinder;
	}
	
	@AstrixInject
	public void setProxyCache(ClusteredProxyCache proxyCache) {
		this.proxyCache = proxyCache;
	}
	
	@AstrixInject
	public void setAstrixSpringContext(AstrixSpringContext astrixSpringContext) {
		this.astrixSpringContext = astrixSpringContext;
	}
	
	@AstrixInject
	public void setServiceActivator(AstrixServiceActivator serviceActivator) {
		this.serviceActivator = serviceActivator;
	}
	
	@AstrixInject
	public void setFaultTolerance(AstrixFaultTolerance faultTolerance) {
		this.faultTolerance = faultTolerance;
	}
	
	@AstrixInject
	public void setVersioningPlugin(AstrixVersioningPlugin versioningPlugin) {
		this.versioningPlugin = versioningPlugin;
	}

	@Override
	public boolean supportsAsyncApis() {
		return true;
	}

	@Override
	public <T> ServiceProperties createServiceProperties(Class<T> exportedService) {
		GigaSpace space = gsBinder.getEmbeddedSpace(astrixSpringContext.getApplicationContext());
		ServiceProperties serviceProperties = gsBinder.createProperties(space);
		return serviceProperties;
	}
	
}
