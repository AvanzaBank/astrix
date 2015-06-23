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
package lunch.tests;

import static com.avanza.astrix.test.util.AstrixTestUtil.isSuccessfulServiceInvocation;
import static org.junit.Assert.assertEquals;
import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import lunch.grader.api.LunchRestaurantGrader;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.beans.registry.InMemoryServiceRegistry;
import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;
import com.avanza.astrix.gs.test.util.PuConfigurers;
import com.avanza.astrix.gs.test.util.RunningPu;
import com.avanza.astrix.test.util.AutoCloseableRule;
import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;

public class LunchGraderIntegrationTest {
	
	public static InMemoryServiceRegistry serviceRegistry = new InMemoryServiceRegistry() {{
		set(AstrixSettings.BEAN_BIND_ATTEMPT_INTERVAL, 200);
	}};
	
	@ClassRule
	public static RunningPu lunchGraderPu = PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-grader-pu.xml")
			   											 .contextProperty("configSourceId", serviceRegistry.getConfigSourceId())
			   											 .startAsync(false)
														 .configure();
	@Rule
	public AutoCloseableRule autoCloseableRule = new AutoCloseableRule();
	
	@Test
	public void itsPossibleToStubOutServiceDependenciesUsingInMemoryServiceRegistry() throws Exception {
		LunchService lunchMock = Mockito.mock(LunchService.class);
		Mockito.stub(lunchMock.getLunchRestaurant("mcdonalds")).toReturn(new LunchRestaurant("mcdonalds", "fastfood"));
		serviceRegistry.registerProvider(LunchService.class, lunchMock, "lunch-system");
		
		AstrixConfigurer configurer = new AstrixConfigurer();
		configurer.set(AstrixSettings.SERVICE_REGISTRY_URI, serviceRegistry.getServiceUri());
		configurer.setSubsystem("lunch-system");
		AstrixContext astrix = autoCloseableRule.add(configurer.configure());
		final LunchRestaurantGrader grader = astrix.waitForBean(LunchRestaurantGrader.class, 5_000);
		
		// Not that the lunch-grader-pu grader.grade uses another service internally, hence we must wait
		// For that as well to bind its service-bean
		assertEventually(isSuccessfulServiceInvocation(new Runnable() {
			@Override
			public void run() {
				grader.grade("mcdonalds", 5);
			}
		}));
		grader.grade("mcdonalds", 3);
		
		assertEquals(4D, grader.getAvarageGrade("mcdonalds"), 0.01D);
	}

	private void assertEventually(Probe successfulServiceInvocation)
			throws InterruptedException {
		new Poller(10_000L, 50).check(successfulServiceInvocation);
	}
	

}
