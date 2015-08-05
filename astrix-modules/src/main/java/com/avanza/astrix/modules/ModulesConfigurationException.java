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
public class ModulesConfigurationException extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private final LinkedList<String> dependencyTrace = new LinkedList<String>();
	private final String msg;
	
	public ModulesConfigurationException() {
		msg = null;
	}
	
	public ModulesConfigurationException(String msg) {
		this.msg = msg;
	}

	public void addToDependencyTrace(Class<?> type, String moduleName) {
		dependencyTrace.addFirst(type.getName() + " [" + moduleName + "]");
	}
	
	@Override
	public String getMessage() {
		StringBuilder result = new StringBuilder();
		if (msg != null) {
			result.append(msg).append("\n");
		}
		boolean prependDependencyArrow = false;
		for (String dep : dependencyTrace) {
			if (prependDependencyArrow) {
				result.append("\n --> ");
			} else {
				prependDependencyArrow = true;
				
			}
			result.append(dep);
		}
		return result.toString();
	}

}
