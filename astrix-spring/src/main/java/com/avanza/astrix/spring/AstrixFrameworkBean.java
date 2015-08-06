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
package com.avanza.astrix.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ApplicationContextEvent;
import org.springframework.context.event.ContextClosedEvent;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.ContextStoppedEvent;
import org.springframework.core.Ordered;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.publish.ApiProvider;
import com.avanza.astrix.beans.service.ObjectSerializerDefinition;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.context.Astrix;
import com.avanza.astrix.context.AstrixApplicationContext;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.serviceunit.AstrixApplicationDescriptor;
import com.avanza.astrix.serviceunit.ExportedServiceBeanDefinition;
import com.avanza.astrix.serviceunit.ServiceAdministrator;
import com.avanza.astrix.serviceunit.ServiceAdministratorVersioningConfigurer;
import com.avanza.astrix.serviceunit.ServiceExporter;

/**
 * 
 * @author Elias Lindholm (elilin)
 */
public class AstrixFrameworkBean implements BeanFactoryPostProcessor, ApplicationContextAware, ApplicationListener<ApplicationContextEvent>, Ordered {
	
	/*
	 * A NOTE ON THE IMPLEMENTATION NOTE: This comment is outdated...
	 * 
	 * IMPLEMENTATION NOTE - Astrix startup
	 * 
	 * The startup procedure goes as follows:
	 * 
	 * 1. BeanFactoryPostProcessor (BFPP)
	 *  - The BFPP will register an instance for each consumedAstrixBean in the BeanFactory
	 *  - The BFPP will also register an instance of AstrixSpingContext to act as bridge
	 *    between spring and astrix.
	 *    
	 * 2. BeanPostProcessor (BPP)
	 *  - The BPP will investigate each spring-bean in the current application and search
	 *    for @AstrixServiceExport annotated classes and register those with the ServiceRegistryExporterClient
	 *    
	 * 3. ApplicationListener<ContexRefreshedEvent>
	 *  - After each spring-bean have bean created and fully initialized this class will receive a call-back
	 *    and start exporting services using the ServiceRegistryExporterClient.
	 *    
	 * 
	 * Note that this class (AstrixFrameworkBean) is the only class in the framework that will recieve spring-lifecylce events.
	 * 
	 */
	
	private static final Logger log = LoggerFactory.getLogger(AstrixFrameworkBean.class);
	private List<Class<?>> consumedAstrixBeans = new ArrayList<>();
	private String subsystem;
	private Map<String, String> settings = new HashMap<>();
	private AstrixApplicationDescriptor applicationDescriptor;
	private AstrixApplicationContext astrixContext;
	private volatile boolean serviceExporterStarted = false;
	private ApplicationContext applicationContext;
	private final AstrixConfigurer configurer = new AstrixConfigurer();
	
	public AstrixFrameworkBean() {
	}
	
	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		astrixContext = createAsterixContext(getDynamicConfig(applicationContext));
		if (isServer()) {
			setupApplicationInstanceId();
		}
		astrixContext.getInstance(AstrixSpringContext.class).setApplicationContext(applicationContext);
		astrixContext.getInstance(AstrixSpringContext.class).setAstrixContext(astrixContext);
		for (Class<?> consumedAstrixBean : this.consumedAstrixBeans) {
			beanFactory.registerSingleton(consumedAstrixBean.getName(), astrixContext.getBean(consumedAstrixBean));
		}
		beanFactory.registerSingleton(AstrixSpringContext.class.getName(), astrixContext.getInstance(AstrixSpringContext.class));
		beanFactory.registerSingleton(AstrixContext.class.getName(), astrixContext);
		beanFactory.addBeanPostProcessor(astrixContext.getInstance(BeanPostProcessor.class));
	}

	/**
	 * All consumedAstrixBeans will be created (see {@link Astrix#getBean(Class)} and registered in the spring ApplicationContext at
	 * startup. All consumedAstrixBeans will be available as autowiring candidates for other beans in the current spring ApplicationContext.<p>
	 * 
	 * Its also possible to wire consumedAstrixBeans by reference. Each consumed Astrix-bean will be registered under the fully qualified
	 * class name of the given Astrix bean.<p>
	 * 
	 * <pre>
	 * Example:
	 * 
	 * setConsumedAstrixBeans(asList(se.avanza.customer.CustomerService.class));
	 * 
	 * Creates and registers an Astrix bean of type CustomerService in the current ApplicationContext. The name
	 * of the CustomerService bean in the spring ApplicationContext will be "se.avanza.customer.CustomerService", which
	 * can be used in a ApplicationContext-xml file to explicitly wire an instance of CustomerService into another
	 * spring bean.
	 * </pre>
	 * 
	 * @param consumedAstrixBeans
	 */
	public void setConsumedAstrixBeans(List<Class<? extends Object>> consumedAstrixBeans) {
		this.consumedAstrixBeans = consumedAstrixBeans;
	}

	public void setSettings(Map<String, String> settings) {
		this.settings.putAll(settings);
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}
	
	@PreDestroy
	public void destroy() {
		this.astrixContext.destroy();
	}
	
	/**
	 * If a service descriptor is provided, then the service exporting part of the framework
	 * will be loaded with all required components for the given serviceDescriptor.
	 * 
	 * @param serviceDescriptor
	 * @deprecated - replaced by {@link AstrixFrameworkBean#setApplicationDescriptor(Class)}
	 */
	@Deprecated
	public void setServiceDescriptor(Class<?> serviceDescriptorHolder) {
		this.applicationDescriptor = AstrixApplicationDescriptor.create(serviceDescriptorHolder);
	}
	
	/**
	 * If an application descriptor is provided, then the service exporting part of the framework
	 * will be loaded with all required components to provide the services defined in
	 * the api's provided by the given applicatinDescriptor.
	 * 
	 * @param serviceDescriptor
	 */
	public void setApplicationDescriptor(Class<?> applicationDescriptorHolder) {
		this.applicationDescriptor = AstrixApplicationDescriptor.create(applicationDescriptorHolder);
	}
	
	/**
	 * All services consumed by the current application. Each type will be created and available
	 * for autowiring in the current applicationContext.
	 * 
	 * Implementation note: This is only application defined usages of Astrix beans. Any Astrix-beans
	 * used internally by the service-framework will not be included in this set. 
	 * 
	 * @return
	 */
	public List<Class<?>> getConsumedAstrixBeans() {
		return consumedAstrixBeans;
	}
	
	public void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}
	
	private DynamicConfig getDynamicConfig(ApplicationContext applicationContext) {
		Collection<DynamicConfig> dynamicConfigs = applicationContext.getBeansOfType(DynamicConfig.class).values();
		if (dynamicConfigs.isEmpty()) {
			return null;
		}
		if (dynamicConfigs.size() == 1) {
			return dynamicConfigs.iterator().next();
		}
		throw new IllegalArgumentException("Multiple DynamicConfig instances found in ApplicationContext");
	}
	
	private AstrixApplicationContext createAsterixContext(DynamicConfig optionalConfig) {
		configurer.setSettings(this.settings);
		if (optionalConfig != null) {
			configurer.setConfig(optionalConfig);
		}
		if (this.subsystem != null) {
			configurer.setSubsystem(this.subsystem);
		}
		return (AstrixApplicationContext) configurer.configure();
	}

	@Override
	public void onApplicationEvent(ApplicationContextEvent event) {
		if (event instanceof ContextRefreshedEvent && !serviceExporterStarted) {
			// Application initialization complete. Export astrix-services.
			exportAllProvidedServices();
			serviceExporterStarted = true;
		} else if (event instanceof ContextClosedEvent || event instanceof ContextStoppedEvent) {
			/*
			 * What's the difference between the "stopped" and "closed" event? In our embedded
			 * integration tests we only receive ContextClosedEvent
			 */
			destroyAstrixContext();
		}
	}

	private void destroyAstrixContext() {
		this.astrixContext.destroy();
	}

	private void exportAllProvidedServices() {
		if (!isServer()) {
			return; // current application exports no services
		}
		String applicationInstanceId = getApplicationInstanceId();
		ServiceExporter serviceExporter = astrixContext.getInstance(ServiceExporter.class);
		
		serviceExporter.addServiceProvider(astrixContext.getInstance(ServiceAdministrator.class));
		ObjectSerializerDefinition serializer = ObjectSerializerDefinition.versionedService(1, ServiceAdministratorVersioningConfigurer.class);
		ServiceDefinition<ServiceAdministrator> serviceDefinition = new ServiceDefinition<>(ApiProvider.create("FrameworkServices"),
																							AstrixBeanKey.create(ServiceAdministrator.class, applicationInstanceId), 
																							serializer, true);
		ExportedServiceBeanDefinition serviceAdminDefintion = new ExportedServiceBeanDefinition(AstrixBeanKey.create(ServiceAdministrator.class, applicationInstanceId), 
																			    serviceDefinition, 
																			    true, // isVersioned  
																			    true, // alwaysActive
																			    AstrixSettings.SERVICE_ADMINISTRATOR_COMPONENT.getFrom(this.astrixContext.getConfig()).get());
		serviceExporter.exportService(serviceAdminDefintion);
		
		serviceExporter.setServiceDescriptor(applicationDescriptor); // TODO This is a hack. Avoid setting serviceDescriptor explicitly here
		serviceExporter.exportProvidedServices();
		serviceExporter.startPublishServices();
	}

	private boolean isServer() {
		return applicationDescriptor != null;
	}
	
	

	private void setupApplicationInstanceId() {
		String applicationInstanceId = AstrixSettings.APPLICATION_INSTANCE_ID.getFrom(this.astrixContext.getConfig()).get();
		if (applicationInstanceId == null) {
			applicationInstanceId = this.applicationDescriptor.toString();
			configurer.set(AstrixSettings.APPLICATION_INSTANCE_ID, this.applicationDescriptor.toString());
			log.info("No applicationInstanceId set, using name of ApplicationDescriptor as applicationInstanceId: {}", applicationInstanceId);
			Objects.requireNonNull(AstrixSettings.APPLICATION_INSTANCE_ID.getFrom(this.astrixContext.getConfig()).get());
		} else {
			log.info("Current applicationInstanceId={}", applicationInstanceId);
		}
	}
	
	private String getApplicationInstanceId() {
		return AstrixSettings.APPLICATION_INSTANCE_ID.getFrom(this.astrixContext.getConfig()).get();
	}

	public void setConsumedAstrixBeans(Class<?>... consumedAstrixBeans) {
		setConsumedAstrixBeans(Arrays.asList(consumedAstrixBeans));
	}
	
	@Override
	public int getOrder() {
		// Run this class as late as possible
		return Ordered.LOWEST_PRECEDENCE;
	}
	
}