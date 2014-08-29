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
public class AstrixFrameworkBean implements BeanDefinitionRegistryPostProcessor {
	
	private List<Class<?>> consumedAstrixBeans = new ArrayList<>();

	/*
	 * We must distinguish between server-side components (those used to export different SERVICES) and
	 * client-side components (those used to consume BEANS (services, librarys, ets)). 
	 * 
	 * For instance: We want the client-side components in every application (web-app, pu, etc)
	 * but only sometimes want the server-side components (we don't want the gs-remoting exporting
	 * mechanism from a web-app).
	 * 
	 * Client side components will have their dependencies injected by AstrixContext (xxxAware).
	 * 
	 * Server side components will have their dependencies injected by spring as
	 * it stands now. Server side components are registered by AstrixBeanRegistryPlugin's. 
	 * 
	 * 
	 */
	
	@Override
	public void postProcessBeanDefinitionRegistry(BeanDefinitionRegistry registry) throws BeansException {
		/*
		 * NOTE: its important to avoid premature instantiation of spring-beans, hence
		 * we may not query the spring ApplicationContext from astrix-classes, since we
		 * don't know in what order spring will instantiate beans.
		 * 
		 * Therefore we init process looks like this:
		 * 
		 * 1. Discover all plugins using astrix-plugin-discovery
		 * 2. Scan for api-providers on classpath and build AstrixBeanFactories
		 *  -> Its important that no "xxxAware" injected dependency is used in this phase,
		 *  especially no ExternalDependencyBean since we have created the ApplicationContext yet.
		 *  
		 * 3. For each consumedApi register a AstrixSpringFactoryBean.
		 * 
		 * 4. Let other service-providing component register it's spring beans. TODO: how to do this in clean way?
		 * 
		 */
		
		// TODO: avoid creating two AstrixContext's (here and as spring bean)
		AstrixContext astrixContext = new AstrixConfigurer().configure(); 
		
		// For each consumedApi, either directly or indirectly (for instance via a library), 
		// we must register a bean definition for the ExternalDependencyBean to "collect" the dependencies from
		// the application-context
		Set<Class<? extends ExternalDependencyBean>> externalDependencyBeanTypes = resolveAllExternalDependencies(astrixContext);
		for (Class<?> dependencBeanClass : externalDependencyBeanTypes) {
			AnnotatedGenericBeanDefinition dependenciesBeanDefinition = new AnnotatedGenericBeanDefinition(dependencBeanClass);
			String dependencyBeanName = "_astrixDependencyBean" + dependencBeanClass.getName();
			registry.registerBeanDefinition(dependencyBeanName, dependenciesBeanDefinition);
		}
		
		// AstrixConfigurer must be created AFTER all dependency-beans have bean created.
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AstrixConfigurer.class);
		beanDefinition.setAutowireMode(Autowire.BY_TYPE.value());
		beanDefinition.setDependsOn(getDependencyBeanNames(externalDependencyBeanTypes));
		registry.registerBeanDefinition("_astrixConfigurer", beanDefinition);

		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixContext.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_astrixConfigurer");
		beanDefinition.setFactoryMethodName("configure");
		registry.registerBeanDefinition("_astrixContext", beanDefinition);
		
		beanDefinition = new AnnotatedGenericBeanDefinition(AstrixPlugins.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		beanDefinition.setFactoryBeanName("_astrixContext");
		beanDefinition.setFactoryMethodName("getPlugins");
		registry.registerBeanDefinition("_astrixPlugins", beanDefinition);
		
		
		for (Class<?> consumedAstrixBean : consumedAstrixBeans) {
			beanDefinition = new AnnotatedGenericBeanDefinition(AstrixSpringFactoryBean.class);
			MutablePropertyValues props = new MutablePropertyValues();
			props.add("type", consumedAstrixBean);
			beanDefinition.setPropertyValues(props);
			registry.registerBeanDefinition("_" + consumedAstrixBean.getName(), beanDefinition);
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
		for (Class<?> consumedBeans : resolveAllConsumedBeans(astrixContext)) {
			Class<? extends ExternalDependencyBean> externalDependencyBean =
					astrixContext.getExternalDependencyBean(consumedBeans);
			if (externalDependencyBean != null) {
				result.add(externalDependencyBean);
			}
		}
		return result;
	}

	private Collection<Class<?>> resolveAllConsumedBeans(AstrixContext astrixContext) {
		return new AstrixBeanDependencyResolver(astrixContext).resolveConsumedBeans(consumedAstrixBeans);
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		// intentionally empty, inherited from BeanDefinitionRegistryPostProcessor
	}
	
	public void setConsumedAstrixBeans(List<Class<?>> consumedAstrixBeans) {
		this.consumedAstrixBeans = consumedAstrixBeans;
	}
	
	public List<Class<?>> getConsumedApis() {
		return consumedAstrixBeans;
	}
	
}