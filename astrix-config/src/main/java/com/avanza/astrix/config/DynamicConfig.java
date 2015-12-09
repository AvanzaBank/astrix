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
package com.avanza.astrix.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * This is an abstraction for a hierarchical set of configuration sources. Each property is resolved
 * by querying each ConfigurationSource in turn until the property is resolved. <p>
 * 
 * Each {@link DynamicProperty} read is cached in the {@link DynamicConfig} instance. The first time a property
 * with a given name is read, an instance of the given {@link DynamicProperty} type is created, and its value is
 * bound to the underlying configuration sources.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class DynamicConfig {

	private final ObjectCache configCache = new ObjectCache();
	private final List<DynamicConfigSource> configSources;
	private final ListenerSupport<DynamicConfigListener> dynamicConfigListenerSupport = new ListenerSupport<>();

	public DynamicConfig(ConfigSource configSource) {
		this(Arrays.asList(configSource));
	}
	
	/**
	 * Creates a {@link DynamicConfig} instance resolving configuration properties using
	 * the defined set of {@link ConfigSource}'s (possibly {@link DynamicConfigSource}). <p>
	 * 
	 * @param first
	 * @param other
	 * @return
	 */
	public static DynamicConfig create(ConfigSource first, ConfigSource... other) {
		List<ConfigSource> sources = new LinkedList<>();
		sources.add(first);
		sources.addAll(Arrays.asList(other));
		return new DynamicConfig(sources);
	}
	

	public static DynamicConfig create(List<? extends ConfigSource> sources) {
		return new DynamicConfig(sources);
	}
	
	public DynamicConfig(List<? extends ConfigSource> configSources) {
		this.configSources = new ArrayList<>(configSources.size());
		for (ConfigSource configSource : configSources) {
			if (configSource instanceof DynamicConfigSource) {
				this.configSources.add(DynamicConfigSource.class.cast(configSource));
			} else {
				this.configSources.add(new DynamicConfigSourceAdapter(configSource));
			}
		}
	}
	
	private static class DynamicConfigSourceAdapter extends AbstractDynamicConfigSource {
		private final ConfigSource configSource;
		public DynamicConfigSourceAdapter(ConfigSource configSource) {
			this.configSource = configSource;
		}
		@Override
		public String get(String propertyName, DynamicPropertyListener<String> propertyChangeListener) {
			return configSource.get(propertyName);
		}
		
		@Override
		public String toString() {
			return this.configSource.toString();
		}
		
	}

	/**
	 * Reads a property of String type.
	 * 
	 * @param name
	 * @return
	 */
	public DynamicStringProperty getStringProperty(String name, String defaultValue) {
		return getProperty(name, DynamicStringProperty.class, defaultValue, PropertyParser.STRING_PARSER);
	}
	
	public DynamicBooleanProperty getBooleanProperty(String name, boolean defaultValue) {
		return getProperty(name, DynamicBooleanProperty.class, defaultValue, PropertyParser.BOOLEAN_PARSER);
	}
	
	public DynamicLongProperty getLongProperty(String name, long defaultValue) {
		return getProperty(name, DynamicLongProperty.class, defaultValue, PropertyParser.LONG_PARSER);
	}
	
	public DynamicIntProperty getIntProperty(String name, int defaultValue) {
		return getProperty(name, DynamicIntProperty.class, defaultValue, PropertyParser.INT_PARSER);
	}

	private <T, P extends DynamicProperty<T>> P getProperty(String name, Class<P> propertyType, T defaultValue, PropertyParser<T> propertyParser) {
		return this.configCache.getInstance(propertyType.getSimpleName() + "." + name, 
				() -> bindPropertyToConfigurationSources(name, propertyType.newInstance(), defaultValue, propertyParser));
	}

	private <T, P extends DynamicProperty<T>> P bindPropertyToConfigurationSources(String name, P property, T defaultValue, PropertyParser<T> propertyParser) {
		DynamicPropertyChain<T> chain = createPropertyChain(name, defaultValue, propertyParser);
		chain.bindTo(property::setValue);
		notifyPropertyCreated(name, property.getCurrentValue());
		property.addListener(newValue -> notifyPropertyChanged(name, newValue));
		return property;
	}

	private <T> void notifyPropertyCreated(String propertyName, T initialValue) {
		dynamicConfigListenerSupport.dispatchEvent(listener -> listener.propertyCreated(propertyName, initialValue));
	}

	private <T> void notifyPropertyChanged(String propertyNAme, T newValue) {
		dynamicConfigListenerSupport.dispatchEvent(listener -> listener.propertyChanged(propertyNAme, newValue));
	}
	
	private <T> DynamicPropertyChain<T> createPropertyChain(String name, T defaultValue, PropertyParser<T> propertyParser) {
		DynamicPropertyChain<T> chain = DynamicPropertyChain.createWithDefaultValue(defaultValue, propertyParser);
		for (DynamicConfigSource configSource : configSources) {
			DynamicConfigProperty<T> newValueInChain = chain.appendValue();
			// bind newValueInChain to configuration property in source
			String propertyValue = configSource.get(name, newValueInChain);
			newValueInChain.set(propertyValue);
		}
		return chain;
	}

	public static DynamicConfig merged(DynamicConfig dynamicConfigA, DynamicConfig dynamicConfigB) {
		List<ConfigSource> merged = new ArrayList<>(dynamicConfigA.configSources.size() + dynamicConfigB.configSources.size());
		merged.addAll(dynamicConfigA.configSources);
		merged.addAll(dynamicConfigB.configSources);
		return new DynamicConfig(merged);
	}
	
	@Override
	public String toString() {
		return this.configSources.toString();
	}

	/**
	 * Adds a listener to this DynamicConfig instance. 
	 * 
	 * The listener receives a "propertyChanged" event each time the resolved
	 * value of a DynamicProperty read
	 * from this instance changes. 
	 * 
	 * The listener receives a "propertyCreated" each time a new property
	 * is created in this {@link DynamicConfig} instance (i.e the first time
	 * a property with a given name is read).
	 * 
	 * @param l
	 */
	public void addListener(DynamicConfigListener l) {
		this.dynamicConfigListenerSupport.addListener(l);
	}

}
