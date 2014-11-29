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
package com.avanza.astrix.remoting.client;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.openspaces.remoting.Routing;

public class RemoteServiceMethodTest {
	
	
	@Test
	public void routesOnRoutingAnnotatedArgument() throws Exception {
		class Service {
			public void hello(@Routing String routingArg, String anotherArg) {
			}
		}
		Router router = new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
		RoutingKey routingKey = router.getRoutingKey("routing-arg");
		assertEquals(RoutingKey.create("routing-arg"), routingKey);
	}
	
	@Test
	public void routesOnRoutingAnnotatedArgumentPropertyIfDefined() throws Exception {
		class RoutingType {
			public String getRouting() {
				return "routing-arg";
			}
		}
		class Service {
			public void hello(@Routing("getRouting") RoutingType routingArg) {
			}
		}
		
		Router router = new GsRoutingStrategy().create(Service.class.getMethod("hello", RoutingType.class));
		RoutingKey routingKey = router.getRoutingKey(new RoutingType());
		assertEquals(RoutingKey.create("routing-arg"), routingKey);
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void missingPropertyMethod_throwsIllegalArgumentException() throws Exception {
		class RoutingType {
		}
		class Service {
			public void hello(@Routing("getRouting") RoutingType routingArg) {
			}
		}
		
		new GsRoutingStrategy().create(Service.class.getMethod("hello", RoutingType.class));
	}
	
	@Test(expected = IllegalArgumentException.class)
	public void invalidPropertyMethod_throwsIllegalArgumentException() throws Exception {
		class RoutingType {
			public String getRouting(String illegalArgument) {
				return "";
			}
		}
		class Service {
			public void hello(@Routing("getRouting") RoutingType routingArg) {
			}
		}
		
		new GsRoutingStrategy().create(Service.class.getMethod("hello", RoutingType.class));
	}
	
	@Test(expected = AmbiguousRoutingException.class)
	public void multipleRoutingAnnotations_throwsAmbiguousRoutingException() throws Exception {
		class Service {
			public void hello(@Routing String routingArg, @Routing String routingArg2) {
			}
		}
		new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
	}
	
	@Test(expected = AmbiguousRoutingException.class)
	public void noRoutingArgument_throwsAmbiguousRoutingException() throws Exception {
		class Service {
			public void hello(String routingArg, String routingArg2) {
			}
		}
		new GsRoutingStrategy().create(Service.class.getMethod("hello", String.class, String.class));
	}

}
