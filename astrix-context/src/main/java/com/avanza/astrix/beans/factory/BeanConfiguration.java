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
package com.avanza.astrix.beans.factory;

import java.util.Map;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.LongBeanSetting;
import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.config.DynamicLongProperty;
/**
 * @author Elias Lindholm (elilin)
 */
public final class BeanConfiguration {
	
	private final AstrixBeanKey<?> beanKey;
	private final DynamicConfig config;
	private final Map<BeanSetting<?>, Object> defaultBeanSettingsOverride;
	
	public BeanConfiguration(AstrixBeanKey<?> beanKey, DynamicConfig config, Map<BeanSetting<?>, Object> defaultBeanSettingsOverride) {
		this.beanKey = beanKey;
		this.config = config;
		this.defaultBeanSettingsOverride = defaultBeanSettingsOverride;
	}

	public DynamicIntProperty get(IntBeanSetting setting) {
		Object defaultOverride = defaultBeanSettingsOverride.get(setting);
		if (defaultOverride != null) {
			return config.getIntProperty(setting.nameFor(beanKey), Integer.class.cast(defaultOverride).intValue());
		}
		return config.getIntProperty(setting.nameFor(beanKey), setting.defaultValue());
	}
	
	public DynamicLongProperty get(LongBeanSetting setting) {
		Object defaultOverride = defaultBeanSettingsOverride.get(setting);
		if (defaultOverride != null) {
			return config.getLongProperty(setting.nameFor(beanKey), Long.class.cast(defaultOverride).longValue());
		}
		return config.getLongProperty(setting.nameFor(beanKey), setting.defaultValue());
	}
	
	public DynamicBooleanProperty get(BooleanBeanSetting setting) {
		Object defaultOverride = defaultBeanSettingsOverride.get(setting);
		if (defaultOverride != null) {
			return config.getBooleanProperty(setting.nameFor(beanKey), Boolean.class.cast(defaultOverride).booleanValue());
		}
		return config.getBooleanProperty(setting.nameFor(beanKey), setting.defaultValue());
	}
	
	public AstrixBeanKey<?> getBeanKey() {
		return beanKey;
	}
}
