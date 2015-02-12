/*
 * Copyright 2014-2015 Avanza Bank AB
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

import java.util.HashMap;
import java.util.Map;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

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
					description.appendText("\nBut last serviceInvocation threw exception: " + lastException.toString());
				} else {
					description.appendText("\nBut last serviceInvocation returnded " + lastResult);
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
				description.appendText("Expected " + type.getName() + " to be thrown");
			}
			@Override
			protected boolean matchesSafely(T item) {
				return true;
			}
		};
	}
	
	public static class MapBuilder {
		private HashMap<String, Integer> result = new HashMap<>();
		public MapBuilder() { }
		public MapBuilder(String key, Integer value) { 
			with(key, value);
		}
		public MapBuilder with(String key, Integer value) {
			result.put(key, value);
			return this;
		}
		public Map<String, Integer> build() {
			return result;
		}
	}

}
