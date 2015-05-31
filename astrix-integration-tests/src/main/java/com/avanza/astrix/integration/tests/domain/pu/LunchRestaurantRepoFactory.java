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

import org.openspaces.core.GigaSpace;
import org.springframework.beans.factory.annotation.Autowired;

public class LunchRestaurantRepoFactory {

	/*
	 * This factory is used to make sure that Astrix supports application-context-xml files
	 * containing factory beans which does not provide a className for the target bean.
	 */
	
	private GigaSpace gigaSpace;
	
	@Autowired
	public LunchRestaurantRepoFactory(GigaSpace gigaSpace) {
		this.gigaSpace = gigaSpace;
	}

	public LunchRestaurantRepo create() {
		return new LunchRestaurantRepo(gigaSpace);
	}
}
