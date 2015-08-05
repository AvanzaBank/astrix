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
package com.avanza.astrix.beans.factory;

import java.util.HashSet;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.modules.ObjectCache;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixBeanInstance<T> {
	
	private final T instance;
	private final AstrixBeanKey<?> beanKey;
	private final Set<AstrixBeanKey<?>> dependencies;
	private boolean lifecycle;
	
	private AstrixBeanInstance(AstrixBeanKey<T> beanKey, T instance, Set<AstrixBeanKey<?>> transitiveDependencies, boolean lifecycle) {
		this.beanKey = beanKey;
		this.instance = instance;
		dependencies = transitiveDependencies;
		this.lifecycle = lifecycle;
	}
	
	static <T> AstrixBeanInstance<T> create(AstrixBeans factoryBeanContext, StandardFactoryBean<T> factory) {
		TransitiveDependencyCollector dependencyCollectingContext = new TransitiveDependencyCollector(factoryBeanContext);
		T instance = factory.create(dependencyCollectingContext);
		return new AstrixBeanInstance<T>(factory.getBeanKey(), instance, dependencyCollectingContext.getTransitiveDependencies(), factory.lifecycled());
	}
	
	public T get() {
		return instance;
	}
	
	public AstrixBeanKey<?> getKey() {
		return beanKey;
	}
	
	/**
	 * Returns a key for each bean that was requested during creation of this bean, i.e
	 * all its direct and transitive dependencies.
	 * @return
	 */
	public Set<AstrixBeanKey<?>> getDependencies() {
		return dependencies;
	}
	
	@PreDestroy
	public final void destroy() {
		if (lifecycle) {
			ObjectCache.destroy(get());
		}
	}
	
	@PostConstruct
	public final void init() {
		if (lifecycle) {
			ObjectCache.init(get());
		}
	}
	
	private static class TransitiveDependencyCollector implements AstrixBeans {
		private final AstrixBeans beanCreationContext;
		private final Set<AstrixBeanKey<?>> transitiveDependencies = new HashSet<>();
		
		public TransitiveDependencyCollector(AstrixBeans beanCreationContext) {
			this.beanCreationContext = beanCreationContext;
		}
		@Override
		public <T> T getBean(AstrixBeanKey<T> key) {
			this.transitiveDependencies.add(key);
			return beanCreationContext.getBean(key);
		}
		
		public Set<AstrixBeanKey<?>> getTransitiveDependencies() {
			return transitiveDependencies;
		}
		
		@Override
		public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
			return beanCreationContext.getBeansOfType(type);
		}
	}
	
	@Override
	public String toString() {
		return this.beanKey.toString();
	}

	public void postProcess(AstrixBeanPostProcessor beanPostProcessor, AstrixBeans astrixBeans) {
		if (lifecycle) {
			beanPostProcessor.postProcess(get(), astrixBeans);
		}
	}
	
}