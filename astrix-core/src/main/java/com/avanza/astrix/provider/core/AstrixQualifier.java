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
 * Annotations used to qualify an Astrix-bean. This annotation may be used to set a qualifier for
 * a bean definition in an ApiProvider, see {@link AstrixApiProvider}, or to inject qualified beans into a library. <p>
 * 
 * Astrix beans are identified by type and qualifier. When an Astrix bean
 * is defined without a qualifier it is consider "unqualified". Unqualified
 * beans can be retrieved from a AstrixContext using the unqualified getBean(Class&#60;T&#62;)
 * method, whereas qualified beans can be retrieved using the getBean(Class&#60;T&#62;, String)
 * method. <p> 
 * 
 * By qualifying Astrix beans one can hook more than one bean of 
 * a given type into Astrix. For instance one might export a MetricsService
 * interface from each server by using different qualifiers for each server. <p>
 * 
 * @author Elias Lindholm (elilin)
 */
@Target(value = { ElementType.PARAMETER, ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Documented
public @interface AstrixQualifier {
	/**
	 * The name of the qualifier for the target bean.
	 */
	String value();
}
