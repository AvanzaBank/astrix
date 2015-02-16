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
package com.avanza.astrix.service.registry.client;

import java.util.Objects;

import org.openspaces.remoting.Routing;

import com.avanza.astrix.beans.factory.AstrixBeanKey;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.service.registry.server.AstrixServiceRegistryEntry;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceRegistryClientImpl implements AstrixServiceRegistryClient {
	
	private final AstrixServiceRegistry serviceRegistry;
	private final String subsystem;

	public AstrixServiceRegistryClientImpl(AstrixServiceRegistry serviceRegistry, String subsystem) {
		this.serviceRegistry = Objects.requireNonNull(serviceRegistry);
		this.subsystem = Objects.requireNonNull(subsystem);
	}

	@Override
	public <T> AstrixServiceProperties lookup(@Routing Class<T> type) {
		return lookup(AstrixBeanKey.create(type, null));
	}

	@Override
	public <T> AstrixServiceProperties lookup(AstrixBeanKey<T> beanKey) {
		AstrixServiceRegistryEntry entry = serviceRegistry.lookup(beanKey.getBeanType().getName(), beanKey.getQualifier());
		if (entry == null) {
			return null;
		}
		return new AstrixServiceProperties(entry.getServiceProperties());
	}

	@Override
	public <T> void register(@Routing Class<T> type, AstrixServiceProperties properties, long lease) {
		properties.setProperty(AstrixServiceProperties.SUBSYSTEM, this.subsystem);
		AstrixServiceRegistryEntry entry = new AstrixServiceRegistryEntry();
		entry.setServiceProperties(properties.getProperties());
		entry.setServiceBeanType(type.getName());
		this.serviceRegistry.register(entry, lease);
	}

}
