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

import com.avanza.astrix.context.AstrixContextImpl;
import com.avanza.astrix.context.AstrixFaultTolerancePlugin;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixPlugins;
import com.avanza.astrix.context.AstrixPluginsAware;
import com.avanza.astrix.context.AstrixServiceComponent;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.FaultToleranceSpecification;
import com.avanza.astrix.context.IsolationStrategy;
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
public class AstrixGsComponent implements AstrixServiceComponent, AstrixPluginsAware{

	private AstrixPlugins plugins;
	private AstrixContextImpl astrixContext;
	private GsBinder gsBinder;
	
	
	@Override
	public <T> T createService(ServiceVersioningContext versioningContext, Class<T> type, AstrixServiceProperties serviceProperties) {
		if (!GigaSpace.class.isAssignableFrom(type)) {
			throw new IllegalStateException("Programming error, attempted to create: " + type);
		}
		T gigaSpace = type.cast(gsBinder.createGsFactory(serviceProperties).create());
		String spaceName = serviceProperties.getProperty(GsBinder.SPACE_NAME_PROPERTY);
		FaultToleranceSpecification<T> ftSpec = FaultToleranceSpecification.builder(type).provider(gigaSpace)
				.isolationStrategy(IsolationStrategy.THREAD).group(spaceName).build();
		return plugins.getPlugin(AstrixFaultTolerancePlugin.class).addFaultTolerance(ftSpec);
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
		GigaSpace space = gsBinder.getEmbeddedSpace(astrixContext.getInstance(AstrixSpringContext.class).getApplicationContext());
		return gsBinder.createProperties(space);
	}
	
	@AstrixInject
	public void setAstrixContext(AstrixContextImpl astrixContext) {
		this.astrixContext = astrixContext;
	}
	
	@AstrixInject
	public void setGsBinder(GsBinder gsBinder) {
		this.gsBinder = gsBinder;
	}

	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.plugins = plugins;
	}

}
