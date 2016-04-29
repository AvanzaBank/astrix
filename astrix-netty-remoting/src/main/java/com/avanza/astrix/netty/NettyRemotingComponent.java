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
package com.avanza.astrix.netty;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.service.BoundServiceBeanInstance;
import com.avanza.astrix.beans.service.ServiceComponent;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceProviderInstanceProperties;
import com.avanza.astrix.beans.service.SimpleBoundServiceBeanInstance;
import com.avanza.astrix.core.remoting.RoutingStrategy;
import com.avanza.astrix.netty.client.NettyRemotingClient;
import com.avanza.astrix.netty.server.NettyRemotingServer;
import com.avanza.astrix.remoting.client.RemotingProxyFactory;
import com.avanza.astrix.remoting.server.AstrixServiceActivator;
import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.ObjectSerializerFactory;

/**
 * This component is in a (very) experimental state.
 * 
 * @author Elias Lindholm
 *
 */
public class NettyRemotingComponent implements ServiceComponent {
	
	private Logger log = LoggerFactory.getLogger(NettyRemotingComponent.class);

	private static final String NETTY_PORT = "astrix.netty.port";
	private static final String NETTY_HOST = "astrix.netty.host";
	public static final String NAME = "netty-remoting";
	
	private final RemotingProxyFactory remotingProxyFactory;
	private final AstrixServiceActivator serviceActivator;
	private final ObjectSerializerFactory objectSerializerFactory;
	private final NettyRemotingServer remotingServer;
	

	public NettyRemotingComponent(RemotingProxyFactory remotingProxyFactory, AstrixServiceActivator serviceActivator,
			ObjectSerializerFactory objectSerializerFactory, NettyRemotingServer remotingServer) {
		this.remotingProxyFactory = remotingProxyFactory;
		this.serviceActivator = serviceActivator;
		this.objectSerializerFactory = objectSerializerFactory;
		this.remotingServer = remotingServer;
	}

	@Override
	public <T> BoundServiceBeanInstance<T> bind(ServiceDefinition<T> serviceDefinition, ServiceProviderInstanceProperties serviceProperties) {
		String host = serviceProperties.getProperty(NETTY_HOST);
		int port = Integer.valueOf(serviceProperties.getProperty(NETTY_PORT));
		log.info("Connecting to: {}:{}", host, port);
		NettyRemotingClient remotingClient = new NettyRemotingClient();
		remotingClient.connect(host, port);
		NettyRemotingTransport nettyRemotingTransport = new NettyRemotingTransport(remotingClient);
		T serviceProxy = remotingProxyFactory.create(serviceDefinition, serviceProperties, nettyRemotingTransport, new RoutingStrategy.RoundRobin());
		return new SimpleBoundServiceBeanInstance<T>(serviceProxy); // TODO: proper lifecycle-management of remotingClient instance
	}

	@Override
	public ServiceProviderInstanceProperties parseServiceProviderUri(String serviceProviderUri) {
		String[] hostAndPort = serviceProviderUri.split(":");
		ServiceProviderInstanceProperties result = new ServiceProviderInstanceProperties();
		result.getProperties().put(NETTY_HOST, hostAndPort[0]);
		result.getProperties().put(NETTY_PORT, hostAndPort[1]);
		return result;
	}

	@Override
	public <T> ServiceProviderInstanceProperties createServiceProperties(ServiceDefinition<T> exportedServiceDefinition) {
		ServiceProviderInstanceProperties properties = new ServiceProviderInstanceProperties();
		properties.getProperties().put(NETTY_HOST, getHostName()); // TODO
		properties.getProperties().put(NETTY_PORT, Integer.toString(remotingServer.getPort()));
		return properties;
	}

	private String getHostName() {
		try {
			InetAddress localHost = java.net.InetAddress.getLocalHost();
			return localHost.getHostName();
		} catch (UnknownHostException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public String getName() {
		return NAME;
	}

	@Override
	public boolean canBindType(Class<?> type) {
		return true;
	}

	@Override
	public <T> void exportService(Class<T> providedApi, T provider, ServiceDefinition<T> serviceDefinition) {
		// TODO: this responsibility does not belong in service component. Introduce RemotingServer abstraction
		AstrixObjectSerializer objectSerializer = objectSerializerFactory.create(serviceDefinition.getObjectSerializerDefinition()); 
		this.serviceActivator.register(provider, objectSerializer, providedApi);
		this.remotingServer.verifyStarted();
	}

	@Override
	public boolean requiresProviderInstance() {
		return true;
	}

}
