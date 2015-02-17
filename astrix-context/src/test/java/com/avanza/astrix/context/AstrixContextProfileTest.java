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

import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixExcludedByProfile;
import com.avanza.astrix.provider.core.AstrixIncludedByProfile;
import com.avanza.astrix.provider.core.Library;

public class AstrixContextProfileTest {
	
	@Test
	public void aProviderIsNotExcludedIfTheCorrespondingProfileIsNotEacive() throws Exception {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.registerApiProvider(NormalPingProvider.class);
		configurer.registerApiProvider(ReversePingProvider.class);
		AstrixContext context = configurer.configure();
		
		Ping ping = context.getBean(Ping.class);
		assertEquals("foo", ping.ping("foo"));
	}
	
	@Test
	public void aProviderIsExcludedWhenTheCorrespondingProfileIsActivet() throws Exception {
		TestAstrixConfigurer configurer = new TestAstrixConfigurer();
		configurer.activateProfile("test");
		configurer.registerApiProvider(NormalPingProvider.class);
		configurer.registerApiProvider(ReversePingProvider.class);
		AstrixContext context = configurer.configure();
		
		Ping ping = context.getBean(Ping.class);
		assertEquals("oof", ping.ping("foo"));
	}
	
	@AstrixExcludedByProfile("test")
	@AstrixApiProvider
	public static class NormalPingProvider {
		@Library
		public Ping ping() {
			return new NormalPing();
		}
	}
	
	@AstrixIncludedByProfile("test")
	@AstrixApiProvider
	public static class ReversePingProvider {
		
		@Library
		public Ping ping() {
			return new ReversePing();
		}
	}
	
	interface Ping {
		String ping(String msg);
	}
	
	static class NormalPing implements Ping {
		@Override
		public String ping(String msg) {
			return msg;
		}
	}
	
	static class ReversePing implements Ping {
		@Override
		public String ping(String msg) {
			if (msg.isEmpty()) {
				return "";
			}
			return msg.charAt(msg.length() - 1) + ping(msg.substring(0, msg.length() - 1));
		}
	}
	

}
