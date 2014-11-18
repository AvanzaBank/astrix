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
package lunch.pu;

import java.util.Arrays;
import java.util.List;

import lunch.api.LunchRestaurant;
import lunch.api.LunchService;
import lunch.api.provider.feeder.InternalLunchFeeder;

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.provider.component.AsterixServiceComponentNames;
import com.avanza.astrix.provider.core.AsterixServiceComponentName;
import com.avanza.astrix.provider.core.AsterixServiceExport;

@AsterixServiceComponentName(AsterixServiceComponentNames.GS_REMOTING)
@AsterixServiceExport({LunchService.class, InternalLunchFeeder.class})
public class LunchServiceImpl implements LunchService, InternalLunchFeeder {

	private final GigaSpace gigaSpace;
	
	@Autowired
	public LunchServiceImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

//	@Override
//	public List<LunchRestaurant> getAllLunchRestaurants(String foodType) {
//		LunchRestaurant template = new LunchRestaurant();
//		template.setFoodType(foodType);
//		return Arrays.asList(gigaSpace.readMultiple(template));
//	}
	
	@Override
	public List<LunchRestaurant> getAllLunchRestaurants() {
		LunchRestaurant template = new LunchRestaurant();
		return Arrays.asList(gigaSpace.readMultiple(template));
	}

	@Override
	public void addLunchRestaurant(LunchRestaurant restaurant) {
		this.gigaSpace.write(restaurant);
	}

	@Override
	public LunchRestaurant getLunchRestaurant(String name) {
		return this.gigaSpace.readById(LunchRestaurant.class, name);
	}

}


