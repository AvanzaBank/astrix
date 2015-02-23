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
package com.avanza.astrix.remoting.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.lang.reflect.Method;

import org.junit.Test;

import com.avanza.astrix.core.AstrixRouting;
import com.avanza.astrix.remoting.util.RoutingKeyMethodScanner;



public class RoutingKeyMethodScannerTest {
	
	@Test
	public void findsNoArgumentMethodWithAstrixRoutingAnnotation() throws Exception {
		class Test {
			@AstrixRouting
			public String routingMethod() {
				return null;
			}
			@SuppressWarnings("unused")
			public String anotherMethod() {
				return null;
			}
		}
		RoutingKeyMethodScanner scanner = new RoutingKeyMethodScanner();
		Method routingKeyMethod = scanner.getRoutingKeyMethod(AstrixRouting.class, Test.class);
		assertEquals(Test.class.getMethod("routingMethod"), routingKeyMethod);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void throwsIllegalArgumentExceptionForTypeWithMultipleAstrixRoutingAnnotations() throws Exception {
		class Test {
			@AstrixRouting
			public String routingMethod() {
				return null;
			}

			@AstrixRouting
			public String anotherMethod() {
				return null;
			}
		}
		RoutingKeyMethodScanner scanner = new RoutingKeyMethodScanner();
		scanner.getRoutingKeyMethod(AstrixRouting.class, Test.class);
	}
	
	@Test
	public void returnsNullIfNoMethodIsMethodHasAstrixRoutingAnnotating() throws Exception {
		class Test {
			@SuppressWarnings("unused")
			public String routingMethod() {
				return null;
			}
			@SuppressWarnings("unused")
			public String anotherMethod() {
				return null;
			}
		}
		RoutingKeyMethodScanner scanner = new RoutingKeyMethodScanner();
		assertNull(scanner.getRoutingKeyMethod(AstrixRouting.class, Test.class));
	}

}
