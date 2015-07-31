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
package com.avanza.astrix.context;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import javax.annotation.PreDestroy;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.factory.ObjectCache;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviderPlugin;
import com.avanza.astrix.beans.publish.PublishedBean;
import com.avanza.astrix.beans.service.ObjectSerializerDefinition;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDiscoveryFactory;
import com.avanza.astrix.beans.service.ServiceDiscoveryMetaFactory;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.ft.BeanFaultToleranceProxyStrategy;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.AstrixObjectSerializerConfig;
import com.avanza.astrix.provider.versioning.Versioned;

/**
 * 
 * @author Elias Lindholm
 *
 */
public final class GenericAstrixApiProviderPlugin implements ApiProviderPlugin {

	// TODO: Rename this abstractions: Its not a plugin anymore
	
	private final ObjectCache provideInstanceCache = new ObjectCache();
	private final AstrixServiceMetaFactory serviceMetaFactory;
	private final ServiceDiscoveryMetaFactory serviceDiscoveryMetaFactory;
	private final BeanFaultToleranceProxyStrategy faultToleranceFactory;
	private final AstrixConfig config;
	
	public GenericAstrixApiProviderPlugin(
			AstrixServiceMetaFactory serviceMetaFactory,
			ServiceDiscoveryMetaFactory serviceDiscoveryMetaFactory,
			BeanFaultToleranceProxyStrategy faultToleranceFactory,
			AstrixConfig config) {
		this.serviceMetaFactory = serviceMetaFactory;
		this.serviceDiscoveryMetaFactory = serviceDiscoveryMetaFactory;
		this.faultToleranceFactory = faultToleranceFactory;
		this.config = config;
	}

	@Override
	public List<PublishedBean> createFactoryBeans(ApiProviderClass apiProviderClass) {
		List<PublishedBean> result = new ArrayList<>();
		// Create factory for each exported bean in api.
		for (Method astrixBeanDefinitionMethod : apiProviderClass.getProviderClass().getMethods()) {
			AstrixBeanDefinitionMethod<?> beanDefinition = AstrixBeanDefinitionMethod.create(astrixBeanDefinitionMethod);
			if (beanDefinition.isLibrary()) {
				result.add(new PublishedBean(createLibraryFactory(apiProviderClass, astrixBeanDefinitionMethod, beanDefinition), beanDefinition.getDefaultBeanSettings()));
				continue;
			}
			if (beanDefinition.isService()) {
				ServiceDefinition<?> serviceDefinition = createServiceDefinition(apiProviderClass, beanDefinition);
				ServiceDiscoveryFactory<?> serviceDiscoveryFactory = serviceDiscoveryMetaFactory.createServiceDiscoveryFactory(
						beanDefinition.getBeanKey().getBeanType(), astrixBeanDefinitionMethod);
				result.add(new PublishedBean(serviceMetaFactory.createServiceFactory(serviceDefinition, serviceDiscoveryFactory), beanDefinition.getDefaultBeanSettings()));
				Class<?> asyncInterface = serviceMetaFactory.loadInterfaceIfExists(beanDefinition.getBeanType().getName() + "Async");
				if (asyncInterface != null) {
					result.add(new PublishedBean(serviceMetaFactory.createServiceFactory(serviceDefinition.asyncDefinition(asyncInterface), serviceDiscoveryFactory), beanDefinition.getDefaultBeanSettings()));
				}
				continue;
			}
		}
		return result;
	}

	private <T> StandardFactoryBean<T> createLibraryFactory(
			ApiProviderClass apiProviderClass,
			Method astrixBeanDefinitionMethod,
			AstrixBeanDefinitionMethod<T> beanDefinition) {
		Object libraryProviderInstance = getApiProviderInstance(apiProviderClass.getProviderClass());
		StandardFactoryBean<T> libraryFactory = new AstrixLibraryFactory<>(libraryProviderInstance, astrixBeanDefinitionMethod, beanDefinition.getQualifier());
		if (beanDefinition.applyFtProxy()) {
			libraryFactory = new AstrixFtProxiedFactory<T>(libraryFactory, faultToleranceFactory, beanDefinition);
		}
		return libraryFactory;
	}
	
	private ServiceDefinition<?> createServiceDefinition(ApiProviderClass apiProviderClass, AstrixBeanDefinitionMethod<?> serviceDefinitionMethod) {
		Class<?> declaringApi = getDeclaringApi(apiProviderClass, serviceDefinitionMethod.getBeanType());
		if (!(declaringApi.isAnnotationPresent(Versioned.class) || serviceDefinitionMethod.isVersioned())) {
			return ServiceDefinition.create(serviceDefinitionMethod.getDefiningApi(), serviceDefinitionMethod.getBeanKey(), 
								serviceDefinitionMethod.getServiceConfigClass(), 
								ObjectSerializerDefinition.nonVersioned(), 
								serviceDefinitionMethod.isDynamicQualified());
		}
		if (!apiProviderClass.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + serviceDefinitionMethod.getBeanType().getName() + ", provider=" + apiProviderClass.getProviderClassName());
		} 
		AstrixObjectSerializerConfig serializerConfig = apiProviderClass.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceDefinition.create(serviceDefinitionMethod.getDefiningApi(),
										serviceDefinitionMethod.getBeanKey(), 
										serviceDefinitionMethod.getServiceConfigClass(), 
									    ObjectSerializerDefinition.versionedService(serializerConfig.version(), 
									    serializerConfig.objectSerializerConfigurer()), 
									    serviceDefinitionMethod.isDynamicQualified());
	}

	private Class<?> getDeclaringApi(ApiProviderClass apiProviderClass, Class<?> providedService) {
		for (Method m : apiProviderClass.getProviderClass().getMethods()) {
			if (m.isAnnotationPresent(Service.class) && m.getReturnType().equals(providedService)) {
				return apiProviderClass.getProviderClass();
			}
		}
		throw new IllegalArgumentException(String.format("ApiProvider does not provide service. providerClass=%s service=%s", apiProviderClass, providedService.getName()));
	}

	private Object getApiProviderInstance(final Class<?> provider) {
		return provideInstanceCache.getInstance(provider, new ObjectCache.ObjectFactory<Object>() {
			@Override
			public Object create() throws Exception {
				Object instance = ReflectionUtil.newInstance(provider);
				if (instance instanceof AstrixConfigAware) {
					AstrixConfigAware.class.cast(instance).setConfig(config.getConfig());
				}
				return instance;
			}
		});
	}

	@Override
	public Class<? extends Annotation> getProviderAnnotationType() {
		return AstrixApiProvider.class;
	}
	
	@PreDestroy
	public void destroy() {
		this.provideInstanceCache.destroy();
	}
	
}