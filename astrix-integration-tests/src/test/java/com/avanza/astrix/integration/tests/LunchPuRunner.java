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
package com.avanza.astrix.integration.tests;

import java.io.IOException;

import com.avanza.astrix.beans.core.AstrixSettings;
import com.avanza.astrix.config.GlobalConfigSourceRegistry;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.provider.component.AstrixServiceComponentNames;
import com.avanza.gs.test.PartitionedPu;
import com.avanza.gs.test.PuConfigurers;

public class LunchPuRunner {
	
	public static void main(String[] args) throws IOException {
		System.setProperty("com.gs.jini_lus.groups", "lunch-pu");
		MapConfigSource settings = new MapConfigSource();
		settings.set(AstrixSettings.SERVICE_REGISTRY_URI, AstrixServiceComponentNames.GS_REMOTING + ":jini://*/*/service-registry-space?groups=service-registry");
		PartitionedPu partitionedPu = new PartitionedPu(PuConfigurers.partitionedPu("classpath:/META-INF/spring/lunch-pu.xml")
				.numberOfPrimaries(1)
				.contextProperty("configSourceId", GlobalConfigSourceRegistry.register(settings))
				.numberOfBackups(0));
		partitionedPu.run();
	}
	
}
