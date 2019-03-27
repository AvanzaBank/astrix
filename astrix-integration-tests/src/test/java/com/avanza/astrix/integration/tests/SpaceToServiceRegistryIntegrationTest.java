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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.ServiceRegistryClient;
import com.avanza.astrix.beans.service.ServiceProperties;
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
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Ignore
public class SpaceToServiceRegistryIntegrationTest {

    @ClassRule
    public static RunningPu serviceRegistrypu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/service-registry-pu.xml")
            .numberOfPrimaries(1)
            .numberOfBackups(0)
            .startAsync(false)
            .configure();

    private static MapConfigSource clientConfig = new MapConfigSource() {{
        set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
    }};

    private static MapConfigSource config = new MapConfigSource() {{
        set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=" + serviceRegistrypu.getLookupGroupName());
        set(AstrixSettings.SERVICE_REGISTRY_EXPORT_RETRY_INTERVAL, 250);
        set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 100);
    }};

    @ClassRule
    public static RunningPu lunchPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
            .numberOfPrimaries(1)
            .numberOfBackups(0)
            .contextProperty("configSourceId", GlobalConfigSourceRegistry.register(config))
            .startAsync(false)
            .configure();

    @Rule
    public AutoCloseableRule autoCloseableRule = new AutoCloseableRule();

    private AstrixContext clientContext;

    @Before
    public void setup() throws Exception {
        this.clientContext = autoCloseableRule.add(new AstrixConfigurer().setConfig(DynamicConfig.create(clientConfig)).configure());
    }

    @Test
    public void name() throws InterruptedException {
        ServiceRegistryClient serviceRegistryClient = clientContext.getBean(ServiceRegistryClient.class);
        List<ServiceProperties> providers = serviceRegistryClient.list(AstrixBeanKey.create(LunchService.class));

        System.out.println(providers);

        Thread.sleep(TimeUnit.MINUTES.toMillis(2));
    }
}
