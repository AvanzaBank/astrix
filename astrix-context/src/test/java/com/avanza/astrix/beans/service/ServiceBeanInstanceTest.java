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
package com.avanza.astrix.beans.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.junit.Test;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.provider.versioning.ServiceVersioningContext;

public class ServiceBeanInstanceTest {
	
	static {
		BasicConfigurator.configure();
	}
	
	@Test
	public void boundServiceInstancesShouldBeReleaseWhenContextIsDestroyed() throws Exception {
		PingServiceComponent fakeServiceComponent = new PingServiceComponent();
		
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerPlugin(AstrixServiceComponent.class, fakeServiceComponent);
		astrixConfigurer.set("pingUri", fakeServiceComponent.getName() + ":");
		astrixConfigurer.registerApiProvider(PingApiProvider.class);
		AstrixContext astrixContext = astrixConfigurer.configure();
		
		Ping ping = astrixContext.getBean(Ping.class);
		assertEquals("foo", ping.ping("foo"));
		
		astrixContext.destroy();
		
		assertEquals(1, fakeServiceComponent.boundServiceInstances.size());
		assertTrue(fakeServiceComponent.boundServiceInstances.get(0).released);
	}
	
	static class FakeBoundServiceBeanInstance<T> implements BoundServiceBeanInstance<T> {
		private T instance;
		private boolean released = false;
		
		public FakeBoundServiceBeanInstance(T instance) {
			this.instance = instance;
		}

		@Override
		public T get() {
			return instance;
		}

		@Override
		public void release() {
			this.released = true;
		}
	}
	
	static class PingServiceComponent implements AstrixServiceComponent {
		
		private List<FakeBoundServiceBeanInstance<?>> boundServiceInstances = new ArrayList<>();
		
		@Override
		public <T> BoundServiceBeanInstance<T> bind(ServiceVersioningContext versioningContext, Class<T> type, AstrixServiceProperties serviceProperties) {
			FakeBoundServiceBeanInstance<T> result = new FakeBoundServiceBeanInstance<T>(type.cast(new PingImpl()));
			boundServiceInstances.add(result);
			return result;
		}

		@Override
		public AstrixServiceProperties createServiceProperties(String serviceUri) {
			return new AstrixServiceProperties();
		}

		@Override
		public <T> AstrixServiceProperties createServiceProperties(Class<T> exportedService) {
			return new AstrixServiceProperties();
		}

		@Override
		public String getName() {
			return "test";
		}

		@Override
		public <T> void exportService(Class<T> providedApi, T provider, ServiceVersioningContext versioningContext) {
		}

		@Override
		public boolean supportsAsyncApis() {
			return false;
		}

		@Override
		public boolean requiresProviderInstance() {
			return false;
		}
		
	}
	
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@AstrixConfigLookup("pingUri")
		@Service
		Ping ping();
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public static class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}

}
