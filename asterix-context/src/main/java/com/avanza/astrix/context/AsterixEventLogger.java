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
package com.avanza.astrix.context;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
/**
 * 
 * @author Elias Lindholm (elilin)
 *
 */
public final class AsterixEventLogger implements AsterixPluginsAware {
	
	private final List<AsterixEventLoggerPlugin> eventLoggers = new CopyOnWriteArrayList<>();
	
	public AsterixEventLogger() {
	}
	
	public AsterixEventLogger(AsterixEventLoggerPlugin... loggers) {
		this.eventLoggers.addAll(Arrays.asList(loggers));
	}
	
	public void increment(String event) {
		for (AsterixEventLoggerPlugin eventLogger : eventLoggers) {
			eventLogger.increment(event);
		}
	}

	@Override
	public void setPlugins(AsterixPlugins plugins) {
		this.eventLoggers.addAll(plugins.getPlugins(AsterixEventLoggerPlugin.class));
	}

}
