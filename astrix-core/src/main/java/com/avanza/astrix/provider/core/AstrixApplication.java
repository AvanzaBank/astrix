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

import com.avanza.astrix.provider.component.AstrixServiceComponentNames;

/**
 * Used to define what services a given Astrix server provides.<p>
 * 
 * Note that @AstrixApplication annotated classes are not found using
 * classpath scanning. To load the server part of the framework and start
 * providing services defined by an @AstrixApplication the annotated
 * class must be explicitly identified, for instance by setting the 
 * com.avanza.astrix.spring.AstrixFrameworkBean#setApplicationDescriptor 
 * property.<p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@Target(value = { ElementType.TYPE })
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixApplication {
	
	/**
	 * Defines a list of AstrixApiProviders (i.e api's) that this application provides remote service endpoints
	 * for. All services defined in the given api's (@Service annotated methods) will be
	 * provided by this application.
	 * 
	 * @return
	 */
	Class<?>[] exportsRemoteServicesFor() default {};
	
	/**
	 * Defines a list of AstrixApiProviders that this application provides remote service endpoints
	 * for. All services defined in the given api's (@Service annotated methods) will be
	 * provided by this application.
	 * 
	 * @return
	 * @deprecated renamed to exportsRemoteServicesFor
	 */
	@Deprecated
	Class<?>[] apiDescriptors() default {};

	/**
	 * Default serviceComponent to use when exporting services provided by this application,
	 * see {@link AstrixServiceComponentNames} for well known ServiceComponent's.
	 * 
	 * This might be overriden on a per service basis by setting the {@link Service#value()} property
	 * on a given service.
	 * 
	 * @return
	 */
	String defaultServiceComponent() default "";
	
	/**
	 * Default component to use when exporting services provided by this application,
	 * see {@link AstrixServiceComponentNames} for well known ServiceComponent's.
	 * 
	 * This might be overriden on a per service basis by setting the {@link Service#value()} property
	 * on a given service.
	 * 
	 * @return
	 * @deprecated renamed to defaultServiceComponent
	 */
	@Deprecated
	String component() default "";
	
}
