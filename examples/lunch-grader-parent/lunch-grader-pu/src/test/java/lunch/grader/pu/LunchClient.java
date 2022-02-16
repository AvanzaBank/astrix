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
package lunch.grader.pu;

import com.avanza.astrix.context.AstrixConfigurer;
import com.avanza.astrix.context.AstrixContext;

import lunch.api.LunchRestaurant;
import lunch.api.LunchService;

public class LunchClient {
	
	public static void main(String[] args) throws InterruptedException {
		AstrixConfigurer configurer = new AstrixConfigurer();
		AstrixContext AstrixContext = configurer.configure();
		LunchService lunchService = AstrixContext.waitForBean(LunchService.class, 5000);
		
		LunchRestaurant r = new LunchRestaurant();
		r.setFoodType("vegetarian");
		r.setName("Martins Green Room");
		lunchService.addLunchRestaurant(r);
		
	}

}
