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
package runners;

import com.avanza.astrix.context.AstrixSettings;
import com.avanza.astrix.gs.test.util.PuApp;

public class LunchGraderPuRunner {
	
	public static void main(String[] args) {
		AstrixSettings externalConfig = new AstrixSettings();
		System.setProperty("com.gs.jini_lus.groups", Config.LOOKUP_GROUP_NAME);
		System.setProperty("externalConfigUri", externalConfig.getExternalConfigUri());
		PuApp.run("classpath:/META-INF/spring/lunch-grader-pu.xml");
	}

}
