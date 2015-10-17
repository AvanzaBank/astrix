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
package com.avanza.astrix.beans.api;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.beans.core.AstrixConfigAware;
import com.avanza.astrix.beans.core.ReactiveTypeConverter;
import com.avanza.astrix.beans.factory.StandardFactoryBean;
import com.avanza.astrix.beans.ft.BeanFaultToleranceFactory;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.BeanDefinitionMethod;
import com.avanza.astrix.beans.publish.BeanPublisherPlugin;
import com.avanza.astrix.beans.publish.LibraryBeanDefinition;
import com.avanza.astrix.beans.publish.ServiceBeanDefinition;
import com.avanza.astrix.beans.service.ServiceDefinition;
import com.avanza.astrix.beans.service.ServiceDefinitionSource;
import com.avanza.astrix.beans.service.ServiceDiscoveryDefinition;
import com.avanza.astrix.core.util.ReflectionUtil;
import com.avanza.astrix.modules.ObjectCache;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.versioning.core.AstrixObjectSerializerConfig;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
import com.avanza.astrix.versioning.core.Versioned;

import rx.Observable;

/**
 * 
 * @author Elias Lindholm
 *
 */
final class ApiProviderBeanPublisherPlugin implements BeanPublisherPlugin {

	private static final Logger log = LoggerFactory.getLogger(ApiProviderBeanPublisherPlugin.class);
	
	private final ObjectCache provideInstanceCache = new ObjectCache();
	private final BeanFaultToleranceFactory faultToleranceFactory;
	private final AstrixConfig config;
	private final ReactiveTypeConverter reactiveTypeConverter;
	
	public ApiProviderBeanPublisherPlugin(
			BeanFaultToleranceFactory faultToleranceFactory,
			AstrixConfig config,
			ReactiveTypeConverter reactiveTypeConverter) {
		this.faultToleranceFactory = faultToleranceFactory;
		this.config = config;
		this.reactiveTypeConverter = reactiveTypeConverter;
	}
	
	@Override
	public void publishBeans(BeanPublisher publisher, ApiProviderClass apiProviderClass) {
		// Create factory for each exported bean in api.
		for (Method astrixBeanDefinitionMethod : apiProviderClass.getProviderClass().getMethods()) {
			BeanDefinitionMethod<?> beanDefinitionMethod = BeanDefinitionMethod.create(astrixBeanDefinitionMethod);
			if (beanDefinitionMethod.isLibrary()) {
				StandardFactoryBean<?> factory = createLibraryFactory(apiProviderClass, astrixBeanDefinitionMethod, beanDefinitionMethod);
				publisher.publishLibrary(new LibraryBeanDefinition<>(factory, beanDefinitionMethod.getDefaultBeanSettings()));
				continue;
			}
			if (beanDefinitionMethod.isService()) {
				ServiceDefinition<?> serviceDefinition = createServiceDefinition(apiProviderClass, beanDefinitionMethod);
				ServiceDiscoveryDefinition serviceDiscoveryDefinition = new ServiceDiscoveryDefinition(beanDefinitionMethod.getServiceDiscoveryProperties(), serviceDefinition.getBeanKey());
				publisher.publishService(new ServiceBeanDefinition<>(beanDefinitionMethod.getDefaultBeanSettings(), serviceDefinition, serviceDiscoveryDefinition));
				Class<?> reactiveInterface = loadInterfaceIfExists(beanDefinitionMethod.getBeanType().getName() + "Async");
				if (reactiveInterface != null && isValidReactiveInterface(beanDefinitionMethod.getBeanType(), reactiveInterface)) {
					ServiceDefinition<?> asyncDefinition = serviceDefinition.asyncDefinition(reactiveInterface);
					publisher.publishService(new ServiceBeanDefinition<>(beanDefinitionMethod.getDefaultBeanSettings(), 
							asyncDefinition, 
							serviceDiscoveryDefinition)); // Use same discovery as for sync version
				}
				continue;
			}
		}
	}
	
	private boolean isValidReactiveInterface(Class<?> targetServiceApi, Class<?> reactiveInterface) {
		for (Method reactiveMethod : reactiveInterface.getMethods()) {
			if (!reactiveMethod.getReturnType().equals(Observable.class) && !this.reactiveTypeConverter.isReactiveType(reactiveMethod.getReturnType())) {
				log.warn("Found reactive interface that contains non-reactive methods and will therefore not be registered in the BeanFactory. nonReactiveReturnType={}, reactiveInterfaceCandidate={}, invalidMethod={}"
						 , reactiveMethod.getReturnType().getName(), reactiveInterface.getName(), reactiveMethod.getName());
				return false;
			}
			if (!hasMethod(targetServiceApi, reactiveMethod.getName(), reactiveMethod.getParameterTypes())) {
				log.warn("Found reactive interface that contains methods that does not correpond to a method in the synchronous interface. reactiveInterfaceCandidate={}, invalidMethod={}"
						, reactiveInterface.getName(), reactiveMethod.getName());
				return false;
			}
		}
		return true;
	}

	private boolean hasMethod(Class<?> targetServiceApi, String name, Class<?>[] parameterTypes) {
		try {
			targetServiceApi.getMethod(name, parameterTypes);
			return true;
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
			return false;
		}
	}

	private Class<?> loadInterfaceIfExists(String interfaceName) {
		try {
			Class<?> c = Class.forName(interfaceName);
			if (c.isInterface()) {
				return c;
			}
		} catch (ClassNotFoundException e) {
			// fall through and return null
		}
		return null;
	}

	private <T> StandardFactoryBean<T> createLibraryFactory(
			ApiProviderClass apiProviderClass,
			Method astrixBeanDefinitionMethod,
			BeanDefinitionMethod<T> beanDefinition) {
		Object libraryProviderInstance = getApiProviderInstance(apiProviderClass.getProviderClass());
		StandardFactoryBean<T> libraryFactory = new AstrixLibraryFactory<>(libraryProviderInstance, astrixBeanDefinitionMethod, beanDefinition.getQualifier());
		if (beanDefinition.applyFtProxy()) {
			libraryFactory = new AstrixFtProxiedFactory<T>(libraryFactory, faultToleranceFactory, beanDefinition, reactiveTypeConverter);
		}
		return libraryFactory;
	}
	
	private ServiceDefinition<?> createServiceDefinition(ApiProviderClass apiProviderClass, BeanDefinitionMethod<?> serviceDefinitionMethod) {
		Class<?> declaringApi = getDeclaringApi(apiProviderClass, serviceDefinitionMethod.getBeanType());
		if (!(declaringApi.isAnnotationPresent(Versioned.class) || serviceDefinitionMethod.isVersioned())) {
			return ServiceDefinition.create(ServiceDefinitionSource.create(serviceDefinitionMethod.getDefiningApi().getName()), serviceDefinitionMethod.getBeanKey(), 
								serviceDefinitionMethod.getServiceConfigClass(), 
								ObjectSerializerDefinition.nonVersioned(), 
								serviceDefinitionMethod.isDynamicQualified());
		}
		if (!apiProviderClass.isAnnotationPresent(AstrixObjectSerializerConfig.class)) {
			throw new IllegalArgumentException("Illegal api-provider. Api is versioned but provider does not declare a @AstrixObjectSerializerConfig." +
					" providedService=" + serviceDefinitionMethod.getBeanType().getName() + ", provider=" + apiProviderClass.getProviderClassName());
		} 
		AstrixObjectSerializerConfig serializerConfig = apiProviderClass.getAnnotation(AstrixObjectSerializerConfig.class);
		return ServiceDefinition.create(ServiceDefinitionSource.create(serviceDefinitionMethod.getDefiningApi().getName()),
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