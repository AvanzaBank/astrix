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
package se.avanzabank.service.suite.context;

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
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.BeanDefinitionRegistryPostProcessor;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceFrameworkBean implements BeanDefinitionRegistryPostProcessor {
	
	// TODO: rename to consumedServices?
	private List<Class<?>> consumedApis = new ArrayList<>();

	/*
	 * We must separate between server-side components (those used to export different services) and
	 * client-side components (those used to consume services). 
	 * 
	 * For instance: We wan't the client-side components in every application (web-app, pu, etc)
	 * but only sometimes want the server-side components (we don't want the gs-remoting exporting
	 * mechanism from a web-app).
	 */
	
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		/*
		 * TODO:
		 * 1.a discover all plugins (remoting, messaging, library etc)
		 *  .b discover all service-providers 
		 *  .c create meta-data model (no instantiation) for all services available
		 *  
		 * 2.  for each 'consumedApi':
		 *  .a Retrieve provider for api.
		 *  .b Register bean-definition for 'dependency-bean' for that plugin
		 *  .c somehow: register a FactoryBean that depends on 'dependency-bean' and creates api
		 *  
		 */
		
		// TODO: avoid creating two AstrixContext's (here and as spring bean)
		AstrixContext astrixContext = new AstrixConfigurer().configure(); 
		
		// For each consumed service, either directly or indirectly (for instance via a library), 
		// we must register a bean definition for the dependency bean to "collect" the dependencies from
		// the application-context
		Set<Class<? extends ExternalDependencyBean>> externalDependencyBeanTypes = resolveAllExternalDependencies(astrixContext);
		for (Class<?> dependencBeanClass : externalDependencyBeanTypes) {
			AnnotatedGenericBeanDefinition dependenciesBeanDefinition = new AnnotatedGenericBeanDefinition(dependencBeanClass);
			String dependencyBeanName = "_astrixDependencyBean" + dependencBeanClass.getName();
			registry.registerBeanDefinition(dependencyBeanName, dependenciesBeanDefinition);
		}
		
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixPlugins.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		registry.registerBeanDefinition("_astrixPlugins", beanDefinition);
		
		// AstrixConfigurer must be created AFTER all dependency-beans have bean created
		// TODO: is this setDependsOn really needed? Won't spring ensure this since we autowire all ExternalDependencyBean's
		// into the AstrixConfigurer?
		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixConfigurer.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		beanDefinition.setDependsOn(getDependencyBeanNames(externalDependencyBeanTypes));
		registry.registerBeanDefinition("_astrixConfigurer", beanDefinition);

		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixContext.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_astrixConfigurer");
		beanDefinition.setFactoryMethodName("configure");
		registry.registerBeanDefinition("_astrixContext", beanDefinition);
		
		for (Class<?> consumedService : consumedApis) {
			beanDefinition = new AnnotatedGenericBeanDefinition(AstrixServiceFactoryBean.class);
			MutablePropertyValues props = new MutablePropertyValues();
			props.add("type", consumedService);
			beanDefinition.setPropertyValues(props);
			registry.registerBeanDefinition("_" + consumedService.getName(), beanDefinition);
		}
			
		for (AstrixBeanRegistryPlugin beanRegistryPlugin :
			AstrixPluginDiscovery.discoverPlugins(AstrixBeanRegistryPlugin.class)) {
			beanRegistryPlugin.registerBeanDefinitions(registry);
		}
	}

	private String[] getDependencyBeanNames(
			Set<Class<? extends ExternalDependencyBean>> externalDependencyBeanTypes) {
		List<String> result = new ArrayList<>(externalDependencyBeanTypes.size());
		for (Class<?> externalDependencyBean : externalDependencyBeanTypes) {
			result.add("_astrixDependencyBean" + externalDependencyBean.getName());
		}
		return result.toArray(new String[result.size()]);
	}

	private Set<Class<? extends ExternalDependencyBean>> resolveAllExternalDependencies(
			AstrixContext astrixContext) {
		Set<Class<? extends ExternalDependencyBean>> result = new HashSet<>();
		for (Class<?> consumedService : resolveAllConsumedServices(astrixContext)) {
			Class<? extends ExternalDependencyBean> externalDependencyBean =
					astrixContext.getExternalDependencyBean(consumedService);
			if (externalDependencyBean != null) {
				result.add(externalDependencyBean);
			}
		}
		return result;
	}

	private Collection<Class<?>> resolveAllConsumedServices(AstrixContext astrixContext) {
		return new ServiceDependencyResolver(astrixContext).resolveConsumedServices(consumedApis);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// intentionally empty, inherited from BeanDefinitionRegistryPostProcessor
	}
	
	public void setConsumedApis(List<Class<?>> consumedApis) {
		this.consumedApis = consumedApis;
	}
	
	public List<Class<?>> getConsumedApis() {
		return consumedApis;
	}
	
}