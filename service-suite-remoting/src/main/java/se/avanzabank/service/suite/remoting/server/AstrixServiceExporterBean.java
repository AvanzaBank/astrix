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

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

import se.avanzabank.service.suite.provider.remoting.AstrixRemoteServiceExport;
/**
 * This bean makes all beans annotated with AstrixRemoteServiceExport invokable using the remoting 
 * framework. 
 * 
 * Note that this bean does not publish any services onto the service bus.  
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceExporterBean implements ApplicationContextAware {
	
	private ApplicationContext applicationContext;
	private AstrixServiceActivator serviceActivator;

	@Autowired
	public AstrixServiceExporterBean(AstrixServiceActivator serviceActivator) {
		this.serviceActivator = serviceActivator;
	}

	@PostConstruct
	public void register() {
		for (Object service : applicationContext.getBeansWithAnnotation(AstrixRemoteServiceExport.class).values()) {
			AstrixRemoteServiceExport remoteServiceExport = service.getClass().getAnnotation(AstrixRemoteServiceExport.class);
			for (Class<?> providedApi : remoteServiceExport.value()) {
				if (!providedApi.isAssignableFrom(service.getClass())) {
					throw new IllegalArgumentException("Cannot export: " + service.getClass() + " as " + providedApi);
				}
			}
			register(service, remoteServiceExport.value());
		}
	}
	
	private void register(Object provider, Class<?>... providedApis) {
		this.serviceActivator.register(provider, providedApis);
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext)
			throws BeansException {
		this.applicationContext = applicationContext;
	}

}
