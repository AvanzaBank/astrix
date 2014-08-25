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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AstrixPlugins {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixPlugins.class);
	
	private final ConcurrentMap<Class<?>, Plugin<?>> pluginsByType = new ConcurrentHashMap<>();
	private final boolean autodiscover = true;
	
	
	public <T> T getPlugin(Class<T> type) {
		Plugin<T> plugin = getPluginInstance(type);
		return plugin.getOne();
	}
	
	public <T> List<T> getPlugins(Class<T> type) {
		Plugin<T> plugin = getPluginInstance(type);
		return plugin.getAll();
	}

	public <T> void registerPlugin(Class<T> type, T provider) {
		this.pluginsByType.putIfAbsent(type, new Plugin<>(type));
		Plugin<T> plugin = (Plugin<T>) pluginsByType.get(type);
		plugin.add(provider);
	}
	
	private <T> Plugin<T> getPluginInstance(Class<T> type) {
		 Plugin<T> plugin = (Plugin<T>) pluginsByType.get(type);
		 if (plugin != null) {
			 return plugin;
		 }
		 if (autodiscover) {
			this.pluginsByType.put(type, Plugin.autoDiscover(type)); 
		 }
		 return (Plugin<T>) pluginsByType.get(type);
	}
	
	static class Plugin<T> {
		private Class<T> type;
		private List<T> providers;
		
		public Plugin(Class<T> type) {
			this(type, new LinkedList<T>());
		}

		public Plugin(Class<T> type, List<T> discoverPlugins) {
			this.type = type;
			this.providers = discoverPlugins;
		}
		
		public static <T> Plugin<T> autoDiscover(Class<T> type) {
			List<T> plugins = AstrixPluginDiscovery.discoverPlugins(type);
			if (plugins.isEmpty()) {
				Method defaultFactory = getDefaultFactory(type);
				if (defaultFactory != null) {
					try {
						log.debug("Plugin not found {}.", type);
						plugins.add((T) defaultFactory.invoke(null));
					} catch (Exception e) {
						log.warn("Failed to create default plugin for type=" + type, e);
					}
				}
			}
			return new Plugin(type, plugins);
		}
		
		// Look for inner factory class with name 'Default' and a factory method named 'create'
		private static Method getDefaultFactory(Class<?> pluginType) {
			try {
				for (Class<?> defaultFactoryCandidate : pluginType.getDeclaredClasses()) {
					if (defaultFactoryCandidate.getName().endsWith("Default")) {
						return defaultFactoryCandidate.getMethod("create");
					}
				}
			} catch (Exception e) {
				log.info("Failed to find default factory for plugin= " + pluginType, e);
			}
			return null;
		}

		public T getOne() {
			if (providers.isEmpty()) {
				throw new IllegalArgumentException("No provider registered for type: " + type);
			}
			if (providers.size() != 1) {
				throw new IllegalArgumentException("Expected one provider for: " + type + " fount: + "+ providers);
			}
			return providers.get(0);
		}

		public List<T> getAll() {
			return providers;
		}
		
		public void add(T provider) {
			this.providers.add(Objects.requireNonNull(provider));
		}
	}
	
}
