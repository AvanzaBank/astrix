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
package lunch.grader.api;

import java.util.List;

import com.avanza.asterix.core.AsterixRemoteResult;
import com.avanza.asterix.core.AsterixRemoteResultReducer;

public class LunchRestaurantGradeReducer implements AsterixRemoteResultReducer<LunchRestaurantGrade, LunchRestaurantGrade> {

	@Override
	public LunchRestaurantGrade reduce(List<AsterixRemoteResult<LunchRestaurantGrade>> grades) {
		LunchRestaurantGrade result = null;
		
		for (AsterixRemoteResult<LunchRestaurantGrade> grade : grades) {
			if (grade.getResult() == null) {
				continue;
			}
			if (result == null || grade.getResult().avarageGrade() > result.avarageGrade()) {
				result = grade.getResult();
			}
		}
		return result;
	}

}