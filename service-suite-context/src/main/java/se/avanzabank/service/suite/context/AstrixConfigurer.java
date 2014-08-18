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

import java.util.List;

public class AstrixConfigurer {

	private boolean useFaultTolerance = false;
	private boolean enableVersioning = false;
	private AstrixContext context = new AstrixContext();
	
	public Astrix configure() {
		configureFaultTolerance(context);
		configureVersioning(context);
		configureLibrarySupport(context);
		AstrixImpl astrix = new AstrixImpl();
		AstrixServiceProviderFactory serviceProviderFactory = new AstrixServiceProviderFactory(context, astrix);
		
		List<AstrixServiceProvider> serviceProviders = new AstrixServiceProviderScanner("se.avanzabank", serviceProviderFactory).scan();
		for (AstrixServiceProvider serviceProvider : serviceProviders) {
			astrix.registerServiceProvider(serviceProvider);
		}
		return astrix;
	}
	
	public void useFaultTolerance(boolean useFaultTolerance) {
		this.useFaultTolerance  = useFaultTolerance;
	}
	
	public void enableVersioning(boolean enableVersioning) {
		this.enableVersioning = enableVersioning;
	}

	private void configureLibrarySupport(AstrixContext context) {
		AstrixPluginDiscovery.discoverAllPlugins(context, AstrixServiceProviderPlugin.class, new AstrixLibraryProviderPlugin());
	}
	
	private void configureVersioning(AstrixContext context) {
		if (enableVersioning) {
			AstrixPluginDiscovery.discoverOnePlugin(context, AstrixVersioningPlugin.class);
		} else {
			context.registerProvider(AstrixVersioningPlugin.class, AstrixVersioningPlugin.Factory.noSerializationSupport());
		}
	}

	private void configureFaultTolerance(AstrixContext context) {
		if (useFaultTolerance) {
			AstrixPluginDiscovery.discoverOnePlugin(context, AstrixFaultTolerancePlugin.class);
		} else {
			context.registerProvider(AstrixFaultTolerancePlugin.class, AstrixFaultTolerancePlugin.Factory.noFaultTolerance());
		}
	}

	public <T> void register(Class<T> type, T provider) {
		context.registerProvider(type, provider);
	}


}
