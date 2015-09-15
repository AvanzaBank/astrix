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
package com.avanza.astrix.beans.api;

import java.util.Arrays;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.factory.AstrixBeans;
import com.avanza.astrix.beans.factory.BeanProxy;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.context.core.AsyncTypeConverter;
import com.avanza.astrix.context.core.BeanInvocationDispatcher;
import com.avanza.astrix.core.util.ReflectionUtil;

final class AstrixFtProxiedFactory<T> implements StandardFactoryBean<T> {
	
	private final StandardFactoryBean<T> target;
	private final BeanFaultToleranceFactory faultToleranceProxyFactory;
	private final PublishedAstrixBean<T> beanDefinition;
	private final AsyncTypeConverter asyncTypeConverter;

	public AstrixFtProxiedFactory(StandardFactoryBean<T> target,
								  BeanFaultToleranceFactory faultToleranceProxyFactory,
								  PublishedAstrixBean<T> beanDefinition,
								  AsyncTypeConverter asyncTypeConverter) {
		this.target = target;
		this.faultToleranceProxyFactory = faultToleranceProxyFactory;
		this.beanDefinition = beanDefinition;
		this.asyncTypeConverter = asyncTypeConverter;
	}

	@Override
	public T create(AstrixBeans beans) {
		T rawBean = target.create(beans);
		BeanProxy ftProxy = faultToleranceProxyFactory.createFaultToleranceProxy(beanDefinition);
		BeanInvocationDispatcher beanProxyDispather = new BeanInvocationDispatcher(Arrays.asList(ftProxy), asyncTypeConverter, rawBean);
		return ReflectionUtil.newProxy(getBeanKey().getBeanType(), beanProxyDispather);
	}

	@Override
	public AstrixBeanKey<T> getBeanKey() {
		return target.getBeanKey();
	}

}
