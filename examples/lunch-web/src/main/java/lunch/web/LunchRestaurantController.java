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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/lunchrestaurants")
public class LunchRestaurantController {
	
	private LunchService lunchService;
	
	@Autowired
	public LunchRestaurantController(LunchService lunchService) {
		this.lunchService = lunchService;
	}

	@RequestMapping(method = RequestMethod.POST)
	public void addLunchRestaurant(@RequestParam("name") String name, @RequestParam("foodType") String foodType) {
		this.lunchService.addLunchRestaurant(new LunchRestaurant(name, foodType));
	}
	
	@RequestMapping(method = RequestMethod.GET)
	public List<LunchRestaurant> addLunchRestaurant() {
		List<LunchRestaurant> lunchRestaurants = this.lunchService.getAllLunchRestaurants();
		return lunchRestaurants;
	}

}
