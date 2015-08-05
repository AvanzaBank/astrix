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
package com.avanza.astrix.modules;

import java.util.LinkedList;



/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class CircularDependency extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final LinkedList<String> dependencyTrace = new LinkedList<String>();
	
	public void addToDependencyTrace(Class<?> type, String moduleName) {
		dependencyTrace.addFirst(type.getName() + " [" + moduleName + "]");
	}
	
	@Override
	public String getMessage() {
		StringBuilder result = new StringBuilder();
		for (String dep : dependencyTrace) {
			if (result.length() > 0) {
				result.append("\n --> ");
			} else {
				result.append("\n     ");
			}
			result.append(dep);
		}
		return result.toString();
	}

}
