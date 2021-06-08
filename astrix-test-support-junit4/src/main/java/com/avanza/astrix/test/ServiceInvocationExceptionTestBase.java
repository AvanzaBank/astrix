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

import com.avanza.astrix.core.ServiceInvocationException;
import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.ReflectionsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

/**
 * Extend this test-class in order to test that your RuntimeExceptions are Astrix ready.
 */
public abstract class ServiceInvocationExceptionTestBase {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Reflections reflections;

    protected ServiceInvocationExceptionTestBase(String packageName) {
        this.reflections = new Reflections(packageName);
    }

    @Test
    public void exceptionsExtendsServiceInvocationException() {
        getSubTypesOf(RuntimeException.class)
                .forEach(this::assertIsServiceInvocationException);
    }

    @Test
    public void subtypesOfServiceInvocationExceptionImplementRecreateOnClientSideMethod() throws Exception {
        Set<Class<? extends ServiceInvocationException>> exceptions = getSubTypesOf(ServiceInvocationException.class);

        for (Class<? extends RuntimeException> exceptionClass : exceptions) {
            Method method = exceptionClass.getDeclaredMethod("recreateOnClientSide");
            Class<?> declaringClass = method.getDeclaringClass();

            assertEquals("Exception class must implement 'recreateOnClientSide' self.", declaringClass, exceptionClass);
        }
    }

    private <T> Set<Class<? extends T>> getSubTypesOf(Class<T> type) {
        try {
            return reflections.getSubTypesOf(type);
        } catch (ReflectionsException exception) {
            // Reflections (0.9.12) throws ReflectionsException if nothing found in package
            log.trace("Could not retrieve subtypes of {}", type, exception);
            return emptySet();
        }
    }

    private void assertIsServiceInvocationException(Class<? extends RuntimeException> exceptionClass) {
        assertThat(ServiceInvocationException.class.isAssignableFrom(exceptionClass), is(true));
    }

}
