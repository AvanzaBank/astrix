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
package com.avanza.astrix.context;

import com.avanza.astrix.provider.core.AstrixApiProvider;

/**
 * Creates and caches instances of Astrix beans. An Astrix bean is a type that is
 * hooked into Astrix, for instance by defining an {@link AstrixApiProvider}. Astrix-beans
 * are typically intended to be reused in many different contexts (typically different applications). <p>
 *  
 * All instances created in the same AstrixContext will be cached and
 * not destroyed until the {@link #destroy()} method is invoked. Asking for
 * the same Astrix beans multiple times will return the same instance.
 * 
 * Astrix beans are created on demand the first time they are requested.
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixContext extends Astrix, AutoCloseable {

	/**
	 * Destroys this AstrixContext. All resources held by the
	 * AstrixContext will be released. 
	 */
	void destroy();

}