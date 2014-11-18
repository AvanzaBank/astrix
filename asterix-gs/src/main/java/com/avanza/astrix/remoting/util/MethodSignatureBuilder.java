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
package com.avanza.astrix.remoting.util;

import java.lang.reflect.Method;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class MethodSignatureBuilder {
	
	public static String build(Method method) {
		StringBuilder methodSignatureBuilder = new StringBuilder(); 
		methodSignatureBuilder.append(method.getName());
		methodSignatureBuilder.append("(");
		boolean first = true;
		for (Class<?> param : method.getParameterTypes()) {
			if (first) {
				first = false;
			} else {
				methodSignatureBuilder.append(", ");
			}
			methodSignatureBuilder.append(param.getName());
		}
		methodSignatureBuilder.append("(");
		return methodSignatureBuilder.toString();
	}

}
