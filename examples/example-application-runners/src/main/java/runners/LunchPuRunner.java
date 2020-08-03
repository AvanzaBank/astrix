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
package runners;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.gs.test.PuApp;

public class LunchPuRunner {
	
	public static void main(String[] args) {
		MapConfigSource mapConfigSource = new MapConfigSource();
		System.setProperty("com.gs.jini_lus.groups", Config.LOOKUP_GROUP_NAME);
		System.setProperty(AstrixSettings.EXPORT_ASTRIX_MBEANS.name(), "true");
		System.setProperty("configSourceId", GlobalConfigSourceRegistry.register(mapConfigSource));
		PuApp.run("classpath:/META-INF/spring/lunch-pu.xml");
		Logger.getLogger("com.avanza").setLevel(Level.DEBUG);
	}

}
