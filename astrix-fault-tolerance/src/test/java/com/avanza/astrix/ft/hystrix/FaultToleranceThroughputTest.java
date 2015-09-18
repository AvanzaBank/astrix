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

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Service;
import com.netflix.hystrix.Hystrix;

public class FaultToleranceThroughputTest {
	
	private InMemoryServiceRegistry registry = new InMemoryServiceRegistry();
	private AstrixContext astrixContext;
	
	private static final AtomicInteger counter = new AtomicInteger(0);
    
	@Before
	public void setup() {
		TestAstrixConfigurer astrixConfigurer = new TestAstrixConfigurer();
        astrixConfigurer.enableFaultTolerance(true);
//        astrixConfigurer.enableFaultTolerance(false);
        astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, registry.getServiceUri());
        /*
         * Note: We are using unique command names for each individual tests to ensure that circuit-breaker/bulk-head
         * is in a clean state when starting execution of each test
         */
        astrixConfigurer.registerStrategy(HystrixCommandNamingStrategy.class, new HystrixCommandNamingStrategy() {
			@Override
			public String getGroupKeyName(PublishedAstrixBean<?> beanDefinition) {
				return "FaultToleranceThrougputTestKey-" + counter.get();
			}
			@Override
			public String getCommandKeyName(PublishedAstrixBean<?> beanDefinition) {
				return "FaultToleranceThrougputTestGroup-" + counter.get();
			}
		});
        astrixConfigurer.registerApiProvider(PingApi.class);
        astrixContext = astrixConfigurer.configure();
	}
	
	@After
	public void after() {
		astrixContext.destroy();
		Hystrix.reset();
	}
    
    @Test(timeout=10000)
    public void faultToleranceProvidesGoodThoughputWithSlowConsumer() throws Exception {
         registry.registerProvider(Ping.class, new SlowPing(100, TimeUnit.MILLISECONDS)); // 100 ms under the 250 ms timeout for the serivce
         
         Ping ping = astrixContext.getBean(Ping.class);
         
         AtomicInteger throughput = new AtomicInteger(0);
         List<Worker> workers = new ArrayList<>();
         for (int i = 0; i < 50; i++) {
        	 workers.add(new Worker(() -> ping.ping("foo"), throughput));
         }
         workers.stream().forEach(Worker::start);
         Thread.sleep(1000);
         workers.stream().forEach(Worker::interrupt);
         
         assertTrue("Througput: " + throughput.get(), throughput.get() >= 10000);
    }
    
    @Test(timeout=10000)
    public void faultToleranceProvidesGoodThoughputWithNonRespondingConsumer() throws Exception {
         registry.registerProvider(Ping.class, new NonRespondingPing());
         
         Ping ping = astrixContext.getBean(Ping.class);
         
         AtomicInteger throughput = new AtomicInteger(0);
         List<Worker> workers = new ArrayList<>();
         for (int i = 0; i < 50; i++) {
        	 workers.add(new Worker(() -> ping.ping("foo"), throughput));
         }
         workers.stream().forEach(Worker::start);
         Thread.sleep(1000);
         workers.stream().forEach(Worker::interrupt);
         
         assertTrue("Througput: " + throughput.get(), throughput.get() >= 10000);
    }
    
    @Test(timeout=2500)
    public void invocationsAreProtectedByTimeout() throws Exception {
         registry.registerProvider(Ping.class, new NonRespondingPing());
         
         Ping ping = astrixContext.getBean(Ping.class);
         try {
			ping.ping("foo");
		} catch (ServiceUnavailableException e) {
		}
    }
    
    public static class Worker extends Thread {
    	
    	private final Runnable command;
    	private final AtomicInteger executionCount;
    	
    	public Worker(Runnable command, AtomicInteger executionCount) {
			this.command = command;
			this.executionCount = executionCount;
		}

		public void run() {
    		while(!interrupted()) {
    			try {
					command.run();
				} catch (Exception e) {
				}
    			executionCount.incrementAndGet();
    		}
    	}		
    	
    }
    
    public interface Ping {
         String ping(String msg);
    }
    
    public static class NonRespondingPing implements Ping {

    	private CountDownLatch countDownLatch = new CountDownLatch(1);
    	
    	@Override
         public String ping(String msg) {
              try {
            	  countDownLatch.await(); // Block indefinitly 
              } catch (InterruptedException e) {
              }
              return msg;
         }
         
    }
    
    public static class SlowPing implements Ping {

		private int simulatedExecutionTime;
		private TimeUnit unit;

		public SlowPing(int simulatedExecutionTime, TimeUnit unit) {
			this.simulatedExecutionTime = simulatedExecutionTime;
			this.unit = unit;
		}

		@Override
		public String ping(String msg) {
			try {
				Thread.sleep(unit.toMillis(simulatedExecutionTime)); // If we dont use fault-tolerance we wont get more than around 50 * 2 = 100 requests throughput
			} catch (InterruptedException e) {
			}
			return msg;
		}
         
    }
    
    @AstrixApiProvider
    public interface PingApi {
         @DefaultBeanSettings(initialTimeout=250)
         @Service
         Ping ping();
    }


}
