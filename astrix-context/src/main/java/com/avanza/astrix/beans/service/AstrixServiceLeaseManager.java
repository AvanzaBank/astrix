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
package com.avanza.astrix.beans.service;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.DynamicConfig;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceLeaseManager {
	
	private final Logger log = LoggerFactory.getLogger(AstrixServiceLeaseManager.class);
	private final List<AstrixServiceBeanInstance<?>> leasedServices = new CopyOnWriteArrayList<>();
	private final DynamicConfig config;
	private final ServiceLeaseRenewalThread leaseRenewalThread = new ServiceLeaseRenewalThread();
	private final ServiceBindThread serviceBindThread = new ServiceBindThread();
	private final AtomicBoolean isStarted = new AtomicBoolean(false);
	
	public AstrixServiceLeaseManager(DynamicConfig config) {
		this.config = config;
	}
	
	public <T> void startManageLease(AstrixServiceBeanInstance<T> serviceBeanInstance) {
		synchronized (isStarted) {
			if (!isStarted.get()) {
				start();
			}
		}
		leasedServices.add(serviceBeanInstance);
	}
	
	private void start() {
		this.leaseRenewalThread.start();
		this.serviceBindThread.start();
		isStarted.set(true);
	}

	@PreDestroy
	public void destroy() {
		this.leaseRenewalThread.interrupt();
		this.serviceBindThread.interrupt();
		for (AstrixServiceBeanInstance<?> leasedService : this.leasedServices) {
			try {
				leasedService.destroy();
			} catch (Exception e) {
				log.warn(String.format("Failed to release service bean: %s", leasedService.getBeanKey()), e);
			}
		}
	}

	private class ServiceBindThread extends Thread {
		
		public ServiceBindThread() {
			super("Astrix-ServiceBind");
			setDaemon(true);
		}
		
		@Override
		public void run() {
			while (!interrupted()) {
				for (AstrixServiceBeanInstance<?> leasedService : leasedServices) {
					if (!leasedService.isBound()) {
						bind(leasedService);
					}
				}
				try {
					Thread.sleep(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL.getFrom(config).get());
				} catch (InterruptedException e) {
					interrupt();
				}
			}
			log.info("Terminating thread=" + getName());
		}
		
		private void bind(AstrixServiceBeanInstance<?> leasedService) {
			try {
				log.debug("Attempting to bind service={}", leasedService.getBeanKey());
				leasedService.bind();
			} catch (Exception e) {
				log.warn("Failed to bind service: " + leasedService.getBeanKey(), e);
			}
		}
	}
	
	private class ServiceLeaseRenewalThread extends Thread {
		
		public ServiceLeaseRenewalThread() {
			super("Astrix-ServiceLeaseRenewal");
			setDaemon(true);
		}
		
		@Override
		public void run() {
			while (!interrupted()) {
				for (AstrixServiceBeanInstance<?> leasedService : leasedServices) {
					renewLease(leasedService);
				}
				try {
					Thread.sleep(AstrixSettings.SERVICE_LEASE_RENEW_INTERVAL.getFrom(config).get());
				} catch (InterruptedException e) {
					interrupt();
				}
			}
			log.info("Terminating thread=" + getName());
		}
		
		private void renewLease(AstrixServiceBeanInstance<?> leasedService) {
			try {
				leasedService.renewLease();
			} catch (Exception e) {
				log.warn("Failed to renew lease for service: " + leasedService.getBeanKey(), e);
			}
		}
	}
	
}