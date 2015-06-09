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
import java.util.ServiceLoader;


/**
 * An AstrixPlugin is an extension point of Astrix.
 * 
 * Astrix uses plugins for abstractions where there is expected that multiple providers
 * exists at the same time. This is different from an {@link AstrixStrategy} 
 * which is also an extension point, but which only expects a single provider 
 * of the given strategy at runtime.  
 * 
 * Astrix uses the {@link ServiceLoader}
 * mechanism in java to discover plugins.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
@Target(value={ElementType.TYPE})
@Retention(value=RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixPlugin {
	
}
