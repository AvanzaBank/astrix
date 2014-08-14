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
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import javax.imageio.spi.ServiceRegistry;

public class AstrixPluginDiscovery {
	
	public static List<AstrixServiceProviderPlugin<?>> discoverServiceProviderPlugins() {
		Iterator<AstrixServiceProviderPlugin> serviceProviderPlugins = ServiceLoader.load(AstrixServiceProviderPlugin.class).iterator();
		List<AstrixServiceProviderPlugin<?>> result = new ArrayList<>();
		while (serviceProviderPlugins.hasNext()) {
			AstrixServiceProviderPlugin serviceProviderPlugin = serviceProviderPlugins.next();
			result.add(serviceProviderPlugin);
		}
		return result;
	}
	
	public static AstrixObjectSerializerFactory discoverObjectSerializerFactory() {
		Iterator<AstrixObjectSerializerFactory> serializerFactory = ServiceLoader.load(AstrixObjectSerializerFactory.class).iterator();
		if (!serializerFactory.hasNext()) {
			return null;
		}
		return serializerFactory.next();
	}
	
	public static AstrixFaultTolerance discoverAstrixFaultTolerance() {
		Iterator<AstrixFaultTolerance> faultTolerance = ServiceLoader.load(AstrixFaultTolerance.class).iterator();		
		if (!faultTolerance.hasNext()) {
			return null;
		}
		return faultTolerance.next(); // TODO: detect config error... 
	}

	public static AstrixContext discoverPlugins() {
		AstrixContext plugins = new AstrixContext();
		AstrixFaultTolerance faultTolerancePlugin = discoverAstrixFaultTolerance();
		if (faultTolerancePlugin != null) {
			plugins.registerFalutTolerancePlugin(faultTolerancePlugin);
		}
		AstrixObjectSerializerFactory objectSerializerPlugin = discoverObjectSerializerFactory();
		if (objectSerializerPlugin != null) {
			plugins.registerObjectSerializerPlugin(objectSerializerPlugin);
		}
		plugins.registerServiceProviderPlugins(discoverServiceProviderPlugins());
		return plugins;
	}
	
	

}
