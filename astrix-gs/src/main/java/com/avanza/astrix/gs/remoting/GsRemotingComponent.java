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
package com.avanza.astrix.gs.remoting;

import org.openspaces.core.GigaSpace;

import com.avanza.astrix.beans.ft.CommandSettings;
import com.avanza.astrix.beans.ft.IsolationStrategy;
import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.FaultToleranceConfigurator;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.gs.BoundProxyServiceBeanInstance;
import com.avanza.astrix.gs.ClusteredProxyCache;
import com.avanza.astrix.gs.ClusteredProxyCacheImpl.GigaSpaceInstance;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.modules.AstrixInject;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.remoting.client.RemotingProxy;
import com.avanza.astrix.remoting.client.RemotingTransport;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.spring.AstrixSpringContext;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;
/**
 * Provides remoting using a GigaSpace clustered proxy as transport. <p> 
 * 
 * @author Elias Lindholm
 *
 */
public class GsRemotingComponent implements ServiceComponent, FaultToleranceConfigurator {

	private GsBinder gsBinder;
	private AstrixSpringContext astrixSpringContext;
	private AstrixServiceActivator serviceActivator;
	private ObjectSerializerFactory objectSerializerFactory;
	private ClusteredProxyCache proxyCache;
	
	@Override
	public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties) {
		AstrixObjectSerializer objectSerializer = objectSerializerFactory.create(serviceDefinition.getObjectSerializerDefinition());
		
		GigaSpaceInstance proxyInstance = proxyCache.getProxy(serviceProperties);
		GsRemotingTransport gsRemotingTransport = new GsRemotingTransport(proxyInstance.getSpaceTaskDispatcher());
		RemotingTransport remotingTransport = RemotingTransport.create(gsRemotingTransport);
		T proxy = RemotingProxy.create(serviceDefinition.getServiceType(), ReflectionUtil.classForName(serviceProperties.getProperty(ServiceProperties.API))
				, remotingTransport, objectSerializer, new GsRoutingStrategy());
		return BoundProxyServiceBeanInstance.create(proxy, proxyInstance);
	}
	
	@Override
	public ServiceProperties parseServiceProviderUri(String serviceProviderUri) {
		return gsBinder.createServiceProperties(serviceProviderUri);
	}

	@Override
	public <T> ServiceProperties createServiceProperties(ServiceDefinition<T> serviceDefinition) {
		GigaSpace space = gsBinder.getEmbeddedSpace(astrixSpringContext.getApplicationContext());
		ServiceProperties serviceProperties = gsBinder.createProperties(space);
		return serviceProperties;
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
	public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition) {
		AstrixObjectSerializer objectSerializer = objectSerializerFactory.create(serviceDefinition.getObjectSerializerDefinition()); 
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
	public void setObjectSerializerFactory(ObjectSerializerFactory objectSerializerFactory) {
		this.objectSerializerFactory = objectSerializerFactory;
	}

	@Override
	public void configure(CommandSettings commandSettings) {
		commandSettings.setExecutionIsolationStrategy(IsolationStrategy.SEMAPHORE);
	}
	
}
