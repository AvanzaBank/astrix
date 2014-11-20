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
package lunch.web;

import java.util.List;

import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import lunch.api.LunchUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class LunchRestaurantController {
	
	private LunchService lunchService;
	private LunchUtil lunchUtil;
	
	@Autowired
	public LunchRestaurantController(LunchService lunchService, LunchUtil lunchUtil) {
		this.lunchService = lunchService;
		this.lunchUtil = lunchUtil;
	}
	@RequestMapping(value= "/lunchrestaurants", method = RequestMethod.POST)
	public LunchRestaurant addLunchRestaurant(@RequestBody LunchRestaurant restaurant) {
		if (restaurant.getName() == null) {
			throw new IllegalArgumentException("Restaurant name is mandatory");
		}
		this.lunchService.addLunchRestaurant(restaurant);
		return restaurant;
	}
	
	@RequestMapping(value= "/lunchrestaurants", method = RequestMethod.GET)
	public List<LunchRestaurant> addLunchRestaurant() {
		List<LunchRestaurant> lunchRestaurants = this.lunchService.getAllLunchRestaurants();
		return lunchRestaurants;
	}
	

	@RequestMapping(value= "/randomrestaurant", method = RequestMethod.GET)
	public LunchRestaurant getRandomLunchRestaurant() {
		return lunchUtil.suggestRandomRestaurant();
	}

}
