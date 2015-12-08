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
package com.avanza.astrix.beans.service;

import static java.util.stream.Collectors.toList;

import java.util.List;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.BeanProxy;

final class ServiceBeanProxies {
	
	private final List<ServiceBeanProxyFactory> proxyFactories;
	
	public ServiceBeanProxies(List<ServiceBeanProxyFactory> proxyFactories) {
		this.proxyFactories = proxyFactories.stream()
											.sorted((proxy1, proxy2) -> Long.compare(proxy1.order(), proxy2.order()))
											.collect(toList());
	}

	public List<BeanProxy> create(AstrixBeanKey<?> beanKey) {
		return proxyFactories.stream()
							 .map(factory -> factory.create(beanKey))
							 .collect(toList());
	}

}
