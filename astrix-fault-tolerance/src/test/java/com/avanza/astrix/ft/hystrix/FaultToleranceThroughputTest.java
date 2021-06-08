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

import com.avanza.astrix.beans.core.AstrixBeanKey;
import com.avanza.astrix.beans.core.AstrixBeanSettings;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.ft.HystrixCommandNamingStrategy;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.context.TestAstrixConfigurer;
import com.avanza.astrix.core.ServiceUnavailableException;
import com.avanza.astrix.provider.core.AstrixApiProvider;
import com.avanza.astrix.provider.core.DefaultBeanSettings;
import com.avanza.astrix.provider.core.Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.function.ThrowingConsumer;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import rx.Observable;

import java.util.EnumSet;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import static com.avanza.astrix.ft.hystrix.FaultToleranceThroughputTest.Protection.BULK_HEAD;
import static com.avanza.astrix.ft.hystrix.FaultToleranceThroughputTest.Protection.TIMEOUT;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FaultToleranceThroughputTest {
	
	public enum Protection {
		BULK_HEAD,
		TIMEOUT,
		CIRCUIT_BREAKER,
	}
	
	private static final AtomicInteger counter = new AtomicInteger(0);
	private final InMemoryServiceRegistry registry = new InMemoryServiceRegistry();
	private AstrixContext astrixContext;
	private AbstractFailingService server;
	private TestAstrixConfigurer astrixConfigurer;

	public static Stream<Object[]> testCases() {
		return Stream.of(
				new Object[]{"Sync Invocation", serviceCall(), allProtections()},
				// Current design of FT layer does not provide proper protection for methods returning Future
				new Object[]{"Future", asyncServiceCall(), allProtectionsExcept(BULK_HEAD)},
				new Object[]{"Observable", observableServiceCall(), allProtections()},
				new Object[]{"Blocking Observable", blockingObservableServiceCall(), allProtectionsExcept(TIMEOUT)},
				new Object[]{"CompletableFuture", completableServiceCall(), allProtections()},
				new Object[]{"Blocking CompletableFuture", blockingCompletableServiceCall(), allProtectionsExcept(TIMEOUT)}
						);
	}

	private static Set<Protection> allProtectionsExcept(Protection missingProtection) {
		return EnumSet.of(missingProtection);
	}

	private static Set<Protection> allProtections() {
		return EnumSet.noneOf(Protection.class);
	}

	private static ThrowingConsumer<FailingService> completableServiceCall() {
		return (service) -> { 
			try {
				service.completableServiceCall().get();
			} catch (ExecutionException e) {
				throw (RuntimeException) e.getCause();
			} 
		};
	}
	
	private static ThrowingConsumer<FailingService> blockingCompletableServiceCall() {
		return (service) -> { 
			try {
				service.blockingCompletableServiceCall().get();
			} catch (ExecutionException e) {
				throw (RuntimeException) e.getCause();
			} 
		};
	}

	private static ThrowingConsumer<FailingService> observableServiceCall() {
		return (service) -> {
			service.observeServiceCall().toBlocking().first();
		};
	}
	
	private static ThrowingConsumer<FailingService> blockingObservableServiceCall() {
		return (ping) -> ping.blockingObserveServiceCall().toBlocking().first();
	}

	private static ThrowingConsumer<FailingService> asyncServiceCall() {
		return FailingService::asyncServiceCall;
	}

	private static ThrowingConsumer<FailingService> serviceCall() {
		return FailingService::serviceCall;
	}


	@BeforeEach
	void setup() {
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
			public String getCommandKeyName(AstrixBeanKey<?> astrixBeanKey) {
				return astrixBeanKey.getBeanType().getName() + counter.get();
			}
		});
        astrixConfigurer.registerApiProvider(ServiceApi.class);
        astrixContext = astrixConfigurer.configure();
	}
	
	@AfterEach
	void after() {
		astrixContext.destroy();
		if (server != null) {
			server.exec.shutdownNow();
		}
	}

	@Timeout(10)
	@MethodSource("testCases")
	@ParameterizedTest(name = "{0}")
	void faultToleranceProvidesGoodThroughputWithSlowConsumer(@SuppressWarnings("unused") String name,
															  ThrowingConsumer<FailingService> serviceInvocation,
															  Set<Protection> missingProtections) throws Exception {
		// With a 250 ms response time, it not possible to get more than 1000/250 * 50 = 200 request throughput without bulkhead
		setServer(new SlowService(250, MILLISECONDS)); // The timeout is set to 2000 ms, so this call is not expected to timeout
		FailingService service = astrixContext.getBean(FailingService.class);

		ThroughputTest throughputTest = new ThroughputTest(1_000, 50, simulatedPageViewUsing(() -> serviceInvocation.accept(service)));
		throughputTest.run(1, TimeUnit.SECONDS);

		if (!missingProtections.contains(BULK_HEAD)) {
			assertTrue(server.getMaxConcurrentExecutions() <= 20, "Max concurrent server calls: " + server.getMaxConcurrentExecutions());
		}
		// Expect full throughput within 1 second, that is, all but the 20 (bulkhead size) that is still waiting for a response)
		throughputTest.assertThroughputPercentAtLeast(97);
	}

	private void setServer(AbstractFailingService server) {
		this.server = server;
		registry.registerProvider(FailingService.class, server); // 100 ms under the 250 ms timeout for the service
	}

	@Timeout(10)
	@MethodSource("testCases")
	@ParameterizedTest(name = "{0}")
	void faultToleranceProvidesGoodThroughputWithNonRespondingConsumer(@SuppressWarnings("unused") String name,
																	   ThrowingConsumer<FailingService> serviceInvocation,
																	   Set<Protection> missingProtections) throws Exception {
		setServer(new NonRespondingService());
		FailingService service = astrixContext.getBean(FailingService.class);

		
		ThroughputTest throughputTest = new ThroughputTest(1_000, 50, simulatedPageViewUsing(() -> serviceInvocation.accept(service)));
		throughputTest.run(1, TimeUnit.SECONDS);

		// Expect full throughput within 1 second
		if (!missingProtections.contains(BULK_HEAD)) {
			assertTrue(server.getMaxConcurrentExecutions() <= 20, "Max concurrent server calls: " + server.getMaxConcurrentExecutions());
		}
		// Expect full throughput within 1 second, that is, all but the 20 (bulkhead size) that is still waiting for a response)
		throughputTest.assertThroughputPercentAtLeast(97);
	}

	@Timeout(value = 2500, unit = MILLISECONDS)
	@MethodSource("testCases")
	@ParameterizedTest(name = "{0}")
	void invocationsAreProtectedByTimeout(@SuppressWarnings("unused") String name,
										  ThrowingConsumer<FailingService> serviceInvocation,
										  Set<Protection> missingProtections) throws Throwable {
		if (missingProtections.contains(TIMEOUT)) {
			return;
		}
		astrixConfigurer.set(AstrixBeanSettings.TIMEOUT, AstrixBeanKey.create(FailingService.class), 50);
		registry.registerProvider(FailingService.class, new NonRespondingService());
		FailingService service = astrixContext.getBean(FailingService.class);

		try {
			serviceInvocation.accept(service);
		} catch (ServiceUnavailableException ignore) {
		}
	}

    
    /*
     * This command simulates the job of executing a page view in a web application.
     */
	private Executable simulatedPageViewUsing(Executable invocation) {
		return () -> {
			try {
				// This simulates a low importance service call. The fault-tolerance layer should
				// ensure that this call fails fast
				invocation.execute();
			} catch (ServiceUnavailableException ignore) {
			}
			// Other more important work on the page view
		};
	}
	
	private static class ThroughputStatistics {
		final int successful;
		final int failed;
		final int pending;
		final int submittedExecutionCount;
		final int nonFinishedExecutionCount;
		
		public ThroughputStatistics(int successful, int failed, int pending, int nonFinishedExecutionCount, int totalExecutions) {
			this.successful = successful;
			this.failed = failed;
			this.pending = pending;
			this.nonFinishedExecutionCount = nonFinishedExecutionCount;
			this.submittedExecutionCount = totalExecutions;
		}
		public double successfulThroughputPercentage() {
			return 100 * successful / (double) submittedExecutionCount;
		}
		public String summary() {
			return String.format("throughput: %2.1f%% \n successful: %d \n failed: %d \n submittedExecutionCount: %d \n pendingExecutionCount: %d \n nonFinishedExecutionCount: %d", 
					successfulThroughputPercentage(),
					successful,
					failed, 
					submittedExecutionCount,
					pending,
					nonFinishedExecutionCount);
		}
	}
	
	public static class ThroughputTest {
		private final int totalExecutions;
		private final Executable command;
		private final int executionThreadCount;
		private ThroughputStatistics statistics;
		
		public ThroughputTest(int totalExecutions, int executionThreadCount, Executable command) {
			this.totalExecutions = totalExecutions;
			this.executionThreadCount = executionThreadCount;
			this.command = command;
		}
		
		public void run(int maxExecutionTime, TimeUnit unit) throws InterruptedException {
			AtomicInteger successfulExecutions = new AtomicInteger(0);
			AtomicInteger failedExecutions = new AtomicInteger(0);
			AtomicInteger pendingExecutions = new AtomicInteger();
			AtomicInteger nonFinishedExecutionCount = new AtomicInteger();
			ExecutorService executor = Executors.newFixedThreadPool(executionThreadCount);
			try {
				for (int i = 0; i < totalExecutions; i++) {
					pendingExecutions.incrementAndGet();
					executor.submit(() -> {
						pendingExecutions.decrementAndGet();
						nonFinishedExecutionCount.incrementAndGet();
						try {
							command.execute();
							successfulExecutions.incrementAndGet();
						} catch (Throwable e) {
							failedExecutions.incrementAndGet();
						} finally {
							nonFinishedExecutionCount.decrementAndGet();
						}
					});
				}
				executor.shutdown(); // Allow all jobs left in the queue to terminate
				executor.awaitTermination(maxExecutionTime, unit);
				statistics = new ThroughputStatistics(successfulExecutions.get(), failedExecutions.get(), pendingExecutions.get(), nonFinishedExecutionCount.get(), totalExecutions);
			} finally {
				executor.shutdownNow(); // Abort any remaining job
			}
		}
		
		public String summary() {
			return String.format("successful: %d failed: %d throughput: %2.1f%%",
					statistics.successful,
					statistics.failed, 
					statistics.successfulThroughputPercentage());
		}

		public void assertThroughputPercentAtLeast(int expectedMinimumThroughput) {
			assertTrue(statistics.successfulThroughputPercentage() >= expectedMinimumThroughput, "Expected minimum throughput: " + expectedMinimumThroughput + "%. Test Result:\n " + statistics.summary());
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
    
    public abstract static class AbstractFailingService implements FailingService {
    	private final ExecutorService exec = Executors.newCachedThreadPool();
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
		 */
		public int getMaxConcurrentExecutions() {
			return maxConcurrentExecutions.get();
		}
		
		protected abstract String invokeServiceImpl();

		@Override
		public Observable<String> observeServiceCall() {
			return Observable.unsafeCreate((subscriber) -> {
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
			CompletableFuture<String> result = new CompletableFuture<>();
			exec.execute(() -> result.complete(serviceCall()));
			return result;
		}

		@Override
		public CompletableFuture<String> blockingCompletableServiceCall() {
			CompletableFuture<String> result = new CompletableFuture<>();
			result.complete(serviceCall());
			return result;
		}
         
    }

	public static class NonRespondingService extends AbstractFailingService {

		private final CountDownLatch countDownLatch = new CountDownLatch(1);

		@Override
		protected String invokeServiceImpl() {
			try {
				countDownLatch.await(); // Block indefinitely
			} catch (InterruptedException ignore) {
			}
			return "success";
		}
	}
    
    public static class SlowService extends AbstractFailingService {

		private final int simulatedExecutionTime;
		private final TimeUnit unit;

		public SlowService(int simulatedExecutionTime, TimeUnit unit) {
			this.simulatedExecutionTime = simulatedExecutionTime;
			this.unit = unit;
		}

		@Override
		protected String invokeServiceImpl() {
			try {
				Thread.sleep(unit.toMillis(simulatedExecutionTime)); // If we don't use fault-tolerance we wont get more than around 50 * 2 = 100 requests throughput
			} catch (InterruptedException ignore) {
			}
			return "succuss";
		}
    }
    
    @AstrixApiProvider
    public interface ServiceApi {
         @DefaultBeanSettings(initialTimeout=2000)
         @Service
         FailingService service();
    }
    
}
