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
import java.util.Map;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.annotation.Autowire;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
/**
 * Registers beans associated with consumption of asterix beans. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixClientRuntimeBuilder {

	private List<Class<?>> consumedAsterixBeans;
	private AsterixContext asterixContext;
	private Map<String, String> settings;
	private Set<Class<? extends ExternalDependencyBean>> usedExternalDependencyBeanTypes;
	
	public AsterixClientRuntimeBuilder(AsterixContext asterixContext, Map<String, String> settings, List<Class<?>> consumedAsterixBeans) {
		this.asterixContext = asterixContext;
		this.settings = settings;
		this.consumedAsterixBeans = new ArrayList<>(consumedAsterixBeans);
	}

	public void registerBeanDefinitions(BeanDefinitionRegistry registry) throws BeansException {
		usedExternalDependencyBeanTypes = resolveAllExternalDependencies(asterixContext);
		for (Class<?> dependencBeanClass : usedExternalDependencyBeanTypes) {
			AnnotatedGenericBeanDefinition dependenciesBeanDefinition = new AnnotatedGenericBeanDefinition(dependencBeanClass);
			String dependencyBeanName = "_asterixDependencyBean" + dependencBeanClass.getName();
			registry.registerBeanDefinition(dependencyBeanName, dependenciesBeanDefinition);
		}
		
		
		// AsterixConfigurer must be created AFTER all dependency-beans have bean created.
		AnnotatedGenericBeanDefinition beanDefinition = new AnnotatedGenericBeanDefinition(AsterixConfigurer.class);
		beanDefinition.setAutowireMode(Autowire.NO.value());
		MutablePropertyValues asterixConfigurerProps = new MutablePropertyValues();
//		asterixConfigurerProps.add("settings", this.settings);
		asterixConfigurerProps.add("subsystem", this.asterixContext.getCurrentSubsystem());
		beanDefinition.setPropertyValues(asterixConfigurerProps);
		beanDefinition.setDependsOn(getDependencyBeanNames(usedExternalDependencyBeanTypes));
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
		Set<Class<?>> result = new HashSet<>(consumedAsterixBeans);
		for (Class<?> directBeanDependency : this.consumedAsterixBeans) {
			result.addAll(asterixContext.getTransitiveBeanDependenciesForBean(directBeanDependency));
		}
		return result;
	}
}
