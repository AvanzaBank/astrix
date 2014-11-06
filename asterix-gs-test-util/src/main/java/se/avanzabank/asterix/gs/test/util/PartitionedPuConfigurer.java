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
package se.avanzabank.asterix.gs.test.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

public final class PartitionedPuConfigurer {
	
	String puXmlPath;
	int numberOfPrimaries = 1;
	int numberOfBackups = 0;
	boolean startAsync = false;
	Properties contextProperties = new Properties();
	Map<String, Properties> beanProperies = new HashMap<>();
	String lookupGroupName = UUID.randomUUID().toString();
	

	public PartitionedPuConfigurer(String puXmlPath) {
		this.puXmlPath = puXmlPath;
	}

	public PartitionedPuConfigurer numberOfPrimaries(int numberOfPrimaries) {
		this.numberOfPrimaries = numberOfPrimaries;
		return this;
	}

	public PartitionedPuConfigurer numberOfBackups(int numberOfBackups) {
		this.numberOfBackups = numberOfBackups;
		return this;
	}

	public PartitionedPuConfigurer beanProperties(String beanName, Properties beanProperties) {
		this.beanProperies.put(beanName, beanProperties);
		return this;
	}

	public PartitionedPuConfigurer startAsync(boolean startAsync) {
		this.startAsync  = startAsync;
		return this;
	}

	public PartitionedPuConfigurer lookupGroup(String group) {
		this.lookupGroupName = group;
		return this;
	}

	public RunningPu configure() {
		if (startAsync) {
			return new RunningPuImpl(new AsyncPuRunner(new PartitionedPu(this)));
		}
		return new RunningPuImpl(new PartitionedPu(this));
	}

	public PartitionedPuConfigurer contextProperties(Properties properties) {
		this.contextProperties = properties;
		return this;
	}
	
	public PartitionedPuConfigurer contextProperty(String name, String value) {
		this.contextProperties.setProperty(name, value);
		return this;
	}

}
