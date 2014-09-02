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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AstrixBeanFactoryRegistry {
	
	private final ConcurrentMap<Class<?>, AstrixApiProvider> apiProviderByBeanType = new ConcurrentHashMap<>();
	private final Logger log = LoggerFactory.getLogger(getClass());

	public <T> AstrixFactoryBean<T> getFactoryBean(Class<T> beanType) {
		AstrixApiProvider apiProvider = getApiProvider(beanType);
		return apiProvider.getFactory(beanType);
	}
	
	public <T> AstrixApiProvider getApiProvider(Class<T> beanType) {
		AstrixApiProvider apiProvider = this.apiProviderByBeanType.get(beanType);
		if (apiProvider == null) {
			throw new IllegalStateException(String.format("No providers found for beanType=%s",
			 		  beanType.getName())); 
		}
		return apiProvider;
	}

	public void registerProvider(AstrixApiProvider apiProvider) {
		for (Class<?> providedApi : apiProvider.providedApis()) {
			AstrixApiProvider duplicateProvider = apiProviderByBeanType.putIfAbsent(providedApi, apiProvider);
			log.debug("Registering provider. api={} provider={}", providedApi.getName(), apiProvider.getDescriptorHolder().getName());
			if (duplicateProvider != null) {
				throw new IllegalStateException(String.format("Multiple providers discovered for api=%s. %s and %s",
													 		  providedApi.getClass().getName(), 
													 		  apiProvider.getClass().getName(), 
													 		  duplicateProvider.getClass().getName()));
			}
		}
	}

}
