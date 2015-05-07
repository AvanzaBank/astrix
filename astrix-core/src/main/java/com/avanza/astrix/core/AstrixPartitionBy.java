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
package com.avanza.astrix.core;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Collection;


/**
 * Indicates that a remote invocation request should be
 * partitioned on a given argument (The argument must be a subclass
 * of {@link Collection}). 
 * 
 * The request will be partitioned by splitting the request
 * into one request against each server partition (provided that
 * there are keys in the target Collection routed to the given partition).
 * 
 * @author Elias Lindholm (elilin)
 */
@Target(value={ElementType.PARAMETER})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixPartitionBy {
	
	@SuppressWarnings("rawtypes")
	Class<? extends AstrixRemoteResultReducer> reducer() default DefaultAstrixRemoteRestultReducer.class;
	
}
