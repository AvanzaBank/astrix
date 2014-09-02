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
package se.avanzabank.asterix.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

public class AsterixConfigurer {

	private boolean useFaultTolerance = false;
	private boolean enableVersioning = true;
	private List<ExternalDependencyBean> externalDependencyBeans = new ArrayList<>();
	private List<Object> externalDependencies = new ArrayList<>();
	
	public AsterixContext configure() {
		AsterixContext context = new AsterixContext();
		context.setExternalDependencyBeans(externalDependencyBeans);
		context.setExternalDependencies(externalDependencies);
		configureFaultTolerance(context);
		configureVersioning(context);
		discoverApiProviderPlugins(context);
		List<AsterixApiProviderPlugin> apiProviderPlugins = context.getPlugins(AsterixApiProviderPlugin.class);
		AsterixApiProviderFactory apiProviderFactory = new AsterixApiProviderFactory(apiProviderPlugins);
		
		List<AsterixApiProvider> apiProviders = new AsterixApiProviderScanner("se.avanzabank", apiProviderFactory).scan();
		for (AsterixApiProvider apiProvider : apiProviders) {
			context.registerApiProvider(apiProvider);
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

	private void discoverApiProviderPlugins(AsterixContext context) {
		AsterixPluginDiscovery.discoverAllPlugins(context, AsterixApiProviderPlugin.class, new AsterixLibraryProviderPlugin()); // TODO: no need to pass default instance
	}
	
	private void configureVersioning(AsterixContext context) {
		if (enableVersioning) {
			AsterixPluginDiscovery.discoverOnePlugin(context, AsterixVersioningPlugin.class);
		} else {
			context.registerPlugin(AsterixVersioningPlugin.class, AsterixVersioningPlugin.Default.create());
		}
	}

	private void configureFaultTolerance(AsterixContext context) {
		if (useFaultTolerance) {
			AsterixPluginDiscovery.discoverOnePlugin(context, AsterixFaultTolerancePlugin.class);
		} else {
			context.registerPlugin(AsterixFaultTolerancePlugin.class, AsterixFaultTolerancePlugin.Default.create());
		}
	}

	public <T> void registerDependency(T dependency) {
		this.externalDependencies.add(dependency);
	}

	public void registerDependency(ExternalDependencyBean externalDependency) {
		this.externalDependencyBeans.add(externalDependency);
	}
	
}
