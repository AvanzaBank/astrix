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
package com.avanza.astrix.context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.factory.BeanFactory;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.BeanPublisher;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDefinitionSource;
import com.avanza.astrix.beans.service.StatefulAstrixBean;
import com.avanza.astrix.modules.Modules;
import com.avanza.astrix.serviceunit.AstrixApplicationDescriptor;
import com.avanza.astrix.serviceunit.ExportedServiceBeanDefinition;
import com.avanza.astrix.serviceunit.ServiceAdministrator;
import com.avanza.astrix.serviceunit.ServiceAdministratorVersioningConfigurer;
import com.avanza.astrix.serviceunit.ServiceExporter;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
/**
 * An AstrixContextImpl is the runtime-environment for the astrix-framework. It is used
 * both by consuming applications as well as server applications. AstrixContextImpl providers access
 * to different Astrix-plugins at runtime and is used as a factory to create Astrix-beans.
 * 
 * @author Elias Lindholm (elilin)
 */
final class AstrixContextImpl implements Astrix, AstrixApplicationContext {
	
	private final Logger log = LoggerFactory.getLogger(AstrixContextImpl.class);
	private final BeanFactory beanFactory;
	private final BeanPublisher beanPublisher;
	private final Modules modules;
	private final AstrixApplicationDescriptor applicationDescriptor;
	private final AstrixConfig config;
	
	public AstrixContextImpl(Modules modules, AstrixApplicationDescriptor applicationDescriptor) {
		this.modules = modules;
		this.applicationDescriptor = applicationDescriptor;
		this.config = modules.getInstance(AstrixConfig.class);
		this.beanPublisher = modules.getInstance(BeanPublisher.class);
		this.beanFactory = modules.getInstance(BeanFactory.class);
	}
	

	void register(ApiProviderClass apiProvider) {
		this.beanPublisher.publish(apiProvider);		
	}

	<T> void registerBeanFactory(StandardFactoryBean<T> beanFactory) {
		this.beanFactory.registerFactory(beanFactory);
	}
	
	@Override
	public void destroy() {
		this.modules.destroy();
	}

	@Override
	public void close() throws Exception {
		destroy();
	}
	
	public BeanConfiguration getBeanConfiguration(AstrixBeanKey<?> beanKey) {
		return this.config.getBeanConfiguration(beanKey);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType) {
		return getBean(beanType, null);
	}
	
	@Override
	public <T> T getBean(Class<T> beanType, String qualifier) {
		return beanFactory.getBean(AstrixBeanKey.create(beanType, qualifier));
	}
	
	public <T> T getBean(AstrixBeanKey<T> beanKey) {
		return beanFactory.getBean(beanKey);
	}
	

	@Override
	public <T> T waitForBean(Class<T> beanType, long timeoutMillis) throws InterruptedException {
		return waitForBean(beanType, null, timeoutMillis);
	}
	
	@Override
	public <T> T waitForBean(Class<T> beanType, String qualifier, long timeoutMillis) throws InterruptedException {
		AstrixBeanKey<T> beanKey = AstrixBeanKey.create(beanType, qualifier);
		T bean = beanFactory.getBean(beanKey);
		for (AstrixBeanKey<?> dependencyKey : beanFactory.getDependencies(beanKey)) {
			Object dependency = beanFactory.getBean(dependencyKey);
			waitForBeanToBeBound(dependency, timeoutMillis);
		}
		waitForBeanToBeBound(bean, timeoutMillis);
		return bean;
	}
	private void waitForBeanToBeBound(Object bean, long timeoutMillis) throws InterruptedException {
		if (bean instanceof StatefulAstrixBean) {
			StatefulAstrixBean.class.cast(bean).waitUntilBound(timeoutMillis);
		}
	}

	/**
	 * Returns an instance of an internal framework class.
	 * @param classType
	 * @return
	 */
	public final <T> T getInstance(final Class<T> classType) {
		return this.modules.getInstance(classType);
	}
	
	@Override
	public void startServicePublisher() {
		if (!isServer()) {
			throw new IllegalStateException("Server part not configured. Set AstrixConfigurer.setApplicationDescriptor to load server part of framework");
		}
		ServiceExporter serviceExporter = getInstance(ServiceExporter.class);
		
		if (config.get(AstrixSettings.SERVICE_ADMINISTRATOR_EXPORTED).get()) {
			exportServiceAdministrator(serviceExporter);
		} else {
			log.info("Export of ServiceAdministrator service explicitly disabled. Won't export ServiceAdministrator service.");
		}
		
		serviceExporter.startPublishServices();
	}


	private void exportServiceAdministrator(ServiceExporter serviceExporter) {
		String applicationInstanceId = config.get(AstrixSettings.APPLICATION_INSTANCE_ID).get();
		serviceExporter.addServiceProvider(getInstance(ServiceAdministrator.class));
		ObjectSerializerDefinition serializer = ObjectSerializerDefinition.versionedService(1, ServiceAdministratorVersioningConfigurer.class);
		AstrixBeanKey<ServiceAdministrator> serviceAdministratorQualifier = AstrixBeanKey.create(ServiceAdministrator.class, applicationInstanceId);
		ServiceDefinition<ServiceAdministrator> serviceDefinition = new ServiceDefinition<>(ServiceDefinitionSource.create("FrameworkServices"),
				serviceAdministratorQualifier, 
				serializer, 
				true); // isDynamicQualified
		String serviceAdministratorComponent = config.get(AstrixSettings.SERVICE_ADMINISTRATOR_COMPONENT).get();
		ExportedServiceBeanDefinition<ServiceAdministrator> serviceAdminDefintion = new ExportedServiceBeanDefinition<>(serviceAdministratorQualifier, 
				serviceDefinition, 
				true, // isVersioned  
				true, // alwaysActive
				serviceAdministratorComponent);
		serviceExporter.exportService(serviceAdminDefintion);
		log.info("Exported ServiceAdministrator service. component={} qualifier={}", serviceAdministratorComponent, serviceAdministratorQualifier);
	}


	private boolean isServer() {
		return this.applicationDescriptor != null;
	}
	
}
