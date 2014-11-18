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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.List;

import com.avanza.astrix.context.AstrixApiDescriptor;
import com.avanza.astrix.context.AstrixBeanAware;
import com.avanza.astrix.context.AstrixBeans;
import com.avanza.astrix.context.AstrixFactoryBeanPlugin;
import com.avanza.astrix.context.AstrixInject;
import com.avanza.astrix.context.AstrixServiceComponent;
import com.avanza.astrix.context.AstrixServiceComponents;
import com.avanza.astrix.context.AstrixServiceProperties;
import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.context.AstrixSettingsAware;
import com.avanza.astrix.context.AstrixSettingsReader;
import com.avanza.astrix.context.IllegalSubsystemException;

public class ServiceRegistryLookupFactory<T> implements AstrixFactoryBeanPlugin<T>, AstrixBeanAware, AstrixSettingsAware {

	private Class<T> api;
	private AstrixApiDescriptor descriptor;
	private AstrixBeans beans;
	private AstrixServiceRegistryLeaseManager leaseManager;
	private AstrixServiceComponents serviceComponents;
	private String subsystem;
	private boolean enforceSubsystemBoundaries;

	public ServiceRegistryLookupFactory(AstrixApiDescriptor descriptor,
										Class<T> api) {
		this.descriptor = descriptor;
		this.api = api;
	}

	@Override
	public T create(String qualifier) {
		AstrixServiceRegistryClient serviceRegistry = beans.getBean(AstrixServiceRegistryClient.class);
		AstrixServiceProperties serviceProperties;
		serviceProperties = serviceRegistry.lookup(api, qualifier);
		String providerSubsystem = serviceProperties.getProperty(AstrixServiceProperties.SUBSYSTEM);
		if (!isAllowedToInvokeService(providerSubsystem)) {
			return createIllegalSubsystemProxy(providerSubsystem);
		}
		T service = create(qualifier, serviceProperties);
		return leaseManager.startManageLease(service, serviceProperties, qualifier, this);
	}

	private boolean isAllowedToInvokeService(String providerSubsystem) {
		if (descriptor.isVersioned()) {
			return true;
		}
		if (!enforceSubsystemBoundaries) {
			return true;
		}
		return providerSubsystem.equals(this.subsystem);
	}
	
	private T createIllegalSubsystemProxy(String providerSubsystem) {
		return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class[]{api}, new IllegalSubsystemProxy(subsystem, providerSubsystem, api)));
	}

	public T create(String qualifier, AstrixServiceProperties serviceProperties) {
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("Misssing entry in service-registry api=%s qualifier=%s: ", api.getName(), qualifier));
		}
		AstrixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
		return serviceComponent.createService(descriptor, api, serviceProperties);
	}
	
	private AstrixServiceComponent getServiceComponent(AstrixServiceProperties serviceProperties) {
		String componentName = serviceProperties.getComponent();
		if (componentName == null) {
			throw new IllegalArgumentException("Expected a componentName to be set on serviceProperties: " + serviceProperties);
		}
		return serviceComponents.getComponent(componentName);
	}
	
	@Override
	public List<Class<?>> getBeanDependencies() {
		return Arrays.<Class<?>>asList(AstrixServiceRegistryClient.class);
	}

	@Override
	public Class<T> getBeanType() {
		return this.api;
	}

	@Override
	public void setAstrixBeans(AstrixBeans beans) {
		this.beans = beans;
	}
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceComponents serviceComponents) {
		this.serviceComponents = serviceComponents;
	}
	
	@AstrixInject
	public void setServiceComponents(AstrixServiceRegistryLeaseManager leaseManager) {
		this.leaseManager = leaseManager;
	}
	
	private class IllegalSubsystemProxy<T> implements InvocationHandler {
		
		private String currentSubsystem;
		private String providerSubsystem;
		private Class<T> beanType;
		

		public IllegalSubsystemProxy(String currentSubsystem, String providerSubsystem, Class<T> beanType) {
			this.currentSubsystem = currentSubsystem;
			this.providerSubsystem = providerSubsystem;
			this.beanType = beanType;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new IllegalSubsystemException(currentSubsystem, providerSubsystem, beanType);
		}
	}

	@Override
	public void setSettings(AstrixSettingsReader settings) {
		this.subsystem = settings.getString(AstrixSettings.SUBSYSTEM_NAME);
		this.enforceSubsystemBoundaries = settings.getBoolean(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
	}
	

}
