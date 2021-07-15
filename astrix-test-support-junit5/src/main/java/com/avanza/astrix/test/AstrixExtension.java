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

import static java.util.Objects.requireNonNull;

import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.function.Function;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ParameterContext;
import org.junit.jupiter.api.extension.ParameterResolutionException;
import org.junit.jupiter.api.extension.ParameterResolver;
import org.junit.platform.commons.support.AnnotationSupport;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.Astrix;

/**
 * Test utility that manages an internal service registry, config-source and AstrixContext. The
 * managed AstrixContext is configured to use the internal service registry.<p>
 * <p>
 * The AstrixTestContext allows registering providers programmatically in the
 * service-registry, see {@link AstrixTestContext#setProxyState(Class, Object)}.
 * <p>
 * Typical usage: <p>
 *
 * <pre>
 *
 * {@literal @}RegisterExtension
 * static AstrixExtension astrix = AstrixExtension.create();
 *
 * {@literal @}RegisterExtension
 * static RunningPu testedPu = ... // setup pu
 *
 *
 * {@literal @}Test
 * void aTest(AstrixTestContext astrixTestContext) {
 *     ...
 *     astrixTestContext.setProxyState(ConsumedService.class, serviceStub);
 *     ...
 *     ServiceUnderTest service = astrixTestContext.waitForBean(ServiceUnderTest.class, 2000);
 * }
 *
 * {@literal @}Test
 * void anotherTest(AstrixTestContext astrixTestContext) {
 *     ...
 *     astrixTestContext.setProxyState(ConsumedService.class, serviceStubWithDifferentBehavior);
 *     ...
 *     ServiceUnderTest service = astrixTestContext.waitForBean(ServiceUnderTest.class, 2000);
 * }
 *
 * </pre>
 */
public class AstrixExtension implements ParameterResolver, BeforeAllCallback, AfterEachCallback, AfterAllCallback {
    private static final Namespace ASTRIX_TEST_CONTEXT_NS = Namespace.create(AstrixExtension.class, AstrixTestContext.class);

    private final Function<ExtensionContext, Configuration> configurationFactory;

    private AstrixExtension(Function<ExtensionContext, Configuration> configurationFactory) {
        this.configurationFactory = requireNonNull(configurationFactory);
    }

    /**
     * Default constructor for @AstrixTest, @ExtendWith(AstrixExtension.class)
     */
    @SuppressWarnings("unused")
    AstrixExtension() {
        this(extensionContext -> findAnnotation(extensionContext, AstrixTest.class)
                .map(annotation -> new Configuration(annotation.resetAfterEach(), annotation.value()))
                .orElse(Configuration.DEFAULT));
    }

    @SafeVarargs
    public static AstrixExtension create(boolean resetAfterEach, Class<? extends TestApi>... testApis) {
        return new AstrixExtension(extensionContext -> new Configuration(resetAfterEach, testApis));
    }

    @SafeVarargs
    public static AstrixExtension create(Class<? extends TestApi>... testApis) {
        return create(true, testApis);
    }

    @Override
    public boolean supportsParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        return Astrix.class.isAssignableFrom(parameterType) || TestApi.class.isAssignableFrom(parameterType);
    }

    @Override
    public Object resolveParameter(ParameterContext parameterContext, ExtensionContext extensionContext) throws ParameterResolutionException {
        Class<?> parameterType = parameterContext.getParameter().getType();
        AstrixTestContext astrixTestContext = getAstrixTestContext(extensionContext);
        if (AstrixTestContext.class.isAssignableFrom(parameterType)) {
            return astrixTestContext;
        }
        if (Astrix.class.isAssignableFrom(parameterType)) {
            return astrixTestContext.getAstrixContext();
        }
        if (TestApi.class.isAssignableFrom(parameterType)) {
            try {
                @SuppressWarnings("unchecked")
                Class<? extends TestApi> testApiType = (Class<? extends TestApi>) parameterType;
                return astrixTestContext.getTestApi(testApiType);
            } catch (RuntimeException exception) {
                throw new ParameterResolutionException("Cannot resolve parameter " + parameterType + " " + parameterContext.getParameter().getName(), exception);
            }
        }
        throw new ParameterResolutionException("Cannot resolve parameter " + parameterType + " " + parameterContext.getParameter().getName());
    }

    @Override
    public void beforeAll(ExtensionContext context) {
        getAstrixTestContext(context);
    }

    @Override
    public void afterEach(ExtensionContext context) {
        if (getConfiguration(context).isResetAfterEach()) {
            getAstrixTestContext(context).resetTestApis();
        }
    }

    @Override
    public void afterAll(ExtensionContext context) {
        getAstrixTestContext(context).destroy();
    }

    public AstrixTestContext getAstrixTestContext(ExtensionContext context) {
        final MapConfigSource configSource = getMapConfigSource(context);
        return getStore(context).getOrComputeIfAbsent(context.getRequiredTestClass(),
                                                      key -> new AstrixTestContext(
                                                              configSource,
                                                              getConfiguration(context).getTestApis()
                                                      ),
                                                      AstrixTestContext.class);
    }

    private MapConfigSource getMapConfigSource(ExtensionContext context) {
        return getStore(context).getOrComputeIfAbsent(
                MapConfigSource.class.getName(),
                key -> new MapConfigSource(),
                MapConfigSource.class
        );
    }

    private Configuration getConfiguration(ExtensionContext context) {
        return configurationFactory.apply(context);
    }

    private static ExtensionContext.Store getStore(ExtensionContext context) {
        return context.getRoot().getStore(ASTRIX_TEST_CONTEXT_NS);
    }

    private static <T extends Annotation> Optional<T> findAnnotation(ExtensionContext context, Class<T> annotationType) {
        Optional<T> annotation = AnnotationSupport.findAnnotation(context.getElement(), annotationType);
        if (annotation.isPresent()) {
            return annotation;
        } else {
            return context.getParent().flatMap(parent -> findAnnotation(parent, annotationType));
        }
    }

    private static final class Configuration {
        private static final Configuration DEFAULT = new Configuration(true);
        private final boolean resetAfterEach;
        private final Class<? extends TestApi>[] testApis;

        @SafeVarargs
        public Configuration(boolean resetAfterEach, Class<? extends TestApi>... testApis) {
            this.resetAfterEach = resetAfterEach;
            this.testApis = testApis;
        }

        public boolean isResetAfterEach() {
            return resetAfterEach;
        }

        public Class<? extends TestApi>[] getTestApis() {
            return testApis;
        }
    }

}

