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

import java.lang.reflect.Proxy;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceLeaseManager extends Thread implements AstrixSettingsAware {
	
	private final Logger log = LoggerFactory.getLogger(AstrixServiceLeaseManager.class);
	private final List<LeasedService<?>> leasedServices = new CopyOnWriteArrayList<>();
	private AstrixSettingsReader settings;
	
	public AstrixServiceLeaseManager() {
		super("Astrix-ServiceLeaseManager");
		setDaemon(true);
	}
	
	@Override
	public void run() {
		while (!interrupted()) {
			for (LeasedService<?> leasedService : leasedServices) {
				renewLease(leasedService);
			}
			try {
				Thread.sleep(settings.getLongProperty(AstrixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 30_000L).get());
			} catch (InterruptedException e) {
				interrupt();
			}
		}
	}

	private void renewLease(LeasedService<?> leasedService) {
		try {
			leasedService.renew();
		} catch (Exception e) {
			log.warn("Failed to renew lease for service: " + leasedService.getBeanType());
		}
	}

	public <T> T startManageLease(T service, AstrixServiceProperties currentProperties, AstrixServiceFactory<T> serviceFactory, AstrixServiceLookup serviceLookup) {
		synchronized (this) {
			if (!isAlive()) {
				start(); // TODO: what if thread is interrupted? Just allow exception to be thrown? 
			}
		}
		LeasedService<T> leasedService = new LeasedService<>(service, currentProperties, serviceFactory, serviceLookup);
		leasedServices.add(leasedService);
		Object leasedServiceProxy = Proxy.newProxyInstance(serviceFactory.getBeanKey().getBeanType().getClassLoader(), new Class[]{serviceFactory.getBeanKey().getBeanType()}, leasedService);
		return serviceFactory.getBeanKey().getBeanType().cast(leasedServiceProxy);
	}
	
	@PreDestroy
	public void destroy() {
		interrupt();
	}

	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}
	
}