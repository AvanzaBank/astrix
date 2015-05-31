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
 * Annotating a service bean definition with this annotation indicates
 * that providers of the given service should be discovered using the configuration
 * rather than the default behavior of using the service registry. <p>
 * 
 * @author Elias Lindholm (elilin)
 */
@Target(value = { ElementType.TYPE, ElementType.METHOD })
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixConfigDiscovery {
	/**
	 * Name of the configuration entry to lookup when discovering a provider. <p> 
	 * 
	 * The configuration entry stored under the given name should be
	 * a valid serviceUri locating a provider of the defined service. <p>
	 */
	String value();
}
