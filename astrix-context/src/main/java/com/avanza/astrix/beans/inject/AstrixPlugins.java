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
package com.avanza.astrix.beans.inject;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.provider.core.AstrixPluginQualifier;

public class AstrixPlugins {
	
	private static final Logger log = LoggerFactory.getLogger(AstrixPlugins.class);
	
	private final ConcurrentMap<Class<?>, Plugin<?>> pluginsByType = new ConcurrentHashMap<>();
	private final boolean autodiscover = true;
	
	public AstrixPlugins() {
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
		registerPlugin(new Plugin<T>(pluginType, Arrays.asList(pluginProvider)));
	}
	

	public <T> void registerPlugin(Plugin<T> plugin) {
		Class<T> pluginType = plugin.getType();
		this.pluginsByType.putIfAbsent(pluginType, new Plugin<>(pluginType));
		Plugin<T> pluginHolder = (Plugin<T>) pluginsByType.get(pluginType);
		for (T pluginInstance : plugin.getAll()) {
			pluginHolder.add(pluginInstance);
		}
	}
	
	private <T> Plugin<T> getPluginHolder(Class<T> type) {
		 Plugin<T> plugin = (Plugin<T>) pluginsByType.get(type);
		 if (plugin != null) {
			 return plugin;
		 }
		 if (autodiscover) {
			this.pluginsByType.put(type, Plugin.autoDiscover(type)); 
		 }
		 return (Plugin<T>) pluginsByType.get(type);
	}
	
	public static class Plugin<T> {
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
		
		public static <T> Plugin<T> autoDiscover(Class<T> type) {
			List<T> plugins = AstrixPluginDiscovery.discoverAllPlugins(type);
			if (plugins.isEmpty()) {
				Method defaultFactory = getDefaultFactory(type);
				if (defaultFactory != null) {
					try {
						log.warn("Plugin not found {}. Using default factory to create {}", type, defaultFactory);
						plugins.add((T) defaultFactory.invoke(null));
					} catch (Exception e) {
						log.warn("Failed to create default plugin for type=" + type, e);
					}
				}
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
				throw new IllegalArgumentException("Expected one provider for: " + type.getName() + " found: + "+ providers);
			}
			return providers.get(0);
		}
		
		public T getOne(String qualifier) {
			for (T provider : getAll()) {
				if (provider.getClass().isAnnotationPresent(AstrixPluginQualifier.class)) {
					AstrixPluginQualifier candidate = provider.getClass().getAnnotation(AstrixPluginQualifier.class);
					if (candidate.value().equals(qualifier)) {
						return provider;
					}
				}
			}
			throw new IllegalArgumentException(String.format("Found no provider of type: %s with qualifier \"%s\"", this.type, qualifier));
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

	public <T> T getPluginInstance(Class<T> providerType) {
		for (Class<?> pluginTypeCandidate : providerType.getInterfaces()) {
			for (Object pluginProvider : getPlugins(pluginTypeCandidate)) {
				if (pluginProvider.getClass().equals(providerType)) {
					return providerType.cast(pluginProvider);
				}
			}
		}
		throw new IllegalArgumentException("Plugin provider not found: " + providerType.getName());
	}
	
	public <T> T getPluginInstance(Class<T> pluginInterface, Class<? extends T> pluginProviderType) {
		for (Object candidate : getPlugins(pluginInterface)) {
			if (candidate.getClass().equals(pluginProviderType)) {
				return  pluginInterface.cast(candidate);
			}
		}
		throw new IllegalArgumentException("Plugin provider not found: " + pluginProviderType.getName());
	}
	
}
