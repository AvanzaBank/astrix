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
package se.avanzabank.asterix.context;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixFrameworkBean implements BeanDefinitionRegistryPostProcessor {
	
	private List<Class<?>> consumedAsterixBeans = new ArrayList<>();
	private Class<?> serviceDescriptor;

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
	 * Server side components will have their dependencies injected by spring as
	 * it stands now. Server side components are registered by AsterixBeanRegistryPlugin's. 
	 * 
	 * 
	 */
	
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		/*
		 * NOTE: its important to avoid premature instantiation of spring-beans, hence
		 * we may not query the spring ApplicationContext from asterix-classes, since we
		 * don't know in what order spring will instantiate beans.
		 * 
		 * Therefore we init process looks like this:
		 * 
		 * 1. Discover all plugins using asterix-plugin-discovery
		 * 2. Scan for api-providers on classpath and build AsterixBeanFactories
		 *  -> Its important that no "xxxAware" injected dependency is used in this phase,
		 *  especially no ExternalDependencyBean since we have created the ApplicationContext yet.
		 *  
		 * 3. For each consumedApi register a AsterixSpringFactoryBean.
		 * 
		 * 4. Let other service-providing component register it's spring beans. TODO: how to do this in clean way?
		 * 
		 */
		
		// TODO: avoid creating two AsterixContext's  (here and as spring bean)
		AsterixContext asterixContext = new AsterixConfigurer().configure(); 
		
		// For each consumedApi, either directly or indirectly (for instance via a library), 
		// we must register a bean definition for the ExternalDependencyBean to "collect" the dependencies from
		// the application-context
		Set<Class<? extends ExternalDependencyBean>> externalDependencyBeanTypes = resolveAllExternalDependencies(asterixContext);
		for (Class<?> dependencBeanClass : externalDependencyBeanTypes) {
			AnnotatedGenericBeanDefinition dependenciesBeanDefinition = new AnnotatedGenericBeanDefinition(dependencBeanClass);
			String dependencyBeanName = "_asterixDependencyBean" + dependencBeanClass.getName();
			registry.registerBeanDefinition(dependencyBeanName, dependenciesBeanDefinition);
		}
		
		// AsterixConfigurer must be created AFTER all dependency-beans have bean created.
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixConfigurer.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		beanDefinition.setDependsOn(getDependencyBeanNames(externalDependencyBeanTypes));
		registry.registerBeanDefinition("_asterixConfigurer", beanDefinition);

		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixContext.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_asterixConfigurer");
		beanDefinition.setFactoryMethodName("configure");
		registry.registerBeanDefinition("_asterixContext", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixPlugins.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_asterixContext");
		beanDefinition.setFactoryMethodName("getPlugins");
		registry.registerBeanDefinition("_asterixPlugins", beanDefinition);
		
		
		for (Class<?> consumedAsterixBean : consumedAsterixBeans) {
			beanDefinition = new AnnotatedGenericBeanDefinition(AsterixSpringFactoryBean.class);
			MutablePropertyValues props = new MutablePropertyValues();
			props.add("type", consumedAsterixBean);
			beanDefinition.setPropertyValues(props);
			registry.registerBeanDefinition("_" + consumedAsterixBean.getName(), beanDefinition);
		}
		
		if (serviceDescriptor == null) {
			// Does not export any services, don't load any service-providing components
			return;
		}
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AsterixApiDescriptor.class);
		beanDefinition.setConstructorArgumentValues(new ConstructorArgumentValues(){{
			addIndexedArgumentValue(0, serviceDescriptor);
		}});
		registry.registerBeanDefinition("_asterixServiceDescriptor", beanDefinition); // TODO: rename to apiDescriptor?
		for (AsterixBeanRegistryPlugin beanRegistryPlugin :
			asterixContext.getPlugins(AsterixBeanRegistryPlugin.class)) {
			if (!this.serviceDescriptor.isAnnotationPresent(beanRegistryPlugin.getDescriptorType())) {
				continue;
			}
			beanRegistryPlugin.registerBeanDefinitions(registry, new AsterixApiDescriptor(serviceDescriptor));
		}
	}

	private String[] getDependencyBeanNames(
			Set<Class<? extends ExternalDependencyBean>> externalDependencyBeanTypes) {
		List<String> result = new ArrayList<>(externalDependencyBeanTypes.size());
		for (Class<?> externalDependencyBean : externalDependencyBeanTypes) {
			result.add("_asterixDependencyBean" + externalDependencyBean.getName());
		}
		return result.toArray(new String[result.size()]);
	}

	private Set<Class<? extends ExternalDependencyBean>> resolveAllExternalDependencies(
			AsterixContext asterixContext) {
		Set<Class<? extends ExternalDependencyBean>> result = new HashSet<>();
		for (Class<?> consumedBeans : resolveAllConsumedBeans(asterixContext)) {
			Class<? extends ExternalDependencyBean> externalDependencyBean =
					asterixContext.getExternalDependencyBean(consumedBeans);
			if (externalDependencyBean != null) {
				result.add(externalDependencyBean);
			}
		}
		return result;
	}

	private Collection<Class<?>> resolveAllConsumedBeans(AsterixContext asterixContext) {
		return new AsterixBeanDependencyResolver(asterixContext).resolveTransitiveBeanDependencies(consumedAsterixBeans);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// intentionally empty, inherited from BeanDefinitionRegistryPostProcessor
	}
	
	public void setConsumedAsterixBeans(List<Class<?>> consumedAsterixBeans) {
		this.consumedAsterixBeans = consumedAsterixBeans;
	}
	
	/**
	 * If a service descriptor is provided, then the service exporting part of the framework
	 * will be loaded with all required components for the given serviceDescriptor.
	 * 
	 * @param serviceDescriptor
	 */
	public void setServiceDescriptor(Class<?> serviceDescriptor) {
		this.serviceDescriptor = serviceDescriptor;
	}
	
	public List<Class<?>> getConsumedApis() {
		return consumedAsterixBeans;
	}
	
}