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
package com.avanza.astrix.spring;

import org.springframework.beans.factory.FactoryBean;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.SystemPropertiesConfigSource;

public class DynamicConfigFactory implements FactoryBean<DynamicConfig> {
	
	private String configSourceId;
	
	@Override
	public DynamicConfig getObject() throws Exception {
		if (configSourceId != null) {
			return DynamicConfig.create(GlobalConfigSourceRegistry.getConfigSource(configSourceId));
		}
		return createDefault();
	}

	public void setConfigSourceId(String configSourceId) {
		this.configSourceId = configSourceId;
	}

	protected DynamicConfig createDefault() {
		return DynamicConfig.create(new SystemPropertiesConfigSource());
	}

	@Override
	public Class<?> getObjectType() {
		return DynamicConfig.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}


}
