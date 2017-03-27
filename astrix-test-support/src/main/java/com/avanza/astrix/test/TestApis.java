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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.test.TestApi.TestContext;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

final class TestApis {

	private final AstrixTestContext astrixTestContext;
	private final Map<Class<?>, TestApi> testApiByType = new ConcurrentHashMap<>();
	private final Object testApiLock = new Object();
	private final List<AstrixBeanKey<?>> exportedServices = new CopyOnWriteArrayList<>();

	public TestApis(AstrixTestContext astrixTestContext, Class<?>... testApis) {
		this.astrixTestContext = astrixTestContext;
		loadRecursive(Arrays.stream(testApis).map(TestApis::castToTestApi));
	}

	private void loadRecursive(Stream<Class<? extends TestApi>> testApis) {
		testApis.forEach(this::ensureLoaded);
	}

	private void ensureLoaded(Class<? extends TestApi> testApi) {
		// Do not attempt testApiByRule.computeIfAbsent here! ConcurrentHashMap does not like reentrant calls!
		// From ConcurrentHashMap#computeIfAbsent:
		// 	"Some attempted update operations on this map by other threads may be blocked while computation
		//   is in progress, so the computation should be short and simple, and must not attempt to update any other mappings of this map"
		synchronized (testApiLock) {
			if (!testApiByType.containsKey(testApi)) {
				testApiByType.put(testApi, loadTestApi(testApi));
			}
		}
	}

	private TestApi loadTestApi(Class<?> testApiType) {
		TestApi testApi = initTestApi(testApiType);
		loadRecursive(testApi.getDependencies());
		testApi.exportServices(new TestApiContext());
		return testApi;
	}

	private TestApi initTestApi(Class<?> testApiType) {
		try {
			return castToTestApi(testApiType).newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to instantiate TestApi: " + testApiType.getName(), e);
		}
	}

	@SuppressWarnings("unchecked")
	private static Class<? extends TestApi> castToTestApi(Class<?> api) {
		if (!TestApi.class.isAssignableFrom(api)) {
			throw new IllegalArgumentException("Not a testapi: " + api);
		}
		return (Class<? extends TestApi>) api;
	}

	<T extends TestApi> T getTestApi(Class<T> testApi) {
		return Optional.ofNullable(testApiByType.get(testApi))
					   .map(testApi::cast)
					   .orElseThrow(() -> new IllegalStateException("No TestApi registered for: " + testApi.getName()));
	}

	void reset() {
		exportedServices.forEach(key -> astrixTestContext.setProxyState(key.getBeanType(), key.getQualifier(), null));
		exportedServices.clear();
		testApiByType.forEach((key, testApi) -> testApiByType.put(key, loadTestApi(key)));
	}

	private class TestApiContext implements TestContext {
		@Override
		public <T> void registerService(Class<T> service, T serviceImpl) {
			registerService(service, null, serviceImpl);
		}

		@Override
		public <T> void registerService(Class<T> service, String qualifier, T serviceImpl) {
			exportedServices.add(AstrixBeanKey.create(service, qualifier));
			astrixTestContext.setProxyState(service, qualifier, serviceImpl);
		}

		@Override
		public <T> T getBean(Class<T> serviceBean) {
			return astrixTestContext.getBean(serviceBean);
		}

		@Override
		public <T> T getBean(Class<T> beanType, String qualifier) {
			return astrixTestContext.getBean(beanType, qualifier);
		}

		@Override
		public <T extends TestApi> T getTestApi(Class<T> testApi) {
			return TestApis.this.getTestApi(testApi);
		}
	}

}
