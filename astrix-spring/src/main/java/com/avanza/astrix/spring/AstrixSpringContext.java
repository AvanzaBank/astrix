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
package com.avanza.astrix.spring;

import org.springframework.context.ApplicationContext;

import com.avanza.astrix.beans.inject.AstrixInject;
import com.avanza.astrix.beans.inject.AstrixInjector;
/**
 * Acts as a bride between Astrix and Spring.
 * 
 * - Astrix managed classes can read spring-beans by accessing the ApplicationContext
 * - Spring managed classes might access Astrix by autowiring in this class
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixSpringContext {
	
	/* TODO: Depending on AstrixInjector feels weird... 
	 * It would be more natural to depend on AstrixContext but it causes circular dependency
	 * right now. When introducing "service-unit" as a concept then this class should depend on
	 * ServiceUnitContext instead of AstrixInjector.. 
	 */
	
	private AstrixInjector astrixInjector;
	private ApplicationContext applicationContext;

	@AstrixInject
	public void setAstrixInjector(AstrixInjector astrixInjector) {
		this.astrixInjector = astrixInjector;
	}
	
	public void setApplicationContext(ApplicationContext applicationContext) {
		this.applicationContext = applicationContext;
	}
	
	public ApplicationContext getApplicationContext() {
		return applicationContext;
	}
	
	public <T> T getInstance(
			Class<T> type) {
		return astrixInjector.getBean(type);
	}
	
}
