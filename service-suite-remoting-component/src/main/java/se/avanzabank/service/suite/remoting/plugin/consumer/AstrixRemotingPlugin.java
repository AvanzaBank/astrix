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
package se.avanzabank.service.suite.remoting.plugin.consumer;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

import org.kohsuke.MetaInfServices;

import se.avanzabank.service.suite.context.AstrixServiceFactory;
import se.avanzabank.service.suite.context.AstrixServiceProviderPlugin;
import se.avanzabank.service.suite.provider.core.AstrixServiceBusApi;
import se.avanzabank.service.suite.provider.remoting.AstrixRemoteApiDescriptor;

@MetaInfServices(AstrixServiceProviderPlugin.class)
public class AstrixRemotingPlugin implements AstrixServiceProviderPlugin {
	
	@Override
	public List<AstrixServiceFactory<?>> create(Class<?> descriptorHolder) {
		AstrixRemoteApiDescriptor remoteApiDescriptor = descriptorHolder.getAnnotation(AstrixRemoteApiDescriptor.class);
		final String targetSpace = remoteApiDescriptor.targetSpaceName();
		if (targetSpace.isEmpty()) {
			throw new IllegalArgumentException("No space name found on: " + descriptorHolder);
		}
		Class<?>[] exportedApis = remoteApiDescriptor.exportedApis();
		List<AstrixServiceFactory<?>> result = new ArrayList<>();
		for (Class<?> api : exportedApis) {
			result.add(
					new AstrixRemotingServiceFactory<>(api, targetSpace, descriptorHolder));
		}
		return result;
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixRemoteApiDescriptor.class;
	}

	@Override
	public boolean consumes(Class<?> descriptorHolder) {
		return descriptorHolder.isAnnotationPresent(getProviderAnnotationType()) 
				&& !descriptorHolder.isAnnotationPresent(AstrixServiceBusApi.class);
	}
	
}
