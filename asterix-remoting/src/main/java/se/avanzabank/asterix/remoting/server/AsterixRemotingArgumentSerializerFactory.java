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
package se.avanzabank.asterix.remoting.server;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import se.avanzabank.asterix.context.AsterixApiDescriptor;
import se.avanzabank.asterix.context.AsterixPlugins;
import se.avanzabank.asterix.context.AsterixVersioningPlugin;
import se.avanzabank.asterix.core.AsterixObjectSerializer;

public class AsterixRemotingArgumentSerializerFactory {
	
//	private Class<?> apiDescriptorHolder;
//	private ApplicationContext applicationContext;
	private AsterixPlugins plugins;
	private AsterixApiDescriptor apiDescriptor;

	@Autowired
	public AsterixRemotingArgumentSerializerFactory(AsterixPlugins plugins, AsterixApiDescriptor apiDescriptor) { // plugin dependency
		this.plugins = plugins;
		this.apiDescriptor = apiDescriptor;
	}

	public AsterixObjectSerializer create() {
		AsterixVersioningPlugin versioningPlugin = plugins.getPlugin(AsterixVersioningPlugin.class);
//		Class<? extends Object> descriptorClass = apiDescriptorHolder;
		return versioningPlugin.create(apiDescriptor);
	}
	
//	@PostConstruct
//	public void readDescriptor() {
//		Collection<Object> remoteServiceDescriptors = applicationContext.getBeansWithAnnotation(AsterixRemoteApiDescriptor.class).values();
//		if (remoteServiceDescriptors.size() != 1) {
//			List<String> remoteServiceDescriptorTypes = new ArrayList<>();
//			for (Object rsd : remoteServiceDescriptors) {
//				remoteServiceDescriptorTypes.add(rsd.getClass().getName());
//			}
//			throw new IllegalStateException("Exactly one bean annotated with @AsterixRemoteApiDescriptor should exists in application context. found: " + remoteServiceDescriptorTypes);
//		}
//		this.apiDescriptorHolder = remoteServiceDescriptors.iterator().next().getClass();
//	}

//	@Override
//	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
//		this.applicationContext = applicationContext;
//	}
	
	

}
