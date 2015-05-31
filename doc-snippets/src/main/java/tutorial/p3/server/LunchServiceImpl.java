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
package tutorial.p3.server;

import java.util.ArrayList;
import java.util.List;

import org.openspaces.core.GigaSpace;
import org.openspaces.remoting.Routing;
import org.springframework.beans.factory.annotation.Autowired;

import com.avanza.astrix.provider.core.AstrixServiceExport;

import tutorial.p3.api.LunchService;

@AstrixServiceExport(LunchService.class)
public class LunchServiceImpl implements LunchService {

	private GigaSpace gigaSpace;
	
	@Autowired
	public LunchServiceImpl(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	@Override
	public List<String> getAllRestaurants() {
		LunchRestaurant[] restaurants = gigaSpace.readMultiple(new LunchRestaurant());
		List<String> result = new ArrayList<>();
		for (LunchRestaurant restaurant : restaurants) {
			result.add(restaurant.getName());
		}
		return result;
	}

	@Override
	public void addLunchRestaurant(@Routing String name) {
		LunchRestaurant r = new LunchRestaurant();
		r.setName(name);
		gigaSpace.write(r);
	}
	

}
