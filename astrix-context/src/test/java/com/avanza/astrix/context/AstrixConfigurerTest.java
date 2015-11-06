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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.junit.Rule;
import org.junit.Test;

import com.avanza.astrix.beans.config.BeanConfiguration;
import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixBeanSettings.BooleanBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.IntBeanSetting;
import com.avanza.astrix.beans.core.AstrixBeanSettings.LongBeanSetting;
import com.avanza.astrix.beans.publish.ApiProviderClass;
import com.avanza.astrix.beans.publish.ApiProviders;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Service;
import com.avanza.astrix.test.util.AutoCloseableRule;

public class AstrixConfigurerTest {
	
	@Rule
	public AutoCloseableRule autoClosables = new AutoCloseableRule(); 
	
	@Test
	public void passesBeanSettingsToConfiguration() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(new ApiProviders() {
			@Override
			public Collection<ApiProviderClass> getAll() {
				return Collections.emptyList();
			}
		});
		IntBeanSetting intSetting = new IntBeanSetting("intSetting", 1);
		BooleanBeanSetting aBooleanSetting = new BooleanBeanSetting("booleanSetting", true);
		LongBeanSetting longSetting = new LongBeanSetting("longSetting", 2);
		
		configurer.set(aBooleanSetting, AstrixBeanKey.create(Ping.class), false);
		configurer.set(intSetting, AstrixBeanKey.create(Ping.class), 21);
		configurer.set(longSetting, AstrixBeanKey.create(Ping.class), 19);
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(Ping.class));
		
		assertEquals(21, pingConfig.get(intSetting).get());
		assertFalse(pingConfig.get(aBooleanSetting).get());
		assertEquals(19, pingConfig.get(longSetting).get());
	}
	
	@Test
	public void customDefaultBeanSettings() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(new ApiProviders() {
			@Override
			public Collection<ApiProviderClass> getAll() {
				return Arrays.asList(ApiProviderClass.create(PingApiProvider.class));
			}
		});
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(Ping.class));

		assertEquals(2000, pingConfig.get(AstrixBeanSettings.TIMEOUT).get());
		assertEquals(false, pingConfig.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED).get());
		assertEquals(false, pingConfig.get(AstrixBeanSettings.BEAN_METRICS_ENABLED).get());
		assertEquals(1, pingConfig.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS).get());
		assertEquals(2, pingConfig.get(AstrixBeanSettings.CORE_SIZE).get());
		assertEquals(3, pingConfig.get(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD).get());
	}
	
	@Test
	public void itsPossibleToOverrideCustomDefaultBeanSettingsOnBeanDefinition() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(new ApiProviders() {
			@Override
			public Collection<ApiProviderClass> getAll() {
				return Arrays.asList(ApiProviderClass.create(PingApiProviderWithOverridingDefault.class));
			}
		});
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(Ping.class));

		assertEquals(3000, pingConfig.get(AstrixBeanSettings.TIMEOUT).get());
		assertEquals(true, pingConfig.get(AstrixBeanSettings.FAULT_TOLERANCE_ENABLED).get());
		assertEquals(false, pingConfig.get(AstrixBeanSettings.BEAN_METRICS_ENABLED).get());
		assertEquals(2, pingConfig.get(AstrixBeanSettings.MAX_CONCURRENT_REQUESTS).get());
		assertEquals(5, pingConfig.get(AstrixBeanSettings.CORE_SIZE).get());
		assertEquals(6, pingConfig.get(AstrixBeanSettings.QUEUE_SIZE_REJECTION_THRESHOLD).get());
	}
	
	@Test
	public void customDefaultBeanSettingsAppliesToAsyncProxy() throws Exception {
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.setAstrixApiProviders(new ApiProviders() {
			@Override
			public Collection<ApiProviderClass> getAll() {
				return Arrays.asList(ApiProviderClass.create(PingApiProvider.class));
			}
		});
		
		AstrixContextImpl astrixContext = autoClosables.add((AstrixContextImpl) configurer.configure());
		BeanConfiguration pingConfig = astrixContext.getBeanConfiguration(AstrixBeanKey.create(PingAsync.class));

		assertEquals(2000, pingConfig.get(AstrixBeanSettings.TIMEOUT).get());
	}
	
	@DefaultBeanSettings(
		initialTimeout = 2000,
		faultToleranceEnabled = false,
		beanMetricsEnabled = false,
		initialMaxConcurrentRequests = 1,
		initialCoreSize = 2,
		initialQueueSizeRejectionThreshold = 3
	)
	public interface Ping {
	}
	
	public interface PingAsync {
	}
	
	@AstrixApiProvider
	public interface PingApiProvider {
		@Service
		Ping ping();
	}
	
	@AstrixApiProvider
	public interface PingApiProviderWithOverridingDefault {
		@DefaultBeanSettings(
			initialTimeout=3000,
			faultToleranceEnabled = true,
			beanMetricsEnabled = false,
			initialMaxConcurrentRequests = 2,
			initialCoreSize = 5,
			initialQueueSizeRejectionThreshold = 6
		)
		@Service
		Ping ping();
	}
	
	
}
