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
package com.avanza.astrix.gs.remoting;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openspaces.remoting.Routing;

import com.avanza.astrix.core.AstrixRouting;
import com.avanza.astrix.core.remoting.Router;
import com.avanza.astrix.core.remoting.RoutingKey;
import com.avanza.astrix.remoting.client.AmbiguousRoutingException;

public class GsRoutingStrategyTest {
	
	@Test
	public void astrixRoutingTakesPrecedence() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@Routing String routingArg, @AstrixRouting String anotherArg) {
			}
		}
		Router router = new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
		RoutingKey routingKey = router.getRoutingKey(new Object[]{"ignored-arg", "second-arg"});
		assertEquals(RoutingKey.create("second-arg"), routingKey);
	}
	
	
	@Test
	public void routesOnRoutingAnnotatedArgument() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@Routing String routingArg, String anotherArg) {
			}
		}
		Router router = new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
		RoutingKey routingKey = router.getRoutingKey(new Object[]{"routing-arg", "another-arg"});
		assertEquals(RoutingKey.create("routing-arg"), routingKey);
	}
	
	@Test
	public void routesOnRoutingAnnotatedArgumentPropertyIfDefined() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@Routing("getRouting") ProperRoutingMethod routingArg) {
			}
		}
		
		Router router = new GsRoutingStrategy().create(Service.class.getMethod("hello", ProperRoutingMethod.class));
		RoutingKey routingKey = router.getRoutingKey(new Object[]{new ProperRoutingMethod()});
		assertEquals(RoutingKey.create("routing-arg"), routingKey);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void missingPropertyMethod_throwsIllegalArgumentException() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@Routing("getRouting") MissingRoutingMethod routingArg) {
			}
		}
		new GsRoutingStrategy().create(Service.class.getMethod("hello", MissingRoutingMethod.class));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void invalidPropertyMethod_throwsIllegalArgumentException() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@Routing("getRouting") IllegalRoutingMethod routingArg) {
			}
		}
		
		new GsRoutingStrategy().create(Service.class.getMethod("hello", IllegalRoutingMethod.class));
	}
	
	@Test(expected = AmbiguousRoutingException.class)
	public void multipleRoutingAnnotations_throwsAmbiguousRoutingException() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(@Routing String routingArg, @Routing String routingArg2) {
			}
		}
		new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
	}
	
	@Test(expected = AmbiguousRoutingException.class)
	public void noRoutingArgument_throwsAmbiguousRoutingException() throws Exception {
		class Service {
			@SuppressWarnings("unused")
			public void hello(String routingArg, String routingArg2) {
			}
		}
		new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
	}
	
	public static class ProperRoutingMethod {
		public String getRouting() {
			return "routing-arg";
		}
	}
	
	public static class MissingRoutingMethod {
	}
	
	public static class IllegalRoutingMethod {
		public String getRouting(String illegalArgument) {
			return "";
		}
	}

}
