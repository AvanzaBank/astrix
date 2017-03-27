package com.avanza.astrix.test;

import com.avanza.astrix.core.ServiceInvocationException;
import org.junit.Test;
import org.reflections.Reflections;

import java.lang.reflect.Method;
import java.util.Set;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

/**
 * Extend this test-class in order to test that your RuntimeExceptions are Astrix ready.
 */
public abstract class ServiceInvocationExceptionTestBase {

	private final Reflections reflections;

	protected ServiceInvocationExceptionTestBase(String packageName) {
		this.reflections = new Reflections(packageName);
	}

	@Test
	public void exceptionsExtendsServiceInvocationException() {
		reflections.getSubTypesOf(RuntimeException.class)
				.forEach(this::assertIsServiceInvocationException);
	}

	private void assertIsServiceInvocationException(Class<? extends RuntimeException> exceptionClass) {
		assertThat(exceptionClass, instanceOf(ServiceInvocationException.class));
	}

	@Test
	public void subtypesOfServiceInvocationExceptionImplementRecreateOnClientSideMethod() throws Exception {
		Set<Class<? extends ServiceInvocationException>> exceptions = reflections.getSubTypesOf(ServiceInvocationException.class);

		for (Class<? extends RuntimeException> exceptionClass : exceptions) {
			Method method = exceptionClass.getDeclaredMethod("recreateOnClientSide");
			Class<?> declaringClass = method.getDeclaringClass();

			assertEquals("Exception class must implement 'recreateOnClientSide' self.", declaringClass, exceptionClass);
		}
	}
}
