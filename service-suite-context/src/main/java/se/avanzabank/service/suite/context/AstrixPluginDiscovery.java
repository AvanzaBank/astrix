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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AstrixPluginDiscovery {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixPluginDiscovery.class);
	
	public static List<AstrixServiceProviderPlugin> discoverServiceProviderPlugins() {
		Iterator<AstrixServiceProviderPlugin> serviceProviderPlugins = ServiceLoader.load(AstrixServiceProviderPlugin.class).iterator();
		List<AstrixServiceProviderPlugin> result = new ArrayList<>();
		while (serviceProviderPlugins.hasNext()) {
			AstrixServiceProviderPlugin serviceProviderPlugin = serviceProviderPlugins.next();
			result.add(serviceProviderPlugin);
		}
		return result;
	}
	
	public static AstrixVersioningPlugin discoverObjectSerializerFactory() {
		Iterator<AstrixVersioningPlugin> serializerFactory = ServiceLoader.load(AstrixVersioningPlugin.class).iterator();
		if (!serializerFactory.hasNext()) {
			return null;
		}
		return serializerFactory.next();
	}
	
	static <T> void discoverAllPlugins(AstrixContext context, Class<T> type, T defaultProvider) {
		List<T> plugins = discoverPlugins(type);
		if (plugins.isEmpty()) {
			log.debug("No plugin discovered for {}, using default {}", type.getName(), defaultProvider.getClass().getName());
			plugins.add(defaultProvider);
		}
		for (T plugin : plugins) {
			log.debug("Found plugin for {}, provider={}", type.getName(), plugin.getClass().getName());
			context.registerProvider(type, plugin);
		}
	}

	static <T> void discoverPlugin(AstrixContext context, Class<T> type, T defaultProvider) {
		T plugin = discoverPlugin(type);
		if (plugin == null) {
			log.debug("No plugin discovered for {}, using default {}", type.getName(), defaultProvider.getClass().getName());
			context.registerProvider(type, defaultProvider);
		} else {
			log.debug("Found plugin for {}, using {}", type.getName(), plugin.getClass().getName());
			context.registerProvider(type, plugin);
		}
	}
	
	static <T> void discoverOnePlugin(AstrixContext context, Class<T> type) {
		T plugin = discoverPlugin(type);
		if (plugin == null) {
			throw new IllegalStateException("No provider found on classpath for: " + type + ". This typically means that you forgot to put the providing jar on the classpath.");
		}
		log.debug("Found plugin for {}, using {}", type.getName(), plugin.getClass().getName());
		context.registerProvider(type, plugin);
	}
	
	
	public static <T> T discoverPlugin(Class<T> type) {
		Iterator<T> plugins = ServiceLoader.load(type).iterator();		
		if (!plugins.hasNext()) {
			return null;
		}
		return plugins.next(); // TODO: detect config error (more than one provider)... 
	}
	
	public static <T> List<T> discoverPlugins(Class<T> type) {
		Iterator<T> plugins = ServiceLoader.load(type).iterator();		
		List<T> result = new ArrayList<>();
		while (plugins.hasNext()) {
			result.add(plugins.next());
		}
		return result; 
	}
	

}
