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
package runners;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.Subscriber;
import rx.functions.Func3;
import rx.functions.Func4;

import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommand.Setter;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.HystrixObservableCommand;

public class HystrixSpike {
	
	
	// TODO: DELETE ME
	
	public static void main(String[] args) {
		HystrixPlugins.getInstance().registerEventNotifier(new HystrixEventNotifier() {
			@Override
			public void markCommandExecution(HystrixCommandKey key,
					ExecutionIsolationStrategy isolationStrategy, int duration,
					List<HystrixEventType> eventsDuringExecution) {
				System.out.println("Executed command: " + key.name() + ", isolationStrategy=" + isolationStrategy + ", duration: " + duration + ", events: " + eventsDuringExecution);
			}
		});
		test2();
	}

	private static void test2() {
		long time = System.currentTimeMillis();
		HystrixCommandGroupKey commandProps = HystrixCommandGroupKey.Factory.asKey("foo");
		Observable<String> a1 = new HystrixObservableCommand<String>(commandProps) {
			@Override
			protected Observable<String> construct() {
				return TimeConsumingObservable.create(new TimeConsumingTask(500L));
			}
		}.observe();
		Observable<String> a2 = new HystrixObservableCommand<String>(commandProps) {
			@Override
			protected Observable<String> construct() {
				return TimeConsumingObservable.create(new TimeConsumingTask(500L));
			}
		}.observe();
		Observable<String> a3 = new HystrixObservableCommand<String>(commandProps) {
			@Override
			protected Observable<String> construct() {
				return TimeConsumingObservable.create(new TimeConsumingTask(500L));
			}
		}.observe();
		System.out.println(System.currentTimeMillis() - time);
			
		time = System.currentTimeMillis();
		List<String> result = Observable.zip(a1, a2, a3, new Func3<String, String, String, List<String>>() {
			@Override
			public List<String> call(String t1, String t2, String t3) {
				List<String> result = new ArrayList<String>();
				result.add(t1);
				result.add(t2);
				result.add(t3);
				return result;
			}
		}).toBlocking().first();
		System.out.println((System.currentTimeMillis() - time) + " - " + result);
			
//		System.exit(0);
	}
	
	private static void test1() {
		Setter commandProps = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("foo"));
//							  .andCommandPropertiesDefaults(com.netflix.hystrix.HystrixCommandProperties.Setter()
//									  																	.withExecutionIsolationStrategy(ExecutionIsolationStrategy.SEMAPHORE));
		
		Observable<String> a1 = new HystrixCommand<String>(commandProps) {
			@Override
			protected String run() throws Exception {
				return new TimeConsumingTask(500L).call();
			}
		}.observe();
		long time = System.currentTimeMillis();
		Observable<String> a2 = new HystrixCommand<String>(commandProps) {
			@Override
			protected String run() throws Exception {
				return new TimeConsumingTask(500L).call();
			}
		}.observe();
		Observable<String> a3 = new HystrixCommand<String>(commandProps) {
			@Override
			protected String run() throws Exception {
				return new TimeConsumingTask(500L).call();
			}
		}.observe();
		Observable<String> a4 = new HystrixCommand<String>(commandProps) {
			@Override
			protected String run() throws Exception {
				return new TimeConsumingTask(1500L).call();
			}
		}.observe();
		System.out.println(System.currentTimeMillis() - time);
			
		time = System.currentTimeMillis();
		List<String> result = Observable.zip(a1, a2, a3, a4, new Func4<String, String, String,String, List<String>>() {
			@Override
			public List<String> call(String t1, String t2, String t3, String t4) {
				List<String> result = new ArrayList<String>();
				result.add(t1);
				result.add(t2);
				result.add(t3);
				result.add(t4);
				return result;
			}
		}).toBlocking().first();
		System.out.println((System.currentTimeMillis() - time) + " - " + result);
			
		System.exit(0);
	}
	
	private static class TimeConsumingObservable {
		public static Observable<String> create(final TimeConsumingTask task) {
			return Observable.create(new OnSubscribe<String>() {
				@Override
				public void call(final Subscriber<? super String> t1) {
					new Thread() {
						public void run() {
							try {
								t1.onNext(task.call());
								t1.onCompleted();
							} catch (Exception e) {
								t1.onError(e);;
							}
						};
					}.start();
				}
			});
		}
	}
	
	private static class TimeConsumingTask {
		private long time;
		
		public TimeConsumingTask(long time) {
			this.time = time;
		}

		public String call() throws Exception {
			Thread.sleep(time);;
			return String.valueOf(time);
		}
		
	}

}
