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
package lunch.api;

import java.util.List;

import org.openspaces.remoting.Routing;

import com.avanza.asterix.core.AsterixBroadcast;



public interface LunchService {
	
	@AsterixBroadcast
	List<LunchRestaurant> getAllLunchRestaurants();
	
	void addLunchRestaurant(LunchRestaurant restaurant);
	
	LunchRestaurant getLunchRestaurant(@Routing String name); 
}