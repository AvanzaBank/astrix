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
package se.avanzabank.service.suite.context;

import org.springframework.beans.factory.FactoryBean;

public class FactoryBeanAdapter<T> implements FactoryBean<T> {
	
	// TODO: delete me? This class was introduced by the idea that the service-framework registers a set of factory-beans, one for each consumed service.
	
	private final AstrixServiceFactory<T> serviceFactory;
	private final AstrixContext context;
	
	public FactoryBeanAdapter(AstrixServiceFactory<T> serviceFactory, AstrixContext context) {
		this.serviceFactory = serviceFactory;
		this.context = context;
	}

	@Override
	public T getObject() throws Exception {
		return serviceFactory.create(context);
	}

	@Override
	public Class<?> getObjectType() {
		return serviceFactory.getServiceType();
	}

	@Override
	public boolean isSingleton() {
		return true;
	}

}
