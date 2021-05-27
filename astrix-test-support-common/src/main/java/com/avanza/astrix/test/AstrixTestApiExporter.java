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
package com.avanza.astrix.test;

import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.TestApi.TestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class AstrixTestApiExporter {
	private static final Logger LOGGER = LoggerFactory.getLogger(AstrixTestApiExporter.class);

	private final TestContext testContext;

	public AstrixTestApiExporter(TestContext testContext) {
		this.testContext = Objects.requireNonNull(testContext);
	}

	public void registerAllProvidedServices(Class<?> astrixApiProvider, Object astrixObjectFactory) {
		Arrays.stream(astrixApiProvider.getMethods())
			  .filter(method -> method.isAnnotationPresent(Service.class))
			  .forEach(method -> register(method.getReturnType(), getQualifier(method), astrixObjectFactory));
	}

	private String getQualifier(Method method) {
		return Optional.ofNullable(method.getAnnotation(AstrixQualifier.class))
					   .map(AstrixQualifier::value)
					   .orElse(null);
	}

	public <T> void register(Class<T> serviceType, String qualifier, Object astrixObjectFactory) {
		T instance = createServiceFromFactoryMethod(serviceType, astrixObjectFactory);
		LOGGER.info("Registering " + serviceType.getSimpleName() + (qualifier != null ? "-" + qualifier : "") + " to " + instance);
		testContext.registerService(serviceType, qualifier, instance);
	}

	@SuppressWarnings("unchecked")
	private <T> T createServiceFromFactoryMethod(Class<T> serviceType, Object astrixObjectFactory) {
		Method method = Arrays.stream(astrixObjectFactory.getClass().getMethods())
							  .filter(candidateMethod -> serviceType.isAssignableFrom(candidateMethod.getReturnType()))
							  .findAny()
							  .orElseThrow(() -> new IllegalArgumentException("No factory method for " + serviceType + " in " + astrixObjectFactory.getClass()));
		try {
			return (T) method.invoke(astrixObjectFactory);
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
			throw new IllegalArgumentException("Illegal factory method for " + serviceType + " in " + astrixObjectFactory.getClass(), e);
		}
	}

}
