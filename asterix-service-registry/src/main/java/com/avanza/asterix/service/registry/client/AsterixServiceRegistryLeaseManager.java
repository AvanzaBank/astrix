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
package com.avanza.asterix.service.registry.client;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.asterix.context.AsterixBeanAware;
import com.avanza.asterix.context.AsterixBeans;
import com.avanza.asterix.context.AsterixServiceProperties;
import com.avanza.asterix.context.AsterixSettings;
import com.avanza.asterix.context.AsterixSettingsAware;
import com.avanza.asterix.context.AsterixSettingsReader;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AsterixServiceRegistryLeaseManager extends Thread implements AsterixBeanAware, AsterixSettingsAware {
	
	private final Logger log = LoggerFactory.getLogger(AsterixServiceRegistryLeaseManager.class);
	private final List<LeasedService<?>> leasedServices = new CopyOnWriteArrayList<>();
	private volatile AsterixServiceRegistryClient serviceRegistry;
	private AsterixBeans beans;
	private AsterixSettingsReader settings;
	
	public AsterixServiceRegistryLeaseManager() {
		super("Asterix-ServiceRegistryLeaseManager");
		setDaemon(true);
	}
	
	@Override
	public void run() {
		while (!interrupted()) {
			for (LeasedService<?> leasedService : leasedServices) {
				renewLease(leasedService);
			}
			try {
				Thread.sleep(settings.getLong(AsterixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 30_000L));
			} catch (InterruptedException e) {
				interrupt();
			}
		}
	}

	private void renewLease(LeasedService<?> leasedService) {
		try {
			AsterixServiceProperties serviceProperties = serviceRegistry.lookup(leasedService.getBeanType(), leasedService.getQualifier());
			leasedService.refreshServiceProperties(serviceProperties);
		} catch (Exception e) {
			log.warn("Failed to renew lease for service: " + leasedService.getBeanType());
		}
	}

	public <T> T startManageLease(T service, AsterixServiceProperties currentProperties, String qualifier, ServiceRegistryLookupFactory<T> factory) {
		synchronized (this) {
			if (!isAlive()) {
				this.serviceRegistry = beans.getBean(AsterixServiceRegistryClient.class);
				start(); // TODO: what if thread is interrupted? Just allow exception to be thrown? 
			}
		}
		LeasedService<T> leasedService = new LeasedService<>(service, currentProperties, qualifier, factory);
		leasedServices.add(leasedService);
		Object leasedServiceProxy = Proxy.newProxyInstance(factory.getBeanType().getClassLoader(), new Class[]{factory.getBeanType()}, leasedService);
		return factory.getBeanType().cast(leasedServiceProxy);
	}

	@Override
	public List<Class<?>> getBeanDependencies() {
		return Arrays.<Class<?>>asList(AsterixServiceRegistryClient.class);
	}

	@Override
	public void setAsterixBeans(AsterixBeans beans) {
		this.beans = beans;
	}

	@Override
	public void setSettings(AsterixSettingsReader settings) {
		this.settings = settings;
	}
	
}