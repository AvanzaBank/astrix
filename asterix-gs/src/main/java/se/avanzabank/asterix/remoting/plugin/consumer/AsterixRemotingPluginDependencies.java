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
package se.avanzabank.asterix.remoting.plugin.consumer;

import org.springframework.beans.factory.annotation.Autowired;

import se.avanzabank.asterix.context.ExternalDependencyBean;
import se.avanzabank.asterix.gs.SpaceUrlBuilder;

public class AsterixRemotingPluginDependencies implements ExternalDependencyBean {
	
	private final SpaceUrlBuilder spaceUrlBuilder;

	@Autowired
	public AsterixRemotingPluginDependencies(SpaceUrlBuilder spaceUrlBuilder) {
		this.spaceUrlBuilder = spaceUrlBuilder;
	}
	
	public SpaceUrlBuilder getSpaceUrlBuilder() {
		return spaceUrlBuilder;
	}
	
}
