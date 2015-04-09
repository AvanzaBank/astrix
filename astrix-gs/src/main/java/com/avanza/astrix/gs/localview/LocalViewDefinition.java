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
package com.avanza.astrix.gs.localview;

import org.openspaces.core.space.cache.LocalViewSpaceConfigurer;

import com.j_spaces.core.client.SQLQuery;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public interface LocalViewDefinition {

	/**
	 * See {@link LocalViewSpaceConfigurer#addViewQuery(SQLQuery)}
	 * 
	 * @param sqlQuery
	 */
	void addViewQuery(SQLQuery<?> sqlQuery);
	
	/**
	 * See {@link LocalViewSpaceConfigurer#batchSize(int)}
	 * @param size
	 */
	void setBatchSize(int size);
	
	/**
	 * See {@link LocalViewSpaceConfigurer#maxDisconnectionDuration(long)}
	 * @param size
	 */
	void setMaxDisconnectionDuration(long maxDisconnectionDuration);

}
