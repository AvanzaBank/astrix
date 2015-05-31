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
package com.avanza.astrix.context;

import com.avanza.astrix.provider.core.AstrixApiProvider;

/**
 * An AstrixContext is a factory to create instances of Astrix beans. An Astrix
 * bean is a type in an API that is published using Astrix, most often by
 * defining an {@link AstrixApiProvider}. <p>  
 * 
 * A typical application only creates one AstrixContext, either by using an 
 * {@link AstrixConfigurer} or by registering an AstrixFrameworkBean 
 * in a spring application. <p>
 *  
 * All instances created by the same AstrixContext will be cached and
 * not destroyed until the {@link #destroy()} method is invoked. Asking for
 * the same Astrix beans multiple times will return the same instance. <p>
 * 
 * Astrix beans are created on demand the first time they are requested. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface AstrixContext extends Astrix, AutoCloseable {

	/**
	 * Destroys this AstrixContext.<p>
	 * 
	 * All resources held by the AstrixContext will be released. After
	 * destroying an AstrixContext all beans that are created
	 * by this AstrixContext should be considered destroyed as well. 
	 * The behavior when invoking such beans after destroy are undefined.
	 */
	void destroy();

}