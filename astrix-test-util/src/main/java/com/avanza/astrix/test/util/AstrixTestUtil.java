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
package com.avanza.astrix.test.util;

import java.util.function.Supplier;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Assert;

public class AstrixTestUtil {
	
	public static <T> Probe serviceInvocationResult(final Supplier<T> serviceInvocation, final Matcher<T> matcher) {
		return new Probe() {
			
			private T lastResult;
			private Exception lastException;

			@Override
			public void sample() {
				try {
					this.lastResult = serviceInvocation.get();
				} catch (Exception e) {
					this.lastException = e;
				}
			}
			
			@Override
			public boolean isSatisfied() {
				return matcher.matches(lastResult);
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected serviceInovcation to return:\n");
				matcher.describeTo(description);
				if (lastException != null) {
					description.appendText("\nBut last serviceInvocation threw exception:\n" + lastException.toString());
				} else {
					description.appendText("\nBut last serviceInvocation returned:\n" + lastResult);
				}
			}
		};
	}
	
	public static <T extends Exception> Probe serviceInvocationException(final Supplier<?> serviceInvocation, final Matcher<T> matcher) {
		return new Probe() {
			
			private Object lastResult;
			private Exception lastException;

			@Override
			public void sample() {
				try {
					this.lastResult = serviceInvocation.get();
				} catch (Exception e) {
					this.lastException = e;
				}
			}
			
			@Override
			public boolean isSatisfied() {
				return matcher.matches(lastException);
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected serviceInovcation to throw exception: ");
				matcher.describeTo(description);
				if (lastException != null) {
					description.appendText("\nBut last service invocation threw exception: " + lastException.toString());
				} else {
					description.appendText("\nBut last service invocation returned " + lastResult);
				}
			}
		};
	}
	
	public static <T extends Exception> Probe isSuccessfulServiceInvocation(final Runnable serviceInvocation) {
		return new Probe() {
			
			private volatile Exception lastException;

			@Override
			public void sample() {
				try {
					serviceInvocation.run();
					this.lastException = null;
				} catch (Exception e) {
					this.lastException = e;
				}
			}
			
			@Override
			public boolean isSatisfied() {
				return this.lastException == null;
			}
			
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("Expected successful serviceInovcation, but last invocation threw exception: " + lastException.toString());
			}
		};
	}
	
	public static <T extends Exception> Matcher<T> isExceptionOfType(final Class<T> type) {
		return new TypeSafeMatcher<T>() {
			@Override
			public void describeTo(Description description) {
				description.appendText("Expected exception of type " + type.getName() + " to be thrown");
			}
			@Override
			protected boolean matchesSafely(T item) {
				return type.isAssignableFrom(item.getClass());
			}
		};
	}
	
	public static void closeSafe(AutoCloseable autoClosable) {
		if (autoClosable == null) {
			return;
		}
		try {
			autoClosable.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static void assertThrows(Runnable command, Class<? extends RuntimeException> expectedExceptionType) {
		try {
			command.run();
			Assert.fail("Expected exception of type " + expectedExceptionType.getName() + " to be thrown");
		} catch (RuntimeException e) {
			Assert.assertEquals("Thrown type: ", expectedExceptionType, e.getClass());
		}
	}

}
