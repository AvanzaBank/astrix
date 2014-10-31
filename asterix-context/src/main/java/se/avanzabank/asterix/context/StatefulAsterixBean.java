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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import se.avanzabank.asterix.core.ServiceUnavailableException;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class StatefulAsterixBean<T> implements InvocationHandler {

	private static final Logger log = LoggerFactory.getLogger(StatefulAsterixBean.class);
	private final AsterixFactoryBeanPlugin<T> beanFactory;
	private final String optionalQualifier;
	private volatile InvocationHandler state;
	private static final AtomicInteger nextId = new AtomicInteger(0);
	private final String id = nextId.incrementAndGet() + ""; // TODO remove beanId? its used for debugging
	private final AsterixEventBus eventBus;
	
	StatefulAsterixBean(AsterixFactoryBeanPlugin<T> beanFactory, String optionalQualifier, AsterixEventBus eventBus) {
		this.beanFactory = beanFactory;
		this.optionalQualifier = optionalQualifier;
		this.eventBus = eventBus;
		this.state = new Unbound();
	}
	
	
	public String getId() {
		return id;
	}
	
	public AsterixFactoryBeanPlugin<T> getBeanFactory() {
		return beanFactory;
	}
	
	public boolean isBound() {
		return this.state.getClass().equals(Bound.class);
	}

	public void bind() {
		T bean = this.beanFactory.create(optionalQualifier);
		this.state = new Bound(bean);
		this.eventBus.fireEvent(new AsterixBeanStateChangedEvent(AsterixBeanKey.create(beanFactory.getBeanType(), optionalQualifier), AsterixBeanState.BOUND));
		log.info("Successfully bound to " + beanFactory.getBeanType() + ", asterixBeanId=" + id);
	}
	
	public void rebind() {
		T bean = this.beanFactory.create(optionalQualifier);
		this.state = new Bound(bean);
		log.info("Successfully rebound to " + beanFactory.getBeanType() + ", asterixBeanId=" + id);
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
			// TODO: how to know when its time to go to unbound state.
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
			throw new ServiceUnavailableException("asterixBeanId=" + id + " beanFactoryType="+ beanFactory.getBeanType().getName() + " qualifier=" + optionalQualifier);
		}
	}

	
}
