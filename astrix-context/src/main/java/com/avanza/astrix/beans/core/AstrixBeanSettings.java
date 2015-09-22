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
package com.avanza.astrix.beans.core;

import java.util.Objects;

import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.DefaultBeanSettings;

/**
 *
 * @author Elias Lindholm (elilin)
 *
 */
public final class AstrixBeanSettings {

	/**
	 * Determines whether fault tolerance should be applied for invocations on the associated
	 * Astrix bean.
	 */
	public static final BooleanBeanSetting FAULT_TOLERANCE_ENABLED = 
			new BooleanBeanSetting("faultTolerance.enabled", DefaultBeanSettings.DEFAULT_FAULT_TOLERANCE_ENABLED);
	
	/**
	 * Determines whether statistics should be collected for each invocation on the associated
	 * Astrix bean.
	 */
	public static final BooleanBeanSetting BEAN_METRICS_ENABLED = 
			new BooleanBeanSetting("beanMetrics.enabled", DefaultBeanSettings.DEFAULT_BEAN_METRICS_ENABLED);

	/**
	 * When fault tolerance is enabled this setting defines the initial timeout used
	 * for invocations on the associated bean. This setting is named "initial" to
	 * reflect the fact that updates to this bean settings at runtime will not have
	 * any effect. All runtime changes to the timeout for the associated Astrix bean
	 * should be done using the archaius configuration.    
	 */
	public static final IntBeanSetting INITIAL_TIMEOUT = 
			new IntBeanSetting("faultTolerance.timeout", DefaultBeanSettings.DEFAULT_INITIAL_TIMEOUT);
	
	/**
	 * Defines the default "maxConcurrentRequests" when semaphore isolation is used to protect invocations 
	 * to the associated bean, i.e. the maximum number of concurrent requests before the 
	 * fault-tolerance layer starts rejecting invocations (by throwing ServiceUnavailableException)
	 */
	public static final IntBeanSetting INITIAL_MAX_CONCURRENT_REQUESTS = 
			new IntBeanSetting("faultTolerance.initialMaxConcurrentRequests", DefaultBeanSettings.DEFAULT_INITIAL_MAX_CONCURRENT_REQUESTS);
	
	/**
	 * Defines the default "coreSize" when thread isolation is used to protect invocations 
	 * to the associated bean, i.e. the number of threads in the bulkhead associated with a
	 * synchronous service invocation.
	 */
	public static final IntBeanSetting INITIAL_CORE_SIZE = 
			new IntBeanSetting("faultTolerance.initialCoreSize", DefaultBeanSettings.DEFAULT_INITIAL_CORE_SIZE);
	
	/**
	 * Defines the default "queueSizeRejectionThreshold" for the queue when thread isolation 
	 * is used to protect invocations to the associated bean, i.e. number of pending service invocations
	 * allowed in the queue to a thread-pool (bulk-head) before starting to throw {@link ServiceUnavailableException}.
	 */
	public static final IntBeanSetting INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD = 
			new IntBeanSetting("faultTolerance.initialQueueSizeRejectionThreshold", DefaultBeanSettings.DEFAULT_INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD);

	
	/**
	 * Its possible to set service-beans in unavailable state, in which it throws
	 * a {@link ServiceUnavailableException} on each invocation. <p>
	 */
	public static final BooleanBeanSetting AVAILABLE = 
			new BooleanBeanSetting("available", true);

	
	private AstrixBeanSettings() {
	}

	public static abstract class BeanSetting<T> {
		private String name;
		private T defaultValue;

		private BeanSetting(String name, T defaultValue) {
			this.name = name;
			this.defaultValue = Objects.requireNonNull(defaultValue);
		}

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
		
		public T defaultValue() {
			return defaultValue;
		}
	}

	public static class BooleanBeanSetting extends BeanSetting<Boolean> {
		public BooleanBeanSetting(String name, boolean defaultValue) {
			super(name, defaultValue);
		}
	}

	public static class LongBeanSetting extends BeanSetting<Long> {
		public LongBeanSetting(String name, long defaultValue) {
			super(name, defaultValue);
		}
	}

	public static class IntBeanSetting extends BeanSetting<Integer> {
		public IntBeanSetting(String name, int defaultValue) {
			super(name, defaultValue);
		}
	}

}