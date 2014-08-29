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
package se.avanzabank.service.suite.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class AstrixConfigurer {

	private boolean useFaultTolerance = false;
	private boolean enableVersioning = true;
	private List<ExternalDependencyBean> externalDependencyBeans = new ArrayList<>();
	private List<Object> externalDependencies = new ArrayList<>();
	
	public AstrixContext configure() {
		AstrixContext context = new AstrixContext();
		context.setExternalDependencyBeans(externalDependencyBeans);
		context.setExternalDependencies(externalDependencies);
		configureFaultTolerance(context);
		configureVersioning(context);
		discoverServiceProviderPlugins(context);
		AstrixServiceProviderFactory serviceProviderFactory = new AstrixServiceProviderFactory(context);
		
		List<AstrixServiceProvider> serviceProviders = new AstrixServiceProviderScanner("se.avanzabank", serviceProviderFactory).scan();
		for (AstrixServiceProvider serviceProvider : serviceProviders) {
			context.registerServiceProvider(serviceProvider);
		}
		return context;
	}
	
	@Autowired(required = false)
	public void setExternalDependencies(List<ExternalDependencyBean> externalDependencies) {
		this.externalDependencyBeans = externalDependencies;
	}
	
	public void useFaultTolerance(boolean useFaultTolerance) {
		this.useFaultTolerance  = useFaultTolerance;
	}
	
	public void enableVersioning(boolean enableVersioning) {
		this.enableVersioning = enableVersioning;
	}

	private void discoverServiceProviderPlugins(AstrixContext context) {
		AstrixPluginDiscovery.discoverAllPlugins(context, AstrixServiceProviderPlugin.class, new AstrixLibraryProviderPlugin());
	}
	
	private void configureVersioning(AstrixContext context) {
		if (enableVersioning) {
			AstrixPluginDiscovery.discoverOnePlugin(context, AstrixVersioningPlugin.class);
		} else {
			context.registerPlugin(AstrixVersioningPlugin.class, AstrixVersioningPlugin.Default.create());
		}
	}

	private void configureFaultTolerance(AstrixContext context) {
		if (useFaultTolerance) {
			AstrixPluginDiscovery.discoverOnePlugin(context, AstrixFaultTolerancePlugin.class);
		} else {
			context.registerPlugin(AstrixFaultTolerancePlugin.class, AstrixFaultTolerancePlugin.Default.create());
		}
	}

	public <T> void registerDependency(T dependency) {
		this.externalDependencies.add(dependency);
	}

	public void registerDependency(ExternalDependencyBean externalDependency) {
		this.externalDependencyBeans.add(externalDependency);
	}
	
}
