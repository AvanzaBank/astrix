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
package lunch.web;

import java.util.ArrayList;
import java.util.List;

import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import lunch.api.LunchUtil;
import lunch.grader.api.LunchRestaurantGrader;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class LunchRestaurantController {
	
	private LunchService lunchService;
	private LunchUtil lunchUtil;
	private LunchRestaurantGrader grader;
	
	@Autowired
	public LunchRestaurantController(LunchService lunchService, LunchUtil lunchUtil, LunchRestaurantGrader grader) {
		this.lunchService = lunchService;
		this.lunchUtil = lunchUtil;
		this.grader = grader;
	}
	@RequestMapping(value= "/lunchrestaurants", method = RequestMethod.POST)
	public GradedLunchRestaurant addLunchRestaurant(@RequestBody GradedLunchRestaurant restaurant) {
		if (restaurant.getName() == null) {
			throw new IllegalArgumentException("Restaurant name is mandatory");
		}
		this.lunchService.addLunchRestaurant(restaurant.asLunchRestaurant());
		return restaurant;
	}
	
	@RequestMapping(value= "/lunchrestaurants", method = RequestMethod.GET)
	public List<GradedLunchRestaurant> getAllLunchRestaurants() {
		List<GradedLunchRestaurant> result = new ArrayList<>();
		for (LunchRestaurant restaurant : lunchService.getAllLunchRestaurants()) {
			GradedLunchRestaurant gradedLunchRestaurant = new GradedLunchRestaurant(restaurant);
			gradedLunchRestaurant.setGrade(grader.getAvarageGrade(restaurant.getName()));
			result.add(gradedLunchRestaurant);
		}
		return result;
	}
	

	@RequestMapping(value= "/randomrestaurant", method = RequestMethod.GET)
	public LunchRestaurant getRandomLunchRestaurant() {
		return lunchUtil.suggestRandomRestaurant();
	}

	@RequestMapping(value= "/grade/{restaurant}", method = RequestMethod.POST)
	public GradedLunchRestaurant grade(@PathVariable("restaurant") String name, @RequestBody int grade) {
		LunchRestaurant lunchRestaurant = this.lunchService.getLunchRestaurant(name);
		grader.grade(name, grade);
		GradedLunchRestaurant result = new GradedLunchRestaurant();
		result.setName(name);
		result.setFoodType(lunchRestaurant.getFoodType());
		result.setGrade(grader.getAvarageGrade(name));
		return result;
	}
	

	public static class GradedLunchRestaurant {
		private String name;
		private String foodType;
		private Double grade;
		
		public GradedLunchRestaurant() {
		}
		
		public LunchRestaurant asLunchRestaurant() {
			return new LunchRestaurant(name, foodType);
		}

		public GradedLunchRestaurant(LunchRestaurant r) {
			this.name = r.getName();
			this.foodType = r.getFoodType();
		}
		
		public String getName() {
			return name;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getFoodType() {
			return foodType;
		}
		public void setFoodType(String foodType) {
			this.foodType = foodType;
		}
		public Double getGrade() {
			return grade;
		}
		public void setGrade(Double grade) {
			this.grade = grade;
		}
		
	}

}
