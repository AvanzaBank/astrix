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
import java.util.ArrayList;
import java.util.Collection;


/**
 * Indicates that a remote invocation request should be
 * partitioned and routed on a given argument. The argument must be a subclass
 * of {@link Collection}, or an array type.
 * 
 * The request will be partitioned by splitting the request
 * into one request against each server partition containing only
 * the arguments in the target collection that are routed to the
 * given server partition.
 * 
 * @author Elias Lindholm (elilin)
 */
@Target(value={ElementType.PARAMETER})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixPartitionedRouting {
	
	/**
	 * If defined, then the given method will be invoked on each element in the
	 * target collection to decide what partition a given element should be routed
	 * to.
	 * 
	 * The target method must take zero arguments and have non-void return type.
	 * 
	 * @return
	 */
	String routingMethod() default "";
	
	/**
	 * AstrixRemoteResultReducer to use to reduce the response from each server partition
	 * into a return value. <p>
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends RemoteResultReducer> reducer() default DefaultAstrixRemoteResultReducer.class;

	/**
	 * Factory used to create instances of the partitioned argument Collection. Default to ArrayList.
	 * If ArrayList is not a compatible type for the given argument then this property must be
	 * set to a Class that is compatible with the partitioned argument container. The Class must
	 * have a zero argument constructor.
	 */
	@SuppressWarnings("rawtypes")
	Class<? extends Collection> collectionFactory() default ArrayList.class;
	
}
