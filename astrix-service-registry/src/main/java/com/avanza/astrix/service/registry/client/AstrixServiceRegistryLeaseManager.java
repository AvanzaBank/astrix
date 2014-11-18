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
package com.avanza.astrix.service.registry.client;

import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.context.AstrixBeanAware;
import com.avanza.astrix.context.AstrixBeans;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class AstrixServiceRegistryLeaseManager extends Thread implements AstrixBeanAware, AstrixSettingsAware {
	
	private final Logger log = LoggerFactory.getLogger(AstrixServiceRegistryLeaseManager.class);
	private final List<LeasedService<?>> leasedServices = new CopyOnWriteArrayList<>();
	private volatile AstrixServiceRegistryClient serviceRegistry;
	private AstrixBeans beans;
	private AstrixSettingsReader settings;
	
	public AstrixServiceRegistryLeaseManager() {
		super("Astrix-ServiceRegistryLeaseManager");
		setDaemon(true);
	}
	
	@Override
	public void run() {
		while (!interrupted()) {
			for (LeasedService<?> leasedService : leasedServices) {
				renewLease(leasedService);
			}
			try {
				Thread.sleep(settings.getLong(AstrixSettings.SERVICE_REGISTRY_MANAGER_LEASE_RENEW_INTERVAL, 30_000L));
			} catch (InterruptedException e) {
				interrupt();
			}
		}
	}

	private void renewLease(LeasedService<?> leasedService) {
		try {
			AstrixServiceProperties serviceProperties = serviceRegistry.lookup(leasedService.getBeanType(), leasedService.getQualifier());
			leasedService.refreshServiceProperties(serviceProperties);
		} catch (Exception e) {
			log.warn("Failed to renew lease for service: " + leasedService.getBeanType());
		}
	}

	public <T> T startManageLease(T service, AstrixServiceProperties currentProperties, String qualifier, ServiceRegistryLookupFactory<T> factory) {
		synchronized (this) {
			if (!isAlive()) {
				this.serviceRegistry = beans.getBean(AstrixServiceRegistryClient.class);
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
		return Arrays.<Class<?>>asList(AstrixServiceRegistryClient.class);
	}
	
	@PreDestroy
	public void destroy() {
		interrupt();
	}

	@Override
	public void setAstrixBeans(AstrixBeans beans) {
		this.beans = beans;
	}

	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.settings = settings;
	}
	
}