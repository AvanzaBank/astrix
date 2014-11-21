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
package tutorial.t1.provider;

import java.util.List;
import java.util.Random;

import tutorial.t1.api.LunchRestaurantFinder;
import tutorial.t1.api.LunchUtil;

public class LunchUtilImpl implements LunchUtil {
	
	private Random rnd = new Random();
	private LunchRestaurantFinder lunchRestaurantFinder;
	
	public LunchUtilImpl(LunchRestaurantFinder lunchRestaurantFinder) {
		this.lunchRestaurantFinder = lunchRestaurantFinder;
	}

	@Override
	public String randomLunchRestaurant() {
		List<String> restaurants = getAllRestaurants();
		return restaurants.get(rnd.nextInt(restaurants.size()));
	}

	private List<String> getAllRestaurants() {
		return lunchRestaurantFinder.getAllRestaurants();
	}

}
