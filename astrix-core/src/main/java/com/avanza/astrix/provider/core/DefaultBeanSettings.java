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
package com.avanza.astrix.provider.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Allows Overriding the default bean settings for a given Astrix bean by 
 * annotating the exported bean type.<p> 
 * 
 * Example usage 1, in service-interface definition:
 * <pre>
 * {@literal @}DefaultBeanSettings(initialTimeout = 2000)
 * public interface TradingService {
 *    // Method definitions
 * }
 * </pre>
 * 
 * Example usage 2, in Astrix bean definition:
 * <pre>
 * 
 * {@literal @}DefaultBeanSettings(initialTimeout = 3000)
 * {@literal @}Service
 * public AccountService accountService();
 * </pre>
 * 
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@Target(value={ElementType.TYPE, ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface DefaultBeanSettings {
	
	int DEFAULT_INITIAL_TIMEOUT = 1000;
	boolean DEFAULT_FAULT_TOLERANCE_ENABLED = true;
	boolean DEFAULT_BEAN_METRICS_ENABLED = true;
	
	/**
	 * See AstrixBeanSettings#INITIAL_TIMEOUT
	 */
	int initialTimeout() default DEFAULT_INITIAL_TIMEOUT;
	
	
	/**
	 * See AstrixBeanSettings#INITIAL_TIMEOUT
	 */
	boolean faultToleranceEnabled() default DEFAULT_FAULT_TOLERANCE_ENABLED;
	
	/**
	 * See AstrixBeanSettings#BEAN_METRICS_ENABLED
	 */
	boolean beanMetricsEnabled() default DEFAULT_BEAN_METRICS_ENABLED;
	
}
