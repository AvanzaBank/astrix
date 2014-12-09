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
package lunch.grader.api.provider;

import lunch.grader.api.LunchRestaurantGrader;

import com.avanza.astrix.provider.core.AstrixServiceProvider;
import com.avanza.astrix.provider.core.AstrixServiceRegistryLookup;
import com.avanza.astrix.provider.versioning.AstrixVersioned;

@AstrixVersioned(
	version = 1,
	objectSerializerConfigurer = LunchGradeApiObjectMapperConfigurer.class
)
@AstrixServiceRegistryLookup
@AstrixServiceProvider({
	LunchRestaurantGrader.class,
	PublicLunchFeeder.class
})
public class LunchGradeApiDescriptor {
}


