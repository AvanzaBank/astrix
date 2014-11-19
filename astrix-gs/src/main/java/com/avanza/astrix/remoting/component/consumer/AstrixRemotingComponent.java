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
package com.avanza.astrix.remoting.component.consumer;

import org.kohsuke.MetaInfServices;
import org.openspaces.core.GigaSpace;

import com.avanza.astrix.context.AstrixApiDescriptor;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.AstrixFaultTolerancePlugin;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixPlugins;
import com.avanza.astrix.context.AstrixPluginsAware;
import com.avanza.astrix.context.AstrixServiceComponent;
import com.avanza.astrix.context.AstrixServiceExporterBean;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixServicePropertiesBuilder;
import com.avanza.astrix.context.AstrixSpringContext;
import com.avanza.astrix.context.AstrixVersioningPlugin;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.gs.GsBinder;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.remoting.client.AstrixRemotingProxy;
import com.avanza.astrix.remoting.client.AstrixRemotingTransport;
import com.avanza.astrix.remoting.component.provider.AstrixRemotingServiceRegistryExporter;
import com.avanza.astrix.remoting.server.AstrixRemotingServiceExporterBean;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;

@MetaInfServices(AstrixServiceComponent.class)
public class AstrixRemotingComponent implements AstrixPluginsAware, AstrixServiceComponent {
	
	private AstrixPlugins plugins;
	private AstrixContext astrixContext;
	
	@Override
	public <T> T createService(AstrixApiDescriptor descriptor, Class<T> api, AstrixServiceProperties serviceProperties) {
		AstrixObjectSerializer objectSerializer = plugins.getPlugin(AstrixVersioningPlugin.class).create(descriptor);
		AstrixFaultTolerancePlugin faultTolerance = plugins.getPlugin(AstrixFaultTolerancePlugin.class);
		
		String targetSpace = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		GigaSpace space = GsBinder.createGsFactory(serviceProperties).create();
		AstrixRemotingTransport remotingTransport = AstrixRemotingTransport.remoteSpace(space);
		
		T proxy = AstrixRemotingProxy.create(api, remotingTransport, objectSerializer);
		T proxyWithFaultTolerance = faultTolerance.addFaultTolerance(api, proxy, targetSpace);
		return proxyWithFaultTolerance;
	}
	
	@Override
	public <T> T createService(AstrixApiDescriptor apiDescriptor, Class<T> type, String serviceUrl) {
		return createService(apiDescriptor, type, GsBinder.createServiceProperties(serviceUrl));
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
	public Class<? extends AstrixServiceExporterBean> getExporterBean() {
		return AstrixRemotingServiceExporterBean.class;
	}
	
	@Override
	public Class<? extends AstrixServicePropertiesBuilder> getServiceBuilder() {
		return AstrixRemotingServiceRegistryExporter.class;
	}

	@Override
	public <T> void exportService(Class<T> providedApi, T provider, AstrixApiDescriptor apiDescriptor) {
		AstrixObjectSerializer objectSerializer = plugins.getPlugin(AstrixVersioningPlugin.class).create(apiDescriptor); 
		this.astrixContext.getInstance(AstrixServiceActivator.class).register(provider, objectSerializer, providedApi);
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContext astrixContext) {
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
