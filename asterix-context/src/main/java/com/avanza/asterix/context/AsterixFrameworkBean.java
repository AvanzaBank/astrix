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
package com.avanza.asterix.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixFrameworkBean implements BeanDefinitionRegistryPostProcessor {
	
	private List<Class<?>> consumedAsterixBeans = new ArrayList<>();
	private String subsystem;
	private Map<String, String> settings = new HashMap<>();
	private AsterixServiceDescriptor serviceDescriptor;
	
	
	/*
	 * We must distinguish between server-side components (those used to export different SERVICES) and
	 * client-side components (those used to consume BEANS (services, libraries, etc)). 
	 * 
	 * For instance: We want the client-side components in every application (web-app, pu, etc)
	 * but only sometimes want the server-side components (we don't want the gs-remoting exporting
	 * mechanism from a web-app).
	 * 
	 * Client side components will have their dependencies injected by AsterixContext (xxxAware).
	 * 
	 * Server side components will have their dependencies injected by spring as it stands now?
	 */
	
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		/*
		 * IMPLEMENTATION NOTE:
		 * 
		 * This is where the asterix-framework register all its required spring-beans in the BeanDefinitionRegistry,
		 * as well as all asterix-beans consumed by the current application (consumedAsterixBeans). 
		 * Both consumer side as well as server side asterix-beans will be registered depending on 
		 * configuration provided by the user of the framework.
		 *  
		 * Its important to avoid premature instantiation of spring-beans. We might do that unintentionally 
		 * if we start pulling beans required by asterix-plugins out from the spring ApplicationContext during
		 * registration of asterix-framework beans. Beans required by different parts of asterix that are 
		 * expected to be provided in the ApplicationContext by the user of the asterix-framework are
		 * called ExternalDependencies (see ExternalDependencyAware). 
		 * 
		 * Hence we must ensure that we don't query the spring ApplicationContext from 
		 * any asterix-plugin during registration of spring beans which starts here.
		 * 
		 * The process for registering all asterix beans required for consuming consumedAsterixBeans 
		 * looks like this:

		 * 1a. Discover all plugins using asterix-plugin-discovery mechanism (spring not involved)
		 * 1b. Scan for api-providers on classpath and build AsterixBeanFactories (spring not involved)
		 *  -> Its important that NO "xxxAware" injected dependency is used in this phase.
		 *     Especially no ExternalDependencyBean since we have not created the ApplicationContext
		 *     which will eventually "wire" the external dependencies into asterix yet.
		 * 1c. For each consumedAsterixBean: Register an AsterixSpringFactoryBean.
		 * 
		 * At this stage all bean-consuming dependencies are in place. 
		 * 
		 * If this application also export a set of services (for instance to the AsterixServiceRegistry), 
		 * then we must also register all required beans/components:
		 * 
		 * 2. Let all AsterixServiceApiPlugin's register their required spring-beans.
		 * 
		 */
		
		try {
			createServiceFrameworkRuntime(registry);
		} catch (ClassNotFoundException e) {
			throw new RuntimeException("Failed to build server runtime", e);
		}
	}

	private void createServiceFrameworkRuntime(BeanDefinitionRegistry registry) throws ClassNotFoundException {
		AsterixConfigurer configurer = new AsterixConfigurer();
		configurer.setSettings(this.settings);
		if (this.serviceDescriptor != null) {
			configurer.setSubsystem(this.serviceDescriptor.getSubsystem());
		} else if (this.subsystem != null) {
			// TODO: use same mechanism to set subsystem for applications that only consume apis for apps providing services
			configurer.setSubsystem(this.subsystem);
		}
		AsterixContext asterixContext = configurer.configure();
		List<Class<?>> consumedAsterixBeans = new ArrayList<>(this.consumedAsterixBeans);
		
		if (serviceDescriptor != null) {
			// This application exports services, build server runtime
			AsterixServerRuntimeBuilder serverRuntimeBuilder = new AsterixServerRuntimeBuilder(asterixContext, serviceDescriptor);
			serverRuntimeBuilder.registerBeanDefinitions(registry);
			consumedAsterixBeans.addAll(serverRuntimeBuilder.getConsumedAsterixBeans());
		}
		AsterixClientRuntimeBuilder clientRuntimeBuilder = new AsterixClientRuntimeBuilder(asterixContext, this.settings, consumedAsterixBeans);
		clientRuntimeBuilder.registerBeanDefinitions(registry);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// intentionally empty, inherited from BeanDefinitionRegistryPostProcessor
	}
	
	public void setConsumedAsterixBeans(List<Class<?>> consumedAsterixBeans) {
		this.consumedAsterixBeans = consumedAsterixBeans;
	}
	
	
	// TODO: remove this settings method?
	public void setSettings(Map<String, String> settings) {
		this.settings = settings;
	}
	
	public Map<String, String> getSettings() {
		return settings;
	}
	
	/**
	 * If a service descriptor is provided, then the service exporting part of the framework
	 * will be loaded with all required components for the given serviceDescriptor.
	 * 
	 * @param serviceDescriptor
	 */
	public void setServiceDescriptor(Class<?> serviceDescriptorHolder) {
		this.serviceDescriptor = AsterixServiceDescriptor.create(serviceDescriptorHolder);
	}
	
	/**
	 * All services consumed by the current application. Each type will be created and available
	 * for autowiring in the current applicationContext.
	 * 
	 * Implementation note: This is only application defined usages of asterix beans. Any asterix-beans
	 * used internally by the service-framework will not be included in this set. 
	 * 
	 * @return
	 */
	public List<Class<?>> getConsumedAsterixBeans() {
		return consumedAsterixBeans;
	}
	
	public void setSubsystem(String subsystem) {
		this.subsystem = subsystem;
	}
	
}