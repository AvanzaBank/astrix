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
package com.avanza.astrix.context;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.core.ServiceUnavailableException;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class StatefulAstrixBean<T> implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(StatefulAstrixBean.class);
	private final AstrixFactoryBeanPlugin<T> beanFactory;
	private final String optionalQualifier;
	private volatile InvocationHandler state;
	private static final AtomicInteger nextId = new AtomicInteger(0);
	private final String id = nextId.incrementAndGet() + ""; // Used for debugging to distinguish between many context's started within same jvm.
	private final AstrixEventBus eventBus;
	
	StatefulAstrixBean(AstrixFactoryBeanPlugin<T> beanFactory, String optionalQualifier, AstrixEventBus eventBus) {
		this.beanFactory = beanFactory;
		this.optionalQualifier = optionalQualifier;
		this.eventBus = eventBus;
		this.state = new Unbound();
	}
	
	
	String getId() {
		return id;
	}
	
	public AstrixFactoryBeanPlugin<T> getBeanFactory() {
		return beanFactory;
	}
	
	public boolean isBound() {
		return this.state.getClass().equals(Bound.class);
	}

	public void bind() {
		T bean = this.beanFactory.create(optionalQualifier);
		this.state = new Bound(bean);
		this.eventBus.fireEvent(new AstrixBeanStateChangedEvent(AstrixBeanKey.create(beanFactory.getBeanType(), optionalQualifier), AstrixBeanState.BOUND));
		log.info("Successfully bound to " + beanFactory.getBeanType() + ", AstrixBeanId=" + id);
	}
	
	public void rebind() {
		T bean = this.beanFactory.create(optionalQualifier);
		this.state = new Bound(bean);
		log.info("Successfully rebound to " + beanFactory.getBeanType() + ", AstrixBeanId=" + id);
	}
	

	@Override
	public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
		return state.invoke(proxy, method, args);
	}
	
	private class Bound implements InvocationHandler {

		private final T bean;
		
		public Bound(T bean) {
			this.bean = bean;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			try {
				return method.invoke(bean, args);
			} catch (InvocationTargetException e) {
				log.debug("Service invocation threw exception", e);
				throw e.getTargetException();
			}
		}
	}
	
	private class Unbound implements InvocationHandler {

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new ServiceUnavailableException("AstrixBeanId=" + id + " beanType="+ beanFactory.getBeanType().getName() + " qualifier=" + optionalQualifier);
		}
	}

	
}
