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
package se.avanzabank.service.suite.remoting.plugin;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.service.suite.context.Astrix;
import se.avanzabank.service.suite.context.AstrixContext;
import se.avanzabank.service.suite.context.AstrixFaultTolerancePlugin;
import se.avanzabank.service.suite.context.AstrixObjectSerializer;
import se.avanzabank.service.suite.context.AstrixVersioningPlugin;
import se.avanzabank.service.suite.context.AstrixServiceFactory;
import se.avanzabank.service.suite.context.AstrixServiceProvider;
import se.avanzabank.service.suite.context.AstrixServiceProviderPlugin;
import se.avanzabank.service.suite.provider.remoting.AstrixRemoteApiDescriptor;
import se.avanzabank.service.suite.remoting.client.AstrixRemotingTransport;
import se.avanzabank.space.SpaceLocator;

@MetaInfServices(AstrixServiceProviderPlugin.class)
public class AstrixRemotingPlugin implements AstrixServiceProviderPlugin {

	private AstrixContext context;

	@Override
	public AstrixServiceProvider create(Class<?> descriptorHolder) {
		AstrixRemoteApiDescriptor remoteApiDescriptor = descriptorHolder.getAnnotation(AstrixRemoteApiDescriptor.class);
		String targetSpace = remoteApiDescriptor.targetSpaceName();
		Class<?>[] exportedApis = remoteApiDescriptor.exportedApis();
		List<AstrixServiceFactory<?>> serviceFactories = new ArrayList<>();
		AstrixObjectSerializer objectSerializer = context.getProvider(AstrixVersioningPlugin.class).create(descriptorHolder);
		final SpaceLocator spaceLocator = context.getProvider(SpaceLocator.class);
		AstrixFaultTolerancePlugin faultTolerance = context.getProvider(AstrixFaultTolerancePlugin.class);
		AstrixRemotingTransportFactory remotingTransportFactory = new AstrixRemotingTransportFactory() {
			@Override
			public AstrixRemotingTransport createRemotingTransport(String targetSpaceName) {
				return AstrixRemotingTransport.remoteSpace(spaceLocator.createClusteredProxy(targetSpaceName)); // TODO: caching of created proxies, fault tolerance?
			}
		};
		for (Class<?> api : exportedApis) {
			serviceFactories.add(
					new AstrixRemotingServiceFactory<>(api, remotingTransportFactory, targetSpace, objectSerializer, faultTolerance));
		}
		return new AstrixServiceProvider(serviceFactories, descriptorHolder);
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixRemoteApiDescriptor.class;
	}

	@Override
	public void setAstrix(Astrix astrix) {
	}

	@Override
	public void setPlugins(AstrixContext plugins) {
		this.context = plugins;
	}
	
}
