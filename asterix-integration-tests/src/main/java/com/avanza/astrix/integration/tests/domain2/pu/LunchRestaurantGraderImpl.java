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
package com.avanza.astrix.integration.tests.domain2.pu;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.integration.tests.domain.api.GetLunchRestaurantRequest;
import com.avanza.astrix.integration.tests.domain.api.LunchRestaurant;
import com.avanza.astrix.integration.tests.domain.api.LunchService;
import com.avanza.astrix.integration.tests.domain2.api.LunchRestaurantGrader;
import com.avanza.astrix.provider.core.AsterixServiceExport;

@AsterixServiceExport(LunchRestaurantGrader.class)
public class LunchRestaurantGraderImpl implements LunchRestaurantGrader {
	
	private final LunchService lunchService;
	private final GigaSpace gigaSpace;

	@Autowired
	public LunchRestaurantGraderImpl(LunchService lunchService, GigaSpace gigaSpace) {
		this.lunchService = lunchService;
		this.gigaSpace = gigaSpace;
	}

	@Override
	public void grade(String restaurantName, int grade) {
		GetLunchRestaurantRequest req = new GetLunchRestaurantRequest();
		req.setName(restaurantName);
		LunchRestaurant lunchRestaurant = lunchService.getLunchRestaurant(req);
		if (lunchRestaurant == null) {
			throw new IllegalArgumentException("Unknown lunch restaurant: " + restaurantName);
		}
		LunchRestaurantGrade currentGrade = gigaSpace.readById(LunchRestaurantGrade.class, restaurantName);
		if (currentGrade == null) {
			currentGrade = new LunchRestaurantGrade();
			currentGrade.setRestaurantName(restaurantName);
		}
		currentGrade.addGrade(grade);
		gigaSpace.write(currentGrade);
	}

	@Override
	public double getAvarageGrade(String restaurantName) {
		LunchRestaurantGrade currentGrade = gigaSpace.readById(LunchRestaurantGrade.class, restaurantName);
		if (currentGrade == null) {
			return -1D;
		}
		return currentGrade.avarageGrade();
	}
	
	
	

}
