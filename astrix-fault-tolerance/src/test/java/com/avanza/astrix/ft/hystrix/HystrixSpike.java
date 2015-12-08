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
package com.avanza.astrix.ft.hystrix;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.AstrixFaultToleranceProxy;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.AstrixQualifier;
import com.avanza.astrix.provider.core.Library;

public class HystrixSpike {
	
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		Logger.getRootLogger().setLevel(Level.INFO);
		Logger.getLogger(HystrixBeanFaultTolerance.class).info("HELL");
		TestAstrixConfigurer astrix = new TestAstrixConfigurer();
		astrix.enableFaultTolerance(true);
		astrix.registerApiProvider(PingApi.class);
		AstrixContext context = astrix.configure();
		
		TestAstrixConfigurer astrix2 = new TestAstrixConfigurer();
		astrix2.enableFaultTolerance(true);
		astrix2.registerApiProvider(PingApi.class);
		AstrixContext context2 = astrix2.configure();


		Ping ping = context.getBean(Ping.class);
		Ping fooPing = context2.getBean(Ping.class);
		ExecutorService exec = Executors.newFixedThreadPool(100);
		for (int i = 0; i < 1; i++) {
			exec.execute(() -> ping.ping("foo"));
			exec.execute(() -> fooPing.ping("foo"));
		}
		while (true) {
		}
	}
	
	public interface Ping {
		String ping(String msg);
	}
	
	public static class PingImpl implements Ping {
		public String ping(String msg) {
			try {
				Thread.sleep(10000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return msg;
		}
	}
	
	@AstrixApiProvider
	public static class PingApi {
		@AstrixFaultToleranceProxy
		@Library
		public Ping ping() {
			return new PingImpl();
		}

		@AstrixFaultToleranceProxy
		@Library
		@AstrixQualifier("foo")
		public Ping pings() {
			return new PingImpl();
		}
	}

}
