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
package com.avanza.astrix.integration.tests;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.integration.tests.domain.api.GetLunchRestaurantRequest;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.api.LunchServiceAsync;
import com.avanza.astrix.test.util.AutoCloseableExtension;
import com.avanza.gs.test.junit5.PuConfigurers;
import com.avanza.gs.test.junit5.RunningPu;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.openspaces.core.GigaSpace;

import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static com.avanza.astrix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
class AsyncRemoteServiceTest {

	private static final InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry();
	
	private static final MapConfigSource config = new MapConfigSource() {{
		set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
		set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
	}};
	
	@RegisterExtension
	static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
												   .numberOfPrimaries(1)
												   .numberOfBackups(0)
												   .contextProperty("configSourceId", GlobalConfigSourceRegistry.register(config))
												   .startAsync(false)
												   .configure();
	
	@RegisterExtension
	AutoCloseableExtension autoClosables = new AutoCloseableExtension();

	private LunchServiceAsync lunchServiceAsync;
	private LunchService lunchService;
	private AstrixContext astrix;

	@BeforeEach
	void setup() throws Exception {
		GigaSpace proxy = lunchPu.getClusteredGigaSpace();
		proxy.clear(null);
		
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.enableFaultTolerance(true);
		configurer.set(AstrixSettings.ENABLE_BEAN_METRICS, true);
		configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
		configurer.setConfig(DynamicConfig.create(config));
		configurer.setSubsystem("test-sub-system");
		this.astrix = autoClosables.add(configurer.configure());
		this.lunchServiceAsync = astrix.waitForBean(LunchServiceAsync.class, 10000);
		this.lunchService = astrix.waitForBean(LunchService.class, 10000);
	}
	
	/* NOTE: Since Astrix converts between Future/Observable internally its easy
	 * to run into a state where asynchronous calls becomes effectively synchronous.
	 * 
	 * The tests in this class are to ensure an asynchronous model for gs-remoting, as
	 * well as the full stack with proxies on each service-bean. 
	 */
	
	@Test
	@Timeout(1)
	void serviceInvocationsReturningFutureDoesRunAsynchronously() throws Exception {
		GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
		request.setName("Martins Green Room");
		
		Future<LunchRestaurant> lunchRestaurant = lunchServiceAsync.waitForLunchRestaurant("Martins Green Room", 1, TimeUnit.SECONDS);
		lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		assertNotNull(lunchRestaurant.get());
	}
	
	@Test
	@Timeout(1)
	void asyncInvocationsAreStartedAsynchronouslynServiceInvocation() throws Exception {
		GetLunchRestaurantRequest request = new GetLunchRestaurantRequest();
		request.setName("Martins Green Room");
		
		Future<LunchRestaurant> lunchRestaurant = lunchServiceAsync.waitForLunchRestaurant("Martins Green Room", 1, TimeUnit.SECONDS);
		
		// By adding the LunchRestaurant asynchronously and not invoke Future.get we ensure that the underlying 
		// service invocation is really started
		Future<Void> addLunchRestaurant = lunchServiceAsync.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
		
		assertNotNull(lunchRestaurant.get());
	}
	
}
