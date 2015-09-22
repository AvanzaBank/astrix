/*
 * Copyright 2014 Avanza Bank AB
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
package com.avanza.astrix.beans.publish;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import com.avanza.astrix.beans.configdiscovery.ConfigDiscoveryProperties;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BeanSetting;
import com.avanza.astrix.beans.registry.ServiceRegistryDiscoveryProperties;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.provider.core.AstrixConfigDiscovery;
import com.avanza.astrix.provider.core.AstrixDynamicQualifier;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Library;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.core.ServiceConfig;
import com.avanza.astrix.versioning.core.Versioned;

public class BeanDefinitionMethod<T> implements PublishedAstrixBean<T> {
	
	private final Method method;
	
	private BeanDefinitionMethod(Method method) {
		this.method = method;
	}

	public boolean isLibrary() {
		return method.isAnnotationPresent(Library.class);
	}

	public AstrixBeanKey<T> getBeanKey() {
		return AstrixBeanKey.create(getBeanType(), getQualifier());
	}

	public String getQualifier() {
		if (method.isAnnotationPresent(AstrixQualifier.class)) {
			return method.getAnnotation(AstrixQualifier.class).value();
		}
		return null;
	}
	
	public Method getMethod() {
		return method;
	}
	
	public boolean isDynamicQualified() {
		return method.isAnnotationPresent(AstrixDynamicQualifier.class);
	}

	public boolean isService() {
		return method.isAnnotationPresent(Service.class);
	}
	
	public String getServiceComponentName() {
		if (!method.getAnnotation(Service.class).value().isEmpty()) {
			return method.getAnnotation(Service.class).value();
		}
		return null;
	}

	public boolean isVersioned() {
		return method.isAnnotationPresent(Versioned.class);
	}

	public static BeanDefinitionMethod<?> create(Method astrixBeanDefinition) {
		return new BeanDefinitionMethod<>(astrixBeanDefinition);
	}

	@SuppressWarnings("unchecked")
	public Class<T> getBeanType() {
		return (Class<T>) method.getReturnType();
	}
	
	
	// TODO: the definition of usesServiceRegistry and usesConfigDiscovery here is a temporal step
	// in refactoring towards a step where an ApiProviderPlugin only defines what an api looks like 
	public boolean usesServiceRegistry() {
		return isService() && !usesConfigDiscovery();
	}
	
	public boolean usesConfigDiscovery() {
		return method.isAnnotationPresent(AstrixConfigDiscovery.class);
	}
	
	public Object getServiceDiscoveryProperties() {
		if (usesServiceRegistry()) {
			return ServiceRegistryDiscoveryProperties.get();
		}
		if (usesConfigDiscovery()) {
			return new ConfigDiscoveryProperties(method.getAnnotation(AstrixConfigDiscovery.class).value());
		}
		throw new IllegalStateException("Bean does not define service discovery");
	}
	
	/**
	 * If the underlying element is annotated with @ServiceConfig, then this method returns
	 * the value of the @ServiceConfig annotation. In all other cases this method returns null.
	 * @return
	 */
	public Class<?> getServiceConfigClass() {
		if (method.isAnnotationPresent(ServiceConfig.class)) {
			return method.getAnnotation(ServiceConfig.class).value();
		}
		return null;
	}

	/**
	 * If true, then a fault tolerance proxy should be applies to a library bean. 
	 * @return
	 */
	public boolean applyFtProxy() {
		return method.isAnnotationPresent(AstrixFaultToleranceProxy.class);
	}
	
	@Override
	public ApiProvider getDefiningApi() {
		return ApiProvider.create(this.method.getDeclaringClass().getName());
	}
	
	public Map<BeanSetting<?>, Object> getDefaultBeanSettings() {
		Map<BeanSetting<?>, Object> defaultSettings = new HashMap<>();
		if (getBeanType().isAnnotationPresent(DefaultBeanSettings.class)) {
			// Default settings defined on service api
			DefaultBeanSettings defaultBeanSettingsOnApi = getBeanType().getAnnotation(DefaultBeanSettings.class);
			defaultSettings.put(AstrixBeanSettings.INITIAL_TIMEOUT, defaultBeanSettingsOnApi.initialTimeout());
			defaultSettings.put(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED, defaultBeanSettingsOnApi.faultToleranceEnabled());
			defaultSettings.put(AstrixBeanSettings.BEAN_METRICS_ENABLED, defaultBeanSettingsOnApi.beanMetricsEnabled());
			defaultSettings.put(AstrixBeanSettings.INITIAL_MAX_CONCURRENT_REQUESTS, defaultBeanSettingsOnApi.initialMaxConcurrentRequests());
			defaultSettings.put(AstrixBeanSettings.INITIAL_CORE_SIZE, defaultBeanSettingsOnApi.initialCoreSize());
			defaultSettings.put(AstrixBeanSettings.INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD, defaultBeanSettingsOnApi.initialQueueSizeRejectionThreshold());
		}
		if (this.method.isAnnotationPresent(DefaultBeanSettings.class)) {
			// Default settings defined on service definition
			DefaultBeanSettings defaultBeanSettingsInDefinition = this.method.getAnnotation(DefaultBeanSettings.class);
			defaultSettings.put(AstrixBeanSettings.INITIAL_TIMEOUT, defaultBeanSettingsInDefinition.initialTimeout());
			defaultSettings.put(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED, defaultBeanSettingsInDefinition.faultToleranceEnabled());
			defaultSettings.put(AstrixBeanSettings.BEAN_METRICS_ENABLED, defaultBeanSettingsInDefinition.beanMetricsEnabled());
			defaultSettings.put(AstrixBeanSettings.INITIAL_MAX_CONCURRENT_REQUESTS, defaultBeanSettingsInDefinition.initialMaxConcurrentRequests());
			defaultSettings.put(AstrixBeanSettings.INITIAL_CORE_SIZE, defaultBeanSettingsInDefinition.initialCoreSize());
			defaultSettings.put(AstrixBeanSettings.INITIAL_QUEUE_SIZE_REJECTION_THRESHOLD, defaultBeanSettingsInDefinition.initialQueueSizeRejectionThreshold());
		}
		return defaultSettings;
	}

}