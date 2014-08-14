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
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import se.avanzabank.service.suite.provider.remoting.AstrixRemoteApiDescriptor;
import se.avanzabank.service.suite.provider.versioning.AstrixJsonApiMigration;
import se.avanzabank.service.suite.provider.versioning.AstrixObjectMapperConfigurer;
import se.avanzabank.service.suite.provider.versioning.AstrixVersioned;

public class AstrixRemoteServiceProviderFactory implements ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private AstrixRemoteApiDescriptor apiDescriptor;
	private AstrixVersioned versionInfo;
	
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
		Object remoteServiceDescriptor = remoteServiceDescriptors.iterator().next();
		this.apiDescriptor = remoteServiceDescriptor.getClass().getAnnotation(AstrixRemoteApiDescriptor.class);
		if (remoteServiceDescriptor.getClass().isAnnotationPresent(AstrixVersioned.class)) {
			this.versionInfo = remoteServiceDescriptor.getClass().getAnnotation(AstrixVersioned.class);
		}
	}
	
	public AstrixRemoteServiceProvider create() throws InstantiationException, IllegalAccessException {
		if (versionInfo != null) {
			
		}
		
//		Class<? extends AstrixJsonApiMigration>[] apiMigrationFactories = versionInfo.apiMigrations();
//		Class<? extends AstrixObjectMapperConfigurer> objectMapperConfigurerFactory = versionInfo.objectMapperConfigurer();
//		AstrixObjectMapperConfigurer astrixObjectMapperConfigurer = objectMapperConfigurerFactory.newInstance();
//		List<AstrixJsonApiMigration> apiMigrations = new ArrayList<>();
//		for (Class<? extends AstrixJsonApiMigration> apiMigrationFactory : apiMigrationFactories) {
//			apiMigrations.add(apiMigrationFactory.newInstance());
//		}
//		return new AstrixRemoteServiceProvider(apiMigrations, astrixObjectMapperConfigurer);
		return null;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

}
