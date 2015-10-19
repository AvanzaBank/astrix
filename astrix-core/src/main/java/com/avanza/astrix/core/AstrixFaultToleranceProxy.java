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
package com.avanza.astrix.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * This annotation may be put on @Library annotated methods in 
 * a AstrixApiProvider to decorate a library with a fault tolerance
 * proxy. <p>
 * 
 * @author Elias Lindholm (elilin)
 */
@Target(value={ElementType.METHOD})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixFaultToleranceProxy {
	/**
	 * Command key name for the hystrix command that will protect
	 * the target method.
	 * 
	 * @deprecated - Not read, uses bean type as command key
	 */
	@Deprecated
	String commandKey() default "";
	
	/**
	 * Command group name for the hystrix command that will
	 * protect the target method.
	 * 
	 * @deprecated - Not read, uses @AstrixApiProvider annotated class-name as group key
	 */
	@Deprecated
	String groupKey() default "";
}
