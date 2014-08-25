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
package se.avanzabank.service.suite.remoting.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import se.avanzabank.service.suite.context.AstrixPlugins;
import se.avanzabank.service.suite.context.AstrixVersioningPlugin;
import se.avanzabank.service.suite.core.AstrixObjectSerializer;
import se.avanzabank.service.suite.provider.remoting.AstrixRemoteApiDescriptor;

public class AstrixRemotingArgumentSerializerFactory implements ApplicationContextAware {
	
	private Class<?> apiDescriptorHolder;
	private ApplicationContext applicationContext;
	private AstrixPlugins plugins;

	@Autowired
	public AstrixRemotingArgumentSerializerFactory(AstrixPlugins plugins) {
		this.plugins = plugins;
	}

	public AstrixObjectSerializer create() {
		AstrixVersioningPlugin versioningPlugin = plugins.getPlugin(AstrixVersioningPlugin.class);
		Class<? extends Object> descriptorClass = apiDescriptorHolder;
		return versioningPlugin.create(descriptorClass);
	}
	
	@PostConstruct
	public void readDescriptor() {
		Collection<Object> remoteServiceDescriptors = applicationContext.getBeansWithAnnotation(AstrixRemoteApiDescriptor.class).values();
		if (remoteServiceDescriptors.size() != 1) {
			List<String> remoteServiceDescriptorTypes = new ArrayList<>();
			for (Object rsd : remoteServiceDescriptors) {
				remoteServiceDescriptorTypes.add(rsd.getClass().getName());
			}
			throw new IllegalStateException("Exactly one bean annotated with @AstrixRemoteApiDescriptor should exists in application context. found: " + remoteServiceDescriptorTypes);
		}
		this.apiDescriptorHolder = remoteServiceDescriptors.iterator().next().getClass();
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	

}
