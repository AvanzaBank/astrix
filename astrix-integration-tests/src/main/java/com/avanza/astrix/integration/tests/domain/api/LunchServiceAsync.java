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
package com.avanza.astrix.integration.tests.domain.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.avanza.astrix.core.AstrixRouting;

import rx.Completable;

public interface LunchServiceAsync {

	// This interface intentionally mixes Future / AsyncFuture / CompletableFuture representation to get tests on each representation 
	
	CompletableFuture<LunchRestaurant> getLunchRestaurant(GetLunchRestaurantRequest request);
	
	CompletableFuture<LunchRestaurant> waitForLunchRestaurant(@AstrixRouting String name, int duration, TimeUnit unit);
	
	Completable addLunchRestaurant(LunchRestaurant restaurant);
}
