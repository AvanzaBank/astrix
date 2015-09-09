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
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	public LunchRestaurant waitForLunchRestaurant(String name, int duration, TimeUnit unit) throws TimeoutException {
		Timeout timeout = new Timeout(unit.toMillis(duration));
		while(!timeout.hasTimeout()) {
			LunchRestaurant result = repo.getByName(name);
			if (result != null) {
				return result;
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				throw new TimeoutException("Received interrupt before reading restaurant: " + name);
			}
		}
		throw new TimeoutException("Failed to read restaurant before timeout: " + name);
	}
	
	private static class Timeout {

		private long endTimeMillis;

		public Timeout(long timeoutMillis) {
			this.endTimeMillis = currentTimeMillis() + timeoutMillis;
		}

		public boolean hasTimeout() {
			return currentTimeMillis() >= endTimeMillis;
		}

		// test hook
		long currentTimeMillis() {
			return System.currentTimeMillis();
		}
		
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


