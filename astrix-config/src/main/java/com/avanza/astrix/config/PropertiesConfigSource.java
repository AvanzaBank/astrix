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
package com.avanza.astrix.config;

import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public class PropertiesConfigSource implements ConfigSource {

	private static final Logger log = LoggerFactory.getLogger(PropertiesConfigSource.class);
	private final Properties properties;

	public PropertiesConfigSource(Properties properties) {
		this.properties = Objects.requireNonNull(properties);
	}

	@Override
	public String get(String propertyName) {
		return properties.getProperty(propertyName);
	}

	public static ConfigSource optionalClasspathPropertiesFile(String fileName) {
		Properties classpathOverride = new Properties();
		try {
			InputStream resourceAsStream = PropertiesConfigSource.class.getClassLoader().getResourceAsStream(fileName);
			if (resourceAsStream == null) {
				log.info("Optional config properties file not present on classpath: " + fileName + "");
				return new PropertiesConfigSource(classpathOverride);
			}
			classpathOverride.load(resourceAsStream);
		} catch (Exception e) {
			log.warn("Failed to load config properties from file: " + fileName);
		}
		return new PropertiesConfigSource(classpathOverride);
	}
	
	public String toString() {
		return "PropertiesConfigSource: " + this.properties.toString();
	}
}
