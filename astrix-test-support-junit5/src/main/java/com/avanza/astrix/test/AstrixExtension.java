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

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

import java.util.function.Consumer;

/**
 * Test utility that manages an internal service registry, config-source and AstrixContext. The
 * managed AstrixContext is configured to use the internal service registry.<p>
 * <p>
 * The AstrixRule allows registering providers programmatically in the
 * service-registry, see {@link AstrixExtension#setProxyState(Class, Object)}.
 * <p>
 * Typical usage: <p>
 *
 * <pre>
 *
 * {@literal @}RegisterExtension
 * static AstrixExtension astrix = new AstrixExtension();
 *
 * {@literal @}RegisterExtension
 * static RunningPu testedPu = ... // setup pu
 *
 *
 * {@literal @}Test
 * void aTest() {
 *     ...
 *     astrix.registerProvider(ConsumedService.class, serviceStub);
 *     ...
 *     ServiceUnderTest service = astrix.waitForBean(ServiceUnderTest.class, 2000);
 * }
 *
 * {@literal @}Test
 * void anotherTest() {
 *     ...
 *     astrix.registerProvider(ConsumedService.class, serviceStubWithDifferentBehavior);
 *     ...
 *     ServiceUnderTest service = astrix.waitForBean(ServiceUnderTest.class, 2000);
 * }
 *
 * </pre>
 *
 */
public class AstrixExtension extends CommonAstrixTestSupport implements AfterAllCallback {

    @SafeVarargs
    public AstrixExtension(Class<? extends TestApi>... testApis) {
        super(testApis);
    }

    @SafeVarargs
    public AstrixExtension(Consumer<? super AstrixRuleContext> contextConfigurer, Class<? extends TestApi>... testApis) {
        super(contextConfigurer, testApis);
    }

    @Override
    public void afterAll(ExtensionContext context) {
        try {
            destroy();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

