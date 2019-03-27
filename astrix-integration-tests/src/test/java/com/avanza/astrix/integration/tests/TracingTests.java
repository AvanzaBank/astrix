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
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.astrix.test.util.AutoCloseableRule;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Properties;

import static com.avanza.astrix.integration.tests.TestLunchRestaurantBuilder.lunchRestaurant;

public class TracingTests {

    @ClassRule
    public static RunningPu serviceRegistrypu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
            .numberOfPrimaries(1)
            .numberOfBackups(0)
            .beanProperties("space", new Properties() {{
                // Run lease-manager thread every 200 ms.
                setProperty("space-config.lease_manager.expiration_time_interval", "200");
            }})
            .startAsync(true)
            .configure();

    private static MapConfigSource config = new MapConfigSource() {{
        set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
        set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
        set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 250);
    }};

    @ClassRule
    public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
            .numberOfPrimaries(1)
            .numberOfBackups(0)
            .contextProperty("configSourceId", GlobalConfigSourceRegistry.register(config))
            .startAsync(true)
            .configure();

    @ClassRule
    public static RunningPu lunchGraderPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
            .numberOfPrimaries(1)
            .numberOfBackups(0)
            .contextProperty("configSourceId", GlobalConfigSourceRegistry.register(config))
            .startAsync(true)
            .configure();

    @Rule
    public AutoCloseableRule autoClosables = new AutoCloseableRule();

    private AstrixContext astrix;

    @Before
    public void before() {
        AstrixConfigurer configurer = new AstrixConfigurer();
        configurer.enableFaultTolerance(true);
        configurer.set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 250);
        configurer.setConfig(DynamicConfig.create(config));
        configurer.setSubsystem("test-sub-system");
        astrix = autoClosables.add(configurer.configure());
    }

    @Test
    public void verifyTracing() throws InterruptedException {
        LunchService lunchService = astrix.waitForBean(LunchService.class, 5000);

        lunchService.addLunchRestaurant(lunchRestaurant().withName("Martins Green Room").build());
    }
}
