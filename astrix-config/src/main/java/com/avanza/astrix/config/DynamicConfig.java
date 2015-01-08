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
import java.util.Collections;
import java.util.List;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class DynamicConfig {

	private List<DynamicConfigSource> configSources;

	public DynamicConfig(DynamicConfigSource configSource) {
		this.configSources = Arrays.asList(configSource);
	}
	
	public DynamicConfig(List<? extends DynamicConfigSource> configSources) {
		this.configSources = new ArrayList<>(configSources);
		Collections.reverse(this.configSources);
	}

	public DynamicStringProperty getStringProperty(String name, String defaultValue) {
		final DynamicStringProperty result = new DynamicStringProperty(null);
		final DynamicPropertyChain chain = getPropertyChain(name, defaultValue, new DynamicPropertyChainListener() {
			@Override
			public void propertyChanged(String newValue) {
				result.set(newValue);
			}
		});
		result.set(chain.get());
		return result;
	}
	
	public BooleanProperty getBooleanProperty(String name, boolean defaultValue) {
		final BooleanProperty result = new BooleanProperty();
		final DynamicPropertyChain chain = getPropertyChain(name, Boolean.toString(defaultValue), new DynamicPropertyChainListener() {
			@Override
			public void propertyChanged(String newValue) {
				result.set(Boolean.parseBoolean(newValue));
			}
		});
		result.set(Boolean.parseBoolean(chain.get()));
		return result;
	}

	private DynamicPropertyChain getPropertyChain(String name, String defaultValue, DynamicPropertyChainListener dynamicPropertyListener) {
		DynamicPropertyChain chain = DynamicPropertyChain.createWithDefaultValue(defaultValue, dynamicPropertyListener);
		for (DynamicConfigSource configSource : configSources) {
			DynamicConfigProperty newValueInChain = chain.prependValue();
			String propertyValue = configSource.get(name, newValueInChain);
			newValueInChain.set(propertyValue);
		}
		return chain;
	}
	
}
