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
package com.avanza.astrix.context;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.ServiceLoader;

class AstrixPluginDiscovery {
	
	static <T> T discoverOnePlugin(Class<T> type) {
		List<T> plugins = discoverAllPlugins(type);
		if (plugins.isEmpty()) {
			throw new IllegalStateException("No provider found on classpath for: " + type + ". This typically means that you forgot to put the providing jar on the classpath.");
		}
		if (plugins.size() > 1) {
			throw new IllegalStateException("Multiple providers found for plugin: " + type + ". Plugins: " + plugins);
		}
		return plugins.get(0);
	}

	static <T> List<T> discoverAllPlugins(Class<T> type) {
		Iterator<T> plugins = ServiceLoader.load(type).iterator();		
		List<T> result = new ArrayList<>();
		while (plugins.hasNext()) {
			result.add(plugins.next());
		}
		return result; 
	}
	

}
