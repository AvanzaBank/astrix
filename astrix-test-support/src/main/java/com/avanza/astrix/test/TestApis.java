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
import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

final class TestApis {

	private final AstrixTestContext astrixTestContext;
	private final List<Class<? extends TestApi>> testApis;
	private final ConcurrentMap<Class<? extends TestApi>, TestApi> testApiByType = new ConcurrentHashMap<>();
	private final Object testApiLock = new Object();
	private final Queue<AstrixBeanKey<?>> exportedServices = new ConcurrentLinkedQueue<>();

	@SafeVarargs
	public TestApis(AstrixTestContext astrixTestContext, Class<? extends TestApi>... testApis) {
		this.astrixTestContext = astrixTestContext;
		this.testApis = Arrays.asList(testApis);
		this.testApis.forEach(this::ensureLoaded);
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

	private TestApi loadTestApi(Class<? extends TestApi> testApiType) {
		TestApi testApi = initTestApi(testApiType);
		testApi.getTestApiDependencies().forEach(this::ensureLoaded);
		testApi.exportServices(new TestApiContext());
		return testApi;
	}

	private TestApi initTestApi(Class<? extends TestApi> testApiType) {
		try {
			return testApiType.newInstance();
		} catch (InstantiationException | IllegalAccessException e) {
			throw new RuntimeException("Failed to instantiate TestApi: " + testApiType.getName(), e);
		}
	}

	<T extends TestApi> T getTestApi(Class<T> testApi) {
		return Optional.ofNullable(testApiByType.get(testApi))
				.map(testApi::cast)
				.orElseThrow(() -> new IllegalStateException("No TestApi registered for: " + testApi.getName()));
	}

	void reset() {
		exportedServices.forEach(key -> astrixTestContext.setProxyState(key.getBeanType(), key.getQualifier(), null));
		exportedServices.clear();
		testApiByType.clear();
		testApis.forEach(this::ensureLoaded);
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
			// TODO: If needed, then create a distinct AstrixContext
			// instance for each TestApiContext instance.
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
