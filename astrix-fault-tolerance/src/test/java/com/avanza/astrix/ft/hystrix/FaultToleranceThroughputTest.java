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

import static com.avanza.astrix.ft.hystrix.FaultToleranceThroughputTest.Protection.BULK_HEAD;
import static com.avanza.astrix.ft.hystrix.FaultToleranceThroughputTest.Protection.TIMEOUT;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.EnumSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.beans.publish.PublishedAstrixBean;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.netflix.hystrix.Hystrix;

import rx.Observable;

@RunWith(Parameterized.class)
public class FaultToleranceThroughputTest {
	
	public enum Protection {
		BULK_HEAD,
		TIMEOUT,
		CIRCUIT_BREAKER,
	}
	
	private static final AtomicInteger counter = new AtomicInteger(0);
	private InMemoryServiceRegistry registry = new InMemoryServiceRegistry();
	private AstrixContext astrixContext;
	private Consumer<FailingService> serviceInvocation;
	private AbstractFailingService server;
	private EnumSet<Protection> missingProtections;
	private TestAstrixConfigurer astrixConfigurer;
	
	@Parameters(name = "{0}") // Uses first argument in parameters as name in name for test
	public static Collection<Object[]> testCases() {
		List<Object[]> result = new LinkedList<>();
		result.add(new Object[]{"Sync Invocation", serviceCall(), allProtections()});
		// Current design of FT layer does not provide proper protection for methods returning Future
		result.add(new Object[]{"Future", asyncServiceCall(), allProtectionsExcept(BULK_HEAD)}); 
		result.add(new Object[]{"Observable", observableServiceCall(), allProtections()});
		result.add(new Object[]{"Blocking Observable", blockingObservableServiceCall(), allProtectionsExcept(TIMEOUT)});
		result.add(new Object[]{"CompletableFuture", completableServiceCall(), allProtections()});
		result.add(new Object[]{"Blocking CompletableFuture", blockingCompletableServiceCall(), allProtectionsExcept(TIMEOUT) });
		return result;
	}

	private static EnumSet<Protection> allProtectionsExcept(Protection missingprotection) {
		return EnumSet.of(missingprotection);
	}

	private static EnumSet<Protection> allProtections() {
		return EnumSet.noneOf(Protection.class);
	}

	private static Consumer<FailingService> completableServiceCall() {
		return (service) -> { 
			try {
				service.completableServiceCall().get();
			} catch (ExecutionException e) {
				throw (RuntimeException) e.getCause();
			} 
		};
	}
	
	private static Consumer<FailingService> blockingCompletableServiceCall() {
		return (service) -> { 
			try {
				service.blockingCompletableServiceCall().get();
			} catch (ExecutionException e) {
				throw (RuntimeException) e.getCause();
			} 
		};
	}

	private static Consumer<FailingService> observableServiceCall() {
		return (service) -> {
			service.observeServiceCall().toBlocking().first();
		};
	}
	
	private static Consumer<FailingService> blockingObservableServiceCall() {
		return (ping) -> ping.blockingObserveServiceCall().toBlocking().first();
	}

	private static Consumer<FailingService> asyncServiceCall() {
		return (ping) -> ping.asyncServiceCall();
	}

	private static Consumer<FailingService> serviceCall() {
		return (ping) -> ping.serviceCall();
	}

	public FaultToleranceThroughputTest(String name, Consumer<FailingService> pingInvocation, EnumSet<Protection> missingProtections) {
		this.serviceInvocation = pingInvocation;
		this.missingProtections = missingProtections;
	}


	@Before
	public void setup() {
		astrixConfigurer = new TestAstrixConfigurer();
		astrixConfigurer.enableFaultTolerance(true);
        astrixConfigurer.set(AstrixSettings.ENABLE_BEAN_METRICS, true);
        astrixConfigurer.set(AstrixSettings.SERVICE_REGISTRY_URI, registry.getServiceUri());
        /*
         * Note: We are using unique command names for each individual tests to ensure that circuit-breaker/bulk-head
         * is in a clean state when starting execution of each test
         */
        counter.incrementAndGet();
        astrixConfigurer.registerStrategy(HystrixCommandNamingStrategy.class, new HystrixCommandNamingStrategy() {
			@Override
			public String getGroupKeyName(PublishedAstrixBean<?> beanDefinition) {
				return beanDefinition.getBeanKey().getBeanType().getName() + counter.get();
			}
			@Override
			public String getCommandKeyName(PublishedAstrixBean<?> beanDefinition) {
				return beanDefinition.getBeanKey().getBeanType().getName() + counter.get();
			}
		});
        astrixConfigurer.registerApiProvider(ServiceApi.class);
        astrixContext = astrixConfigurer.configure();
	}
	
	@After
	public void after() {
		astrixContext.destroy();
		Hystrix.reset();
		if (server != null) {
			server.exec.shutdownNow();
		}
	}

	@Test(timeout = 10000)
	public void faultToleranceProvidesGoodThoughputWithSlowConsumer() throws Exception {
		setServer(new SlowService(100, TimeUnit.MILLISECONDS)); // The timeout is set to 250 ms, so this call is not expected to timeout
		FailingService service = astrixContext.getBean(FailingService.class);

		ThroughputTest throughputTest = new ThroughputTest(10_000, 50, simulatedPageViewUsing(service));
		throughputTest.run(1, TimeUnit.SECONDS);

		if (!missingProtections.contains(BULK_HEAD)) {
			assertTrue("Max concurrent server calls: " + server.getMaxConcurrentExecutions(), server.getMaxConcurrentExecutions() <= 20);
		}
		// Expect full throughput within 1 second
		throughputTest.assertThroughputPercentAtLeast(99);
	}

	private void setServer(AbstractFailingService server) {
		this.server = server;
		registry.registerProvider(FailingService.class, server); // 100 ms under the 250 ms timeout for the service
	}
    
	@Test(timeout = 10000)
	public void faultToleranceProvidesGoodThoughputWithNonRespondingConsumer() throws Exception {
		setServer(new NonRespondingService());
		FailingService service = astrixContext.getBean(FailingService.class);

		
		ThroughputTest throughputTest = new ThroughputTest(10_000, 50, simulatedPageViewUsing(service));
		throughputTest.run(1, TimeUnit.SECONDS);

		// Expect full througput within 1 second
		if (!missingProtections.contains(BULK_HEAD)) {
			assertTrue("Max concurrent server calls: " + server.getMaxConcurrentExecutions(), server.getMaxConcurrentExecutions() <= 20);
		}
		
		throughputTest.assertThroughputPercentAtLeast(99);
	}

	@Test(timeout = 2500)
	public void invocationsAreProtectedByTimeout() throws Exception {
		if (missingProtections.contains(TIMEOUT)) {
			return;
		}
		astrixConfigurer.set(AstrixBeanSettings.INITIAL_TIMEOUT, AstrixBeanKey.create(FailingService.class), 50);
		registry.registerProvider(FailingService.class, new NonRespondingService());
		FailingService  service = astrixContext.getBean(FailingService.class);

		try {
			serviceInvocation.accept(service);
		} catch (ServiceUnavailableException e) {
		}
	}

    
    /*
     * This command simulates the job of executing a page view in a web application.
     * 
     */
	private Runnable simulatedPageViewUsing(FailingService service) {
		return () -> {
			try {
				// This simulates a low importence service call. The fault-tolerance layer should
				// ensure that this call fails fast
				serviceInvocation.accept(service);
			} catch (ServiceUnavailableException e) {
			}
			// Other more imortant work on the page view
		};
	}
	
	private static class ThroughputStatistics {
		final int successful;
		final int failed;
		final int remaining;
		final int submittedExecutionCount;
		public ThroughputStatistics(int successful, int failed, int remaining, int totalExecutions) {
			this.successful = successful;
			this.failed = failed;
			this.remaining = remaining;
			this.submittedExecutionCount = totalExecutions;
		}
		public double successfulThroughputPercentage() {
			return 100 * successful / (double) submittedExecutionCount;
		}
		public String summary() {
			return String.format("throughput: %2.1f%% \n ssuccessful: %d \n failed: %d \n submittedExecutionCount: %d \n nonStarted: %d", 
					successfulThroughputPercentage(),
					successful,
					failed, 
					submittedExecutionCount,
					remaining);
		}
	}
	
	public static class ThroughputTest {
		ExecutorService executor;
		private int totalExecutions;
		private Runnable command;
		private int executionThreadCount;
		private ThroughputStatistics statistics;
		
		public ThroughputTest(int totalExecutions, int executionThreadCount, Runnable command) {
			this.totalExecutions = totalExecutions;
			this.executionThreadCount = executionThreadCount;
			this.command = command;
		}
		
		public void run(int maxExecutionTime, TimeUnit unit) throws InterruptedException {
			AtomicInteger successfulExecutions = new AtomicInteger(0);
			AtomicInteger failedExecutions = new AtomicInteger(0);
			AtomicInteger remainingExecutions = new AtomicInteger(totalExecutions);
			executor = Executors.newFixedThreadPool(executionThreadCount);
			try {
				for (int i = 0; i < getTotalExecutions(); i++) {
					executor.submit(() -> {
						try {
							command.run();
							successfulExecutions.incrementAndGet();
						} catch (Exception e) {
							failedExecutions.incrementAndGet();
						} finally {
							remainingExecutions.decrementAndGet();
						}
					});
				}
				executor.shutdown(); // Allow all jobs left in the queue to terminate
				executor.awaitTermination(maxExecutionTime, unit);
				statistics = new ThroughputStatistics(successfulExecutions.get(), failedExecutions.get(), remainingExecutions.get(), totalExecutions);
			} finally {
				executor.shutdownNow(); // Abort any remaining job
			}
		}
		
		public String summary() {
			return String.format("suscessful: %d failed: %d througput: %2.1f%%", 
					statistics.successful,
					statistics.failed, 
					statistics.successfulThroughputPercentage());
		}
		
		
		public int getTotalExecutions() {
			return totalExecutions;
		}

		public void assertThroughputPercentAtLeast(int expectedMinimumThroughput) {
			assertTrue("Expected mininum throughput: " + expectedMinimumThroughput + "%. Test Result:\n " + statistics.summary(),
					this.statistics.successfulThroughputPercentage() > expectedMinimumThroughput);
		}
		
	}
    
    public interface FailingService {
         String serviceCall();
         Future<String> asyncServiceCall();
         Observable<String> observeServiceCall();
         Observable<String> blockingObserveServiceCall();
         CompletableFuture<String> completableServiceCall();
         CompletableFuture<String> blockingCompletableServiceCall();
    }
    
    public static abstract class AbstractFailingService implements FailingService {
    	private ExecutorService exec = Executors.newCachedThreadPool();
    	private final AtomicInteger maxConcurrentExecutions = new AtomicInteger(0);
    	private final AtomicInteger currentConcurrentExecutions = new AtomicInteger(0);
		@Override
		public Future<String> asyncServiceCall() {
			CompletableFuture<String> result = new CompletableFuture<>();
			exec.execute(() -> result.complete(serviceCall()));
			return result; 
		}
		
		@Override
		public final String serviceCall() {
			synchronized (currentConcurrentExecutions) {
				int currentConcurrentExecutionCount = this.currentConcurrentExecutions.incrementAndGet();
				if (currentConcurrentExecutionCount > maxConcurrentExecutions.get()) {
					maxConcurrentExecutions.set(currentConcurrentExecutionCount);
				}
			}
			try {
				return invokeServiceImpl();
			} finally {
				synchronized (currentConcurrentExecutions) {
					currentConcurrentExecutions.decrementAndGet();
				}
			}
		}
		
		/**
		 * The maximum number of concurrent executions into the failing service. 
		 * 
		 * This should never be larger than the bulkhead
		 * @return
		 */
		public int getMaxConcurrentExecutions() {
			return maxConcurrentExecutions.get();
		}
		
		protected abstract String invokeServiceImpl();

		@Override
		public Observable<String> observeServiceCall() {
			return Observable.create((subscriber) -> {
				exec.execute(() -> {
					try {
						subscriber.onNext(serviceCall());
						subscriber.onCompleted();
					} catch (Exception e) {
						subscriber.onError(e);
					}
				});
			});
		}

		@Override
		public Observable<String> blockingObserveServiceCall() {
			return Observable.just(serviceCall());
		}

		@Override
		public CompletableFuture<String> completableServiceCall() {
			CompletableFuture<String> result = new CompletableFuture<String>();
			exec.execute(() -> result.complete(serviceCall()));
			return result;
		}

		@Override
		public CompletableFuture<String> blockingCompletableServiceCall() {
			CompletableFuture<String> result = new CompletableFuture<String>();
			result.complete(serviceCall());
			return result;
		}
         
    }

	public static class NonRespondingService extends AbstractFailingService {

		private CountDownLatch countDownLatch = new CountDownLatch(1);

		@Override
		protected String invokeServiceImpl() {
			try {
				countDownLatch.await(); // Block indefinitly
			} catch (InterruptedException e) {
			}
			return "success";
		}
	}
    
    public static class SlowService extends AbstractFailingService {

		private int simulatedExecutionTime;
		private TimeUnit unit;

		public SlowService(int simulatedExecutionTime, TimeUnit unit) {
			this.simulatedExecutionTime = simulatedExecutionTime;
			this.unit = unit;
		}

		@Override
		protected String invokeServiceImpl() {
			try {
				Thread.sleep(unit.toMillis(simulatedExecutionTime)); // If we don't use fault-tolerance we wont get more than around 50 * 2 = 100 requests throughput
			} catch (InterruptedException e) {
			}
			return "succuss";
		}
    }
    
    @AstrixApiProvider
    public interface ServiceApi {
         @DefaultBeanSettings(initialTimeout=2000)
         @com.avanza.astrix.provider.core.Service
         FailingService service();
    }
    
    // Define Consumer/Runnable interface that allows checked exceptions
    public interface Consumer<T> {
	    void accept(T t) throws Exception;
	}
	public interface Runnable {
	    void run() throws Exception;
	}


}
