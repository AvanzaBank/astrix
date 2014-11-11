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
package com.avanza.asterix.context;

import java.util.Collection;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixBeanStateWorker extends Thread {

	private static final Logger log = LoggerFactory.getLogger(AsterixBeanStateWorker.class);
	private final Collection<StatefulAsterixBean<?>> managedBeans = new CopyOnWriteArrayList<>();
	private final long beanRebindAttemptIntervalMillis;
	private final ScheduledExecutorService beanStateWorkerThreadPool = Executors.newScheduledThreadPool(1); // TODO: manage life cycle

	public AsterixBeanStateWorker(AsterixSettingsReader settings, AsterixEventBus eventBus) {
		this.beanRebindAttemptIntervalMillis = settings.getLong(AsterixSettings.BEAN_REBIND_ATTEMPT_INTERVAL, 10_000L);
	}
	
	public void add(StatefulAsterixBean<?> bean) {
		this.managedBeans.add(bean);
	}
	
	@Override
	public void run() {
		while(!interrupted()) {
			for (StatefulAsterixBean<?> asterixBean : managedBeans) {
				this.beanStateWorkerThreadPool.execute(new BindCommand(asterixBean));
			}
			try {
				log.debug("Waiting " + this.beanRebindAttemptIntervalMillis + " ms until next bean state inspection");
				Thread.sleep(this.beanRebindAttemptIntervalMillis);
			} catch (InterruptedException e) {
				interrupt();
			} 
		}
		log.info("Shutting down bean state worker. Current managedBean count=" + managedBeans);
	}
	
	private static class BindCommand implements Runnable {
		
		private final StatefulAsterixBean<?> asterixBean;
		
		public BindCommand(StatefulAsterixBean<?> asterixBean) {
			this.asterixBean = asterixBean;
		}

		@Override
		public void run() {
			if (asterixBean.isBound()) {
				return;
			}
			log.debug("Binding bean. asterixBeanId=" + asterixBean.getId());
			try {
				asterixBean.bind();
			} catch (MissingExternalDependencyException e) {
				log.error("Runtime configuration error. Failed to create " + asterixBean.getBeanFactory().getBeanType(), e);
				return;
			} catch (Exception e) {
				log.info("Failed to bind to " + asterixBean.getBeanFactory().getBeanType().getName(), e);
			}
		}
	}
	
}
