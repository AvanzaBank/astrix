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
package com.avanza.asterix.context;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.asterix.provider.core.AsterixPluginQualifier;

public class AsterixPlugins {
	
	private static final Logger log = LoggerFactory.getLogger(AsterixPlugins.class);
	
	private final ConcurrentMap<Class<?>, Plugin<?>> pluginsByType = new ConcurrentHashMap<>();
	private final boolean autodiscover = true;
	private final AsterixPluginInitializer pluginInitializer;
	
	public AsterixPlugins(AsterixPluginInitializer plugininitializer) {
		this.pluginInitializer = plugininitializer;
	}

	/**
	 * Retrieves a plugin which there is expected to exist exactly one instance of
	 * at runtime. Zero or multiple instance is a runtime configuration error. <p>
	 * 
	 * @param type
	 * @return
	 */
	public <T> T getPlugin(Class<T> type) {
		Plugin<T> plugin = getPluginHolder(type);
		return plugin.getOne();
	}
	
	/**
	 * Returns all plugins of a given type. This is used to locate all runtime instances
	 * of a given plugin. 
	 * 
	 * @param type
	 * @return
	 */
	public <T> List<T> getPlugins(Class<T> type) {
		Plugin<T> plugin = getPluginHolder(type);
		return plugin.getAll();
	}
	
	public <T> void registerPlugin(Class<T> pluginType, T pluginProvider) {
		this.pluginInitializer.init(pluginProvider);
		this.pluginsByType.putIfAbsent(pluginType, new Plugin<>(pluginType));
		Plugin<T> plugin = (Plugin<T>) pluginsByType.get(pluginType);
		plugin.add(pluginProvider);
	}
	
	private <T> Plugin<T> getPluginHolder(Class<T> type) {
		 Plugin<T> plugin = (Plugin<T>) pluginsByType.get(type);
		 if (plugin != null) {
			 return plugin;
		 }
		 if (autodiscover) {
			this.pluginsByType.put(type, Plugin.autoDiscover(type, pluginInitializer)); 
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
			for (Object pluginProvider : providers) {
				
			}
		}
		
		public Class<T> getType() {
			return type;
		}
		
		public static <T> Plugin<T> autoDiscover(Class<T> type, AsterixPluginInitializer initializer) {
			List<T> plugins = AsterixPluginDiscovery.discoverAllPlugins(type);
			if (plugins.isEmpty()) {
				Method defaultFactory = getDefaultFactory(type);
				if (defaultFactory != null) {
					try {
						log.debug("Plugin not found {}. Using default factory to create {}", type, defaultFactory);
						plugins.add((T) defaultFactory.invoke(null));
					} catch (Exception e) {
						log.warn("Failed to create default plugin for type=" + type, e);
					}
				}
			}
			for (T plugin : plugins) {
				initializer.init(plugin);
			}
			return new Plugin<T>(type, plugins);
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
		
		public T getOne(String qualifier) {
			for (T provider : getAll()) {
				if (provider.getClass().isAnnotationPresent(AsterixPluginQualifier.class)) {
					AsterixPluginQualifier candidate = provider.getClass().getAnnotation(AsterixPluginQualifier.class);
					if (candidate.value().equals(qualifier)) {
						return provider;
					}
				}
			}
			throw new IllegalArgumentException(String.format("Found no provider of type: %s with qualifier %s", this.type, qualifier));
		}

		public List<T> getAll() {
			return providers;
		}
		
		public void add(T provider) {
			this.providers.add(Objects.requireNonNull(provider));
		}

	}

	public <T> T getPlugin(Class<T> pluginType, String qualifier) {
		Plugin<T> plugin = getPluginHolder(pluginType);
		return plugin.getOne(qualifier);
	}
	
}
