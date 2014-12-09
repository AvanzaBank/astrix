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
package com.avanza.astrix.context;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.avanza.astrix.provider.core.AstrixConfigLookup;
import com.avanza.astrix.provider.core.AstrixServiceProvider;

public class AstrixServiceTest {
	
	@Test
	public void testName() throws Exception {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.registerApiDescriptor(PingApiDescriptor.class);
		astrixConfigurer.set("pingServiceUri", AstrixDirectComponent.registerAndGetUri(Ping.class, new PingImpl()));
		AstrixContext astrix = astrixConfigurer.configure();
		Ping ping = astrix.getBean(Ping.class);
		assertEquals("hello", ping.ping("hello"));
	}
	
	interface Ping {
		String ping(String msg);
	}
	
	static class PingImpl implements Ping {
		public String ping(String msg) {
			return msg;
		}
	}
	
	@AstrixConfigLookup("pingServiceUri")
	@AstrixServiceProvider(Ping.class)
	public static class PingApiDescriptor {
	}

}
