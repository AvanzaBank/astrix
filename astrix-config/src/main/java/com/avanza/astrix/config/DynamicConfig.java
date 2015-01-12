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
package com.avanza.astrix.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class DynamicConfig {

	private final List<DynamicConfigSource> configSources;

	public DynamicConfig(ConfigSource configSource) {
		this(Arrays.asList(configSource));
	}
	
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

	public DynamicStringProperty getStringProperty(String name, String defaultValue) {
		final DynamicStringProperty result = new DynamicStringProperty(null);
		final DynamicPropertyChain<String> chain = getPropertyChain(name, defaultValue, new DynamicPropertyChainListener<String>() {
			@Override
			public void propertyChanged(String newValue) {
				result.set(newValue);
			}
		}, new PropertyParser.StringParser());
		result.set(chain.get());
		return result;
	}
	
	public DynamicBooleanProperty getBooleanProperty(String name, boolean defaultValue) {
		final DynamicBooleanProperty result = new DynamicBooleanProperty();
		final DynamicPropertyChain<Boolean> chain = getPropertyChain(name, defaultValue, new DynamicPropertyChainListener<Boolean>() {
			@Override
			public void propertyChanged(Boolean newValue) {
				result.set(newValue.booleanValue());
			}
		}, new PropertyParser.BooleanParser());
		result.set(chain.get());
		return result;
	}
	
	public DynamicLongProperty getLongProperty(String name, long deafualtValue) {
		final DynamicLongProperty result = new DynamicLongProperty();
		final DynamicPropertyChain<Long> chain = getPropertyChain(name, Long.valueOf(deafualtValue), new DynamicPropertyChainListener<Long>() {
			@Override
			public void propertyChanged(Long newValue) {
				result.set(newValue.longValue());
			}
		}, new PropertyParser.LongParser());
		result.set(chain.get());
		return result;
	}
	
	private <T> DynamicPropertyChain<T> getPropertyChain(String name, T defaultValue, DynamicPropertyChainListener<T> dynamicPropertyListener, PropertyParser<T> propertyParser) {
		DynamicPropertyChain<T> chain = DynamicPropertyChain.createWithDefaultValue(defaultValue, dynamicPropertyListener, propertyParser);
		for (DynamicConfigSource configSource : configSources) {
			DynamicConfigProperty<T> newValueInChain = chain.appendValue();
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
	
}
