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
package se.avanzabank.service.suite.provider.versioning;

import org.codehaus.jackson.node.ObjectNode;

/**
 * 
 * @author Elias Lindholm (elilin)
 *
 * @param <T>
 */
public interface AstrixJsonMessageMigration<T> {
	
	/**
	 * The java type representing the given json message.
	 * 
	 * @return
	 */
	Class<T> getJavaType();
	
	/**
	 * Upgrades a given json message to the next version. <p>
	 * @param json
	 */
	void upgrade(ObjectNode json);
	
	/**
	 * Downgrades a given json-message to this version (fromVersion()), from 
	 * the next version. <p>	
	 * 
	 * @param json
	 */
	void downgrade(ObjectNode json);
	
	

}
