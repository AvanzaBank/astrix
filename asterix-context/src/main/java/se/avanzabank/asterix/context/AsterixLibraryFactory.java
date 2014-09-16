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
package se.avanzabank.asterix.context;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AsterixLibraryFactory<T> implements AsterixFactoryBean<T>, AsterixBeanAware {

	private Object factoryInstance;
	private Method factoryMethod;
	private Class<T> type;
	private List<Class<?>> asterixBeanDependencies = new ArrayList<>();
	private AsterixBeans beans;
	
	@SuppressWarnings("unchecked")
	public AsterixLibraryFactory(Object factoryInstance, Method factoryMethod) {
		this.factoryInstance = factoryInstance;
		this.factoryMethod = factoryMethod;
		this.type = (Class<T>) factoryMethod.getReturnType();
		this.asterixBeanDependencies = Arrays.asList(factoryMethod.getParameterTypes());
	}

	@Override
	public T create(String qualifier) {
		Object[] args = new Object[factoryMethod.getParameterTypes().length];
		// TODO: analyze each factory for what dependencies they have?
		for (int argumentIndex = 0; argumentIndex < factoryMethod.getParameterTypes().length; argumentIndex++) {
			Class<?> argumentType = factoryMethod.getParameterTypes()[argumentIndex];
			args[argumentIndex] = beans.getBean(argumentType); // TODO: discover circular library creation
		}
		Object result;
		try {
			result = factoryMethod.invoke(factoryInstance, args);
		} catch (Exception e) {
			throw new RuntimeException("Failed to instantiate library using: " + factoryMethod.toString() + " on " + type.getName(), e);
		}
		return type.cast(result);
	}
	
	@Override
	public Class<T> getBeanType() {
		return this.type;
	}
	
	@Override
	public List<Class<?>> getBeanDependencies() {
		return this.asterixBeanDependencies;
	}

	@Override
	public void setAsterixBeans(AsterixBeans beans) {
		this.beans = beans;
	}
	
}
