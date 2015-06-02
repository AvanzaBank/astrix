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

import com.avanza.astrix.config.DynamicBooleanProperty;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicIntProperty;
import com.avanza.astrix.config.DynamicLongProperty;
import com.avanza.astrix.config.DynamicProperty;

/**
 *
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixBeanSettings {

	/**
	 * Determines whether fault tolerance should be applied for invocations on the associated
	 * Astrix bean.
	 */
	public static final BooleanBeanSetting FAULT_TOLERANCE_ENABLED = new BooleanBeanSetting(
			"faultTolerance.enabled", true);

	/**
	 * When fault tolerance is enabled this setting defines the initial timeout used
	 * for invocations on the associated bean. This setting is named "initial" to
	 * reflect the fact that updates to this bean settings at runtime will not have
	 * any effect. All runtime changes to the timeout for the associated Astrix bean
	 * should be done using the archauis configuration.    
	 */
	public static final IntBeanSetting INITIAL_TIMEOUT = new IntBeanSetting(
			"faultTolerance.timeout", 1000);

	public static abstract class BeanSetting<T extends DynamicProperty<?>> {
		private String name;

		public BeanSetting(String name) {
			this.name = name;
		}

		public T getFor(AstrixBeanKey<?> beanKey, DynamicConfig config) {
			return getProperty(resolveSettingName(beanKey), config);
		}

		protected abstract T getProperty(String setting, DynamicConfig config);
		

		public String nameFor(AstrixBeanKey<?> beanKey) {
			return resolveSettingName(beanKey);
		}

		private String resolveSettingName(AstrixBeanKey<?> beanKey) {
			if (beanKey.isQualified()) {
				return "astrix.bean." + beanKey.getBeanType().getName() + "."
						+ beanKey.getQualifier() + "." + name;
			}
			return "astrix.bean." + beanKey.getBeanType().getName() + "."
					+ name;
		}
	}

	public static class BooleanBeanSetting extends
			BeanSetting<DynamicBooleanProperty> {
		private final boolean defaultValue;

		public BooleanBeanSetting(String name, boolean defaultValue) {
			super(name);
			this.defaultValue = defaultValue;
		}

		@Override
		protected DynamicBooleanProperty getProperty(String name,
				DynamicConfig config) {
			return config.getBooleanProperty(name, defaultValue);
		}
	}

	public static class LongBeanSetting extends
			BeanSetting<DynamicLongProperty> {
		private final long defaultValue;

		public LongBeanSetting(String name, long defaultValue) {
			super(name);
			this.defaultValue = defaultValue;
		}

		@Override
		protected DynamicLongProperty getProperty(String name,
				DynamicConfig config) {
			return config.getLongProperty(name, defaultValue);
		}
	}

	public static class IntBeanSetting extends BeanSetting<DynamicIntProperty> {
		private final int defaultValue;

		public IntBeanSetting(String name, int defaultValue) {
			super(name);
			this.defaultValue = defaultValue;
		}

		@Override
		protected DynamicIntProperty getProperty(String name,
				DynamicConfig config) {
			return config.getIntProperty(name, defaultValue);
		}
	}

}