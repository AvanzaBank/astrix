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

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.context.AstrixContext;
/**
 * Spring {@link FactoryBean} that uses an AstrixContext as
 * factory to create a given Astrix bean.
 * 
 * The AstrixContext is typically created by registering an
 * {@link AstrixFrameworkBean} in the spring-application context,
 * which will create an {@link AstrixContext} in the current
 * spring application context.
 * 
 * @author Elias Lindholm (elilin)
 * @param <T>
 */
public final class AstrixFactoryBean<T> implements FactoryBean<T> {
	
	private final AstrixContext astrixContext;
	private Class<T> beanType;
	private String qualifier;
	
	@Autowired
	public AstrixFactoryBean(AstrixContext astrixContext) {
		this.astrixContext = astrixContext;
	}
	
	/**
	 * The type of the Astrix bean to create. <p>
	 * 
	 * @param beanType
	 */
	public void setBeanType(Class<T> beanType) {
		this.beanType = beanType;
	}
	
	/**
	 * Optional qualifier used to create an qualified Astrix bean.
	 * 
	 * @param qualifier
	 */
	public void setQualifier(String qualifier) {
		this.qualifier = qualifier;
	}

	@Override
	public T getObject() throws Exception {
		if (beanType == null) {
			throw new IllegalStateException("beanType property must be set");
		}
		if (qualifier != null) {
			return astrixContext.getBean(beanType, qualifier);
		}
		return astrixContext.getBean(beanType);
	}

	@Override
	public Class<?> getObjectType() {
		return this.beanType;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
