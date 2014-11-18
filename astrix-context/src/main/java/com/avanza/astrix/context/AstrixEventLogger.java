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
public final class AstrixEventLogger implements AstrixPluginsAware {
	
	private final List<AstrixEventLoggerPlugin> eventLoggers = new CopyOnWriteArrayList<>();
	
	public AstrixEventLogger() {
	}
	
	public AstrixEventLogger(AstrixEventLoggerPlugin... loggers) {
		this.eventLoggers.addAll(Arrays.asList(loggers));
	}
	
	public void increment(String event) {
		for (AstrixEventLoggerPlugin eventLogger : eventLoggers) {
			eventLogger.increment(event);
		}
	}

	@Override
	public void setPlugins(AstrixPlugins plugins) {
		this.eventLoggers.addAll(plugins.getPlugins(AstrixEventLoggerPlugin.class));
	}

}
