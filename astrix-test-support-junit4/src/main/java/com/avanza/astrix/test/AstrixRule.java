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

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.function.Consumer;

/**
 * Test utility that manages an internal service registry, config-source and AstrixContext. The
 * managed AstrixContext is configured to use the internal service registry.<p>
 * <p>
 * The AstrixRule allows registering providers programmatically in the
 * service-registry, see {@link AstrixRule#setProxyState(Class, Object)}.
 * <p>
 * Typical usage: <p>
 *
 * <pre>
 *
 * {@literal @}ClassRule
 * public static AstrixRule astrix = new AstrixRule();
 *
 * {@literal @}ClassRule
 * public static RunningPu testedPu = ... // setup pu
 *
 *
 * {@literal @}Test
 * public void aTest() {
 *     ...
 *     astrix.registerProvider(ConsumedService.class, serviceStub);
 *     ...
 *     ServiceUnderTest service = astrix.waitForBean(ServiceUnderTest.class, 2000);
 * }
 *
 * {@literal @}Test
 * public void anotherTest() {
 *     ...
 *     astrix.registerProvider(ConsumedService.class, serviceStubWithDifferentBehavior);
 *     ...
 *     ServiceUnderTest service = astrix.waitForBean(ServiceUnderTest.class, 2000);
 * }
 *
 * </pre>
 *
 * @author Elias Lindholm
 */
public class AstrixRule extends CommonAstrixTestSupport implements TestRule {

    @SafeVarargs
    public AstrixRule(Class<? extends TestApi>... testApis) {
        super(testApis);
    }

    @SafeVarargs
    public AstrixRule(Consumer<? super AstrixRuleContext> contextConfigurer, Class<? extends TestApi>... testApis) {
        super(contextConfigurer, testApis);
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {

        	@Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } finally {
                    try {
                        destroy();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

        };
    }

}

