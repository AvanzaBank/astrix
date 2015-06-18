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
package com.avanza.astrix.context.module;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ModuleDiscovery {
	
	private static Logger log = LoggerFactory.getLogger(ModuleDiscovery.class);
	
	// TODO: remove?
	private static <T> T discoverOnePlugin(Class<T> type) {
		List<T> plugins = discoverAllPlugins(type);
		if (plugins.isEmpty()) {
			throw new IllegalStateException("No provider found on classpath for: " + type + ". This typically means that you forgot to put the providing jar on the classpath.");
		}
		if (plugins.size() > 1) {
			throw new IllegalStateException("Multiple providers found for plugin: " + type + ". Plugins: " + plugins);
		}
		return plugins.get(0);
	}
	
	static List<Module> loadModules() {
		Iterator<Module> modules = ServiceLoader.load(Module.class).iterator();		
		List<Module> result = new ArrayList<>();
		while (modules.hasNext()) {
			Module module = modules.next();
			log.debug("Discovered module. module={} pluginProviderType={}", module.getClass().getName());
			result.add(module);
		}
		return result; 
	}

	static <T> List<T> discoverAllPlugins(Class<T> type) {
		Iterator<T> plugins = ServiceLoader.load(type).iterator();		
		List<T> result = new ArrayList<>();
		while (plugins.hasNext()) {
			T plugin = plugins.next();
			log.debug("Discovered plugin instance, pluginType={} pluginProviderType={}", type.getName(), plugin.getClass().getName());
			result.add(plugin);
		}
		return result; 
	}
	

}
