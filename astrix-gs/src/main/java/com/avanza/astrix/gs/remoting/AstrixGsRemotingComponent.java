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

import com.avanza.astrix.context.AstrixContextImpl;
import com.avanza.astrix.context.AstrixFaultTolerancePlugin;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixPlugins;
import com.avanza.astrix.context.AstrixPluginsAware;
import com.avanza.astrix.context.AstrixServiceComponent;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixVersioningPlugin;
import com.avanza.astrix.context.FaultToleranceSpecification;
import com.avanza.astrix.context.IsolationStrategy;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;
import com.avanza.astrix.remoting.client.AstrixRemotingProxy;
import com.avanza.astrix.remoting.client.AstrixRemotingTransport;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.spring.AstrixSpringContext;
/**
 * Provides remoting using a GigaSpace clustered proxy as transport. <p> 
 * 
 * @author Elias Lindholm
 *
 */
@MetaInfServices(AstrixServiceComponent.class)
public class AstrixGsRemotingComponent implements AstrixPluginsAware, AstrixServiceComponent {
	
	private AstrixPlugins plugins;
	private AstrixContextImpl astrixContext;
	
	@Override
	public <T> T createService(ServiceVersioningContext versioningContext, Class<T> api, AstrixServiceProperties serviceProperties) {
		AstrixObjectSerializer objectSerializer = plugins.getPlugin(AstrixVersioningPlugin.class).create(versioningContext);
		AstrixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AstrixFaultTolerancePlugin.class);
		
		String targetSpace = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		GigaSpace space = GsBinder.createGsFactory(serviceProperties).create();
		AstrixRemotingTransport remotingTransport = GsRemotingTransport.remoteSpace(space);
		
		T proxy = AstrixRemotingProxy.create(api, remotingTransport, objectSerializer, new GsRoutingStrategy());
		FaultToleranceSpecification<T> ftSpec = FaultToleranceSpecification.builder(api).provider(proxy)
				.group(targetSpace).isolationStrategy(IsolationStrategy.THREAD).build();
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(ftSpec);
		return proxyWithFaultTolerance;
	}
	
	@Override
	public AstrixServiceProperties createServiceProperties(String serviceUri) {
		return GsBinder.createServiceProperties(serviceUri);
	}
	
	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.plugins = plugins;
	}

	@Override
	public String getName() {
		return AstrixServiceComponentNames.GS_REMOTING;
	}
	
	@Override
	public <T> void exportService(Class<T> providedApi, T provider, ServiceVersioningContext versioningContext) {
		AstrixObjectSerializer objectSerializer = plugins.getPlugin(AstrixVersioningPlugin.class).create(versioningContext); 
		this.astrixContext.getInstance(AstrixServiceActivator.class).register(provider, objectSerializer, providedApi);
	}
	
	@Override
	public boolean requiresProviderInstance() {
		return true;
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContextImpl astrixContext) {
		this.astrixContext = astrixContext;
	}

	@Override
	public boolean supportsAsyncApis() {
		return true;
	}

	@Override
	public <T> AstrixServiceProperties createServiceProperties(Class<T> exportedService) {
		GigaSpace gigaSpace = astrixContext.getInstance(AstrixSpringContext.class).getApplicationContext().getBean(GigaSpace.class);
		AstrixServiceProperties serviceProperties = GsBinder.createProperties(gigaSpace);
		serviceProperties.setQualifier(null);
		return serviceProperties;
	}
	
}
