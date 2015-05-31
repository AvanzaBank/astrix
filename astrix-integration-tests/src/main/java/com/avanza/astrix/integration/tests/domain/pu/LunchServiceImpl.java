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
package com.avanza.astrix.integration.tests.domain.pu;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.integration.tests.domain.api.GetLunchRestaurantRequest;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain.apiruntime.feeder.InternalLunchFeeder;
import com.avanza.astrix.provider.core.AstrixServiceExport;

@AstrixServiceExport({LunchService.class, InternalLunchFeeder.class})
public class LunchServiceImpl implements LunchService, InternalLunchFeeder {

	private LunchRestaurantRepo repo;
	
	@Autowired
	public LunchServiceImpl(LunchRestaurantRepo repo) {
		this.repo = repo;
	}

	@Override
	public LunchRestaurant suggestRandomLunchRestaurant(String foodType) {
		List<LunchRestaurant> candidates = this.repo.findByFoodType(foodType);
		return candidates.isEmpty() ? null : candidates.get(0);
	}

	@Override
	public void addLunchRestaurant(LunchRestaurant restaurant) {
		this.repo.writeLunchRestaurant(restaurant);
	}

	@Override
	public LunchRestaurant getLunchRestaurant(GetLunchRestaurantRequest r) {
		if ("throwException".equals(r.getName())) {
			throw new IllegalArgumentException("Illegal restaurant: " + r.getName());
		}
		return this.repo.getByName(r.getName());
	}
	
	@Override
	public List<LunchRestaurant> getLunchRestaurants(String... restaurantNames) {
		List<LunchRestaurant> result = new ArrayList<>();
		for (String restaurantName : restaurantNames) {
			result.add(this.repo.getByName(restaurantName));
		}
		return result;
	}

}


