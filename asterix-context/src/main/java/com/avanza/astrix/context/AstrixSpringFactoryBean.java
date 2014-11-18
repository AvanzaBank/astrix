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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;
/**
 * Bridges between spring and Astrix to make Astrix-beans available in
 * spring-application context.   
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixSpringFactoryBean<T> implements FactoryBean<T> {
	
	private Class<T> type;
	private AstrixContext Astrix;

	@Autowired
	public void setAstrixContext(AstrixContext Astrix) {
		this.Astrix = Astrix;
	}
	
	public AstrixContext getAstrixContext() {
		return Astrix;
	}
	
	public void setType(Class<T> type) {
		this.type = type;
	}
	
	public Class<T> getType() {
		return type;
	}

	@Override
	public T getObject() throws Exception {
		return Astrix.getBean(type);
	}

	@Override
	public Class<?> getObjectType() {
		return type;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
