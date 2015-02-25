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
package runners;

import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import lunch.api.LunchServiceAsync;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties.ExecutionIsolationStrategy;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

public class HystrixCommandExecutionSupervisor {
	
	public static void main(String[] args) throws InterruptedException, ExecutionException {
		
		HystrixPlugins.getInstance().registerEventNotifier(new HystrixEventNotifier() {
			@Override
			public void markCommandExecution(HystrixCommandKey key,
					ExecutionIsolationStrategy isolationStrategy, int duration,
					List<HystrixEventType> eventsDuringExecution) {
				System.out.println("Executed command: " + key.name() + ", isolationStrategy=" + isolationStrategy + ", duration: " + duration + ", events: " + eventsDuringExecution);
			}
		});
		AstrixConfigurer astrixConfigurer = new AstrixConfigurer();
		astrixConfigurer.set(AstrixSettings.ASTRIX_SERVICE_REGISTRY_URI, "gs-remoting:jini://*/*/service-registry-space?groups=" + Config.LOOKUP_GROUP_NAME);
		AstrixContext context = astrixConfigurer.configure();
		LunchService lunchService = context.getBean(LunchService.class);
		LunchServiceAsync lunchServiceAsync = context.getBean(LunchServiceAsync.class);

		while(true) {
			if (new Random().nextInt(5) == 1) {
				lunchService.addLunchRestaurant(new LunchRestaurant(new Random().nextInt() + "", "foo"));
			}
			Thread.sleep(1000);
			lunchService.getAllLunchRestaurants();
			
			Future<LunchRestaurant> asyncFuture = lunchServiceAsync.getLunchRestaurant("foo");
			asyncFuture.get();
		}
	}

}
