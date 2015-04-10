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
package com.avanza.astrix.beans.factory;

import java.util.List;
import java.util.Set;
import java.util.Stack;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixBeanFactory {
	
	private final AstrixFactoryBeanRegistry registry;
	private final ObjectCache beanInstanceCache = new ObjectCache();
	private final List<AstrixBeanPostProcessor> beanPostProcessors = new CopyOnWriteArrayList<>();
	
	public AstrixBeanFactory(AstrixFactoryBeanRegistry registry) {
		this.registry = registry;
	}
	
	public <T> T getBean(final AstrixBeanKey<T> key) {
		return new CircularDependencyAwareAstrixBeanInstances().getBean(key);
	}
	
	public <T> Set<AstrixBeanKey<T>> getBeanKeysOfType(Class<T> type) {
		return this.registry.getBeansOfType(type);
	}

	@PreDestroy
	public void destroy() {
		this.beanInstanceCache.destroy();
	}

	/**
	 * This method returns all beans that was requested during creation of a given bean. Effectively returning
	 * all direct and transitive dependencies for a given bean.
	 * 
	 * NOTE: This method will trigger CREATION of the given bean if its not created before.
	 * 
	 * @param beanKey
	 * @return
	 */
	public Set<AstrixBeanKey<? extends Object>> getDependencies(AstrixBeanKey<? extends Object> beanKey) {
		return new CircularDependencyAwareAstrixBeanInstances().getBeanInstance(beanKey).getDependencies();
	}
	
	/**
	 * The CircularDependencyAwareAstrixBeans is responsible for:
	 * 
	 * 1. Detecting circular dependencies.
	 * 2. Ensure that each created bean is managed by the ObjectCache.
	 *
	 */
	private class CircularDependencyAwareAstrixBeanInstances implements AstrixBeans {
		private final Stack<AstrixBeanKey<?>> constructionStack = new Stack<>();
		
		@Override
		public <T> T getBean(final AstrixBeanKey<T> beanKey) {
			return getBeanInstance(beanKey).get();
		}
		
		public <T> AstrixBeanInstance<? extends T> getBeanInstance(AstrixBeanKey<T> beanKey) {
			final AstrixBeanKey<? extends T> resolvedBeanKey = registry.resolveBean(beanKey);
			return beanInstanceCache.getInstance(resolvedBeanKey, new ObjectCache.ObjectFactory<AstrixBeanInstance<? extends T>>() {
				@Override
				public AstrixBeanInstance<? extends T> create() throws Exception {
					// Bean instance not created, create!
					try {
						return doCreateBean(resolvedBeanKey);
					} catch (MissingBeanProviderException e) {
						if (constructionStack.size() > 1) {
							// Its a dependency thats missing
							throw new MissingBeanDependencyException(e.getBeanType(), constructionStack);
						}
						// It's the top level bean thats missing, propagate
						throw e;
					}
				}
			});
		}
		
		private <T> AstrixBeanInstance<? extends T> doCreateBean(final AstrixBeanKey<T> beanKey) {
			if (constructionStack.contains(beanKey)) {
				throw new AstrixCircularDependency(constructionStack);
			}
			constructionStack.add(beanKey);
			StandardFactoryBean<? extends T> factoryBean = registry.getFactoryBean(beanKey);
			AstrixBeanInstance<? extends T> instance = createBeanInstance(factoryBean);
			for (AstrixBeanPostProcessor beanPostProcessor : beanPostProcessors) {
				beanPostProcessor.postProcess(instance.get(), this);
			}
			constructionStack.pop();
			return instance;
		}

		private <T> AstrixBeanInstance<T> createBeanInstance(StandardFactoryBean<T> factoryBean) {
			return AstrixBeanInstance.create(this, factoryBean);
		}
		
		@Override
		public <T> Set<AstrixBeanKey<T>> getBeansOfType(Class<T> type) {
			return registry.getBeansOfType(type);
		}
		
	}

	public void registerBeanPostProcessor(AstrixBeanPostProcessor beanPostProcessor) {
		this.beanPostProcessors.add(beanPostProcessor);
	}
}