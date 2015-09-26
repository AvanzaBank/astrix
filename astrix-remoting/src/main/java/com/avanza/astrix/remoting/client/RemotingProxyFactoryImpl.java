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
package com.avanza.astrix.remoting.client;

import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProperties;
import com.avanza.astrix.context.core.ReactiveTypeConverter;
import com.avanza.astrix.core.remoting.RoutingStrategy;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;

public class RemotingProxyFactoryImpl implements RemotingProxyFactory {
	
	private final ObjectSerializerFactory objectSerializerFactory;
	private final ReactiveTypeConverter reactiveTypeConverter;
	
	public RemotingProxyFactoryImpl(ObjectSerializerFactory objectSerializerFactory, ReactiveTypeConverter reactiveTypeConverter) {
		this.objectSerializerFactory = objectSerializerFactory;
		this.reactiveTypeConverter = reactiveTypeConverter;
	}

	@Override
	public <T> T create(ServiceDefinition<T> serviceDefinition, ServiceProperties serviceProperties,
			RemotingTransportSpi remotingTransportSpi, RoutingStrategy routingStrategy) {
		AstrixObjectSerializer objectSerializer = objectSerializerFactory.create(serviceDefinition.getObjectSerializerDefinition());
		RemotingTransport remotingTransport = RemotingTransport.create(remotingTransportSpi);
		return RemotingProxy.create(serviceDefinition.getServiceType(), ReflectionUtil.classForName(serviceProperties.getProperty(ServiceProperties.API))
				, remotingTransport, objectSerializer, routingStrategy, reactiveTypeConverter);
	}

}
