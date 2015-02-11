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

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.DynamicLongProperty;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixBeanStateWorker extends Thread implements AstrixConfigAware {

	private static final Logger log = LoggerFactory.getLogger(AstrixBeanStateWorker.class);
	private final Collection<StatefulAstrixBean<?>> managedBeans = new CopyOnWriteArrayList<>();
	private DynamicLongProperty beanRebindAttemptIntervalMillis;
	private final ExecutorService beanStateWorkerThreadPool = Executors.newSingleThreadExecutor(new ThreadFactory() {
		@Override
		public Thread newThread(Runnable r) {
			Thread t = new Thread(r, "Astrix-BeanStateWorkerExecutor");
			t.setDaemon(true);
			return t;
		}
	});

	public AstrixBeanStateWorker() {
		super("Astrix-BeanStateWorkerDispatcher");
		setDaemon(true);
	}
	
	@PreDestroy
	public void destroy() {
		interrupt();
	}
	
	public void add(StatefulAstrixBean<?> bean) {
		synchronized (this) {
			if (!isAlive()) {
				start();
			}
		}
		this.managedBeans.add(bean);
	}
	
	@Override
	public void run() {
		while(!interrupted()) {
			for (StatefulAstrixBean<?> AstrixBean : managedBeans) {
				this.beanStateWorkerThreadPool.execute(new BindCommand(AstrixBean));
			}
			try {
				log.debug("Waiting " + this.beanRebindAttemptIntervalMillis + " ms until next bean state inspection");
				Thread.sleep(this.beanRebindAttemptIntervalMillis.get());
			} catch (InterruptedException e) {
				interrupt();
			} 
		}
		beanStateWorkerThreadPool.shutdown();
		log.info("Shutting down bean state worker. Current managedBean count=" + managedBeans);
	}
	
	private static class BindCommand implements Runnable {
		
		private final StatefulAstrixBean<?> astrixBean;
		
		public BindCommand(StatefulAstrixBean<?> astrixBean) {
			this.astrixBean = astrixBean;
		}

		@Override
		public void run() {
			if (astrixBean.isBound()) {
				return;
			}
			log.debug("Binding bean. astrixBeanId=" + astrixBean.getId());
			try {
				astrixBean.bind();
			} catch (Exception e) {
				log.info("Failed to bind to " + astrixBean.getBeanFactory().getBeanKey().getBeanType().getName() + " astrixBeanId=" + astrixBean.getId(), e);
			}
		}
	}

	@Override
	public void setConfig(DynamicConfig config) {
		this.beanRebindAttemptIntervalMillis = config.getLongProperty(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 10_000L);
	}
	
}
