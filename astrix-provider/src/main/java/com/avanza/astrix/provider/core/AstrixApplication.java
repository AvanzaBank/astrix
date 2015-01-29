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
package com.avanza.astrix.provider.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.avanza.astrix.provider.component.AstrixServiceComponentNames;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@Target(value = { ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixApplication {
	
	/**
	 * Identifies what services this application provides by pointing to
	 * a list of AstrixApiProvider's. All defined services (@Service annotated methods)
	 * will be provided by the current application.
	 * 
	 * @return
	 */
	Class<?>[] apiDescriptors();

	/**
	 * Default {@link AstrixServiceComponent} to use when exporting services provided by this application,
	 * see {@link AstrixServiceComponentNames} for well known AstrixServiceComponent's.
	 * 
	 * 
	 * @return
	 */
	String component();
	
}
