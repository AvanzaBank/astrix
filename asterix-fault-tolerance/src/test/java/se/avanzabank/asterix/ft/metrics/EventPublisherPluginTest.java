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
package se.avanzabank.asterix.ft.metrics;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.hamcrest.Matchers;
import org.junit.Test;

import se.avanzabank.asterix.context.AsterixContext;
import se.avanzabank.asterix.context.AsterixEventLoggerPlugin;
import se.avanzabank.asterix.context.AsterixFaultTolerancePlugin;
import se.avanzabank.asterix.context.TestAsterixConfigurer;
import se.avanzabank.asterix.ft.service.SimpleService;
import se.avanzabank.asterix.ft.service.SimpleServiceImpl;

public class EventPublisherPluginTest {

	@Test
	public void eventNotified() throws Exception {
		TestAsterixConfigurer configurer = new TestAsterixConfigurer();
		configurer.enableFaultTolerance(true);
		FakeAsterixEventLoggerPlugin fakeEventLogger = new FakeAsterixEventLoggerPlugin();
		configurer.registerPlugin(AsterixEventLoggerPlugin.class, fakeEventLogger);
		AsterixContext context = configurer.configure();
		AsterixFaultTolerancePlugin faultTolerancePlugin = context.getPlugins().getPlugin(
				AsterixFaultTolerancePlugin.class);
		SimpleService ftService = faultTolerancePlugin.addFaultTolerance(SimpleService.class, new SimpleServiceImpl(), "foo");
		ftService.echo("");
		assertThat(fakeEventLogger.incrementedEvents, Matchers.contains("hystrix.foo.SimpleService.SUCCESS"));
	}

	static class FakeAsterixEventLoggerPlugin implements AsterixEventLoggerPlugin {

		private Collection<String> incrementedEvents = new ArrayList<>();

		@Override
		public void increment(String event) {
			incrementedEvents.add(event);
		}

	}
}
