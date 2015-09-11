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
package com.avanza.astrix.beans.config;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.config.DynamicConfig;
/**
 * @author Elias Lindholm (elilin)
 */
final class BeanConfigurationsImpl implements AstrixConfigAware, BeanConfigurations {
	
	private DynamicConfig config;
	private final Map<AstrixBeanKey<?>, Map<BeanSetting<?>, Object>> beanSettingByType = new ConcurrentHashMap<>();

	public BeanConfiguration getBeanConfiguration(AstrixBeanKey<?> beanKey) {
		Map<BeanSetting<?>, Object> defaultBeanSettingsOverride = beanSettingByType.get(beanKey);
		if (defaultBeanSettingsOverride == null) {
			defaultBeanSettingsOverride = Collections.emptyMap();
		}
		return new BeanConfiguration(beanKey, config, defaultBeanSettingsOverride);
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.config = config;
	}

	@Override
	public void setDefaultBeanConfig(AstrixBeanKey<?> beanKey,
			Map<BeanSetting<?>, Object> defaultBeanSettingsOverride) {
		this.beanSettingByType.put(beanKey, defaultBeanSettingsOverride);
	}
	
}
