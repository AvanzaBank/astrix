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
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public class AstrixServiceFactory<T> implements AstrixFactoryBeanPlugin<T> {
	
	private final Class<T> api;
	private final AstrixServiceComponents serviceComponents;
	private final AstrixServiceLookup serviceLookup;
	private final String subsystem;
	private final boolean enforceSubsystemBoundaries;
	private final AstrixServiceLeaseManager leaseManager;
	private final ServiceVersioningContext versioningContext;

	public AstrixServiceFactory(ServiceVersioningContext serviceVersioningContext, 
								Class<T> beanType, 
								AstrixServiceLookup serviceLookup, 
								AstrixServiceComponents serviceComponents, 
								AstrixServiceLeaseManager leaseManager,
								AstrixSettingsReader settings) {
		this.versioningContext = serviceVersioningContext;
		this.api = beanType;
		this.serviceLookup = serviceLookup;
		this.serviceComponents = serviceComponents;
		this.leaseManager = leaseManager;
		this.subsystem = settings.getString(AstrixSettings.SUBSYSTEM_NAME);
		this.enforceSubsystemBoundaries = settings.getBoolean(AstrixSettings.ENFORCE_SUBSYSTEM_BOUNDARIES, true);
	}

	@Override
	public T create(String qualifier) {
		AstrixServiceProperties serviceProperties = serviceLookup.lookup(api, qualifier);
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("No service-provider found in service-registry: api=%s qualifier=%s", api.getName(), qualifier));
		}
		String providerSubsystem = serviceProperties.getProperty(AstrixServiceProperties.SUBSYSTEM);
		if (!isAllowedToInvokeService(providerSubsystem)) {
			return createIllegalSubsystemProxy(providerSubsystem);
		}
		T service = create(qualifier, serviceProperties);
		return leaseManager.startManageLease(service, serviceProperties, qualifier, this, serviceLookup);
	}

	@Override
	public Class<T> getBeanType() {
		return api;
	}

	private boolean isAllowedToInvokeService(String providerSubsystem) {
		if (versioningContext.isVersioned()) {
			return true;
		}
		if (!enforceSubsystemBoundaries) {
			return true;
		}
		return this.subsystem.equals(providerSubsystem);
	}
	
	private T createIllegalSubsystemProxy(String providerSubsystem) {
		return api.cast(Proxy.newProxyInstance(api.getClassLoader(), new Class[]{api}, new IllegalSubsystemProxy(subsystem, providerSubsystem, api)));
	}

	public T create(String qualifier, AstrixServiceProperties serviceProperties) {
		if (serviceProperties == null) {
			throw new RuntimeException(String.format("Misssing entry in service-registry api=%s qualifier=%s: ", api.getName(), qualifier));
		}
		AstrixServiceComponent serviceComponent = getServiceComponent(serviceProperties);
		return serviceComponent.createService(versioningContext, api, serviceProperties);
	}
	
	private AstrixServiceComponent getServiceComponent(AstrixServiceProperties serviceProperties) {
		String componentName = serviceProperties.getComponent();
		if (componentName == null) {
			throw new IllegalArgumentException("Expected a componentName to be set on serviceProperties: " + serviceProperties);
		}
		return serviceComponents.getComponent(componentName);
	}
	
	private class IllegalSubsystemProxy implements InvocationHandler {
		
		private String currentSubsystem;
		private String providerSubsystem;
		private Class<?> beanType;
		

		public IllegalSubsystemProxy(String currentSubsystem, String providerSubsystem, Class<?> beanType) {
			this.currentSubsystem = currentSubsystem;
			this.providerSubsystem = providerSubsystem;
			this.beanType = beanType;
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
			throw new IllegalSubsystemException(currentSubsystem, providerSubsystem, beanType);
		}
	}
	
}
