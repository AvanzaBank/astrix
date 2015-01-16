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


/**
 * @author Elias Lindholm (elilin)
 */
@Target(value = { ElementType.TYPE})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixApiProvider {

	/**
	 * @return
	 * @deprecated Don't split api into "api-definition" class and "api-provider" class anymore.
	 * 
	 * For instance, dont do this:
	 * 
	 * class MyApi {
	 *     @Service
	 *     MySevice service();
	 * }
	 * 
	 * @AstrixApiProvider(MyApi.class)
	 * class MyApiProvider {
	 * 
	 * }
	 * 
	 * Put the AstrixApiProvider annotation directly on the same class/interface that defines the api.
	 * 
	 * For instance:
	 * 
	 * @AstrixApiProvider
	 * interface MyServiceProvider {
	 *     @Service
	 *     MyService service();
	 *     
	 *     // Other service definition
	 * }
	 * 
	 * Or for a library:
	 * 
	 * @AstrixApiProvider
	 * class MyLibProvider {
	 *     @Library
	 *     MyLibrary lib() {
	 *         return new MyLibImpl();
	 *     }
	 *     
	 *     // Other library definitions
	 * }
	 * 
	 * 
	 * 
	 */
	@Deprecated
	Class<?>[] value() default {};
	
}
