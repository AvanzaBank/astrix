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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Test;

import se.avanzabank.asterix.context.AsterixEventLoggerPlugin;

import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixEventType;

public class HystrixEventPublisherTest {

	@Test
	public void test() {
		FakeEventLogger eventLogger = new FakeEventLogger();
		HystrixEventPublisher publisher = new HystrixEventPublisher(eventLogger);
		publisher.markEvent(HystrixEventType.SUCCESS, HystrixCommandKey.Factory.asKey("space_foo"));
		assertThat(eventLogger.incrementedEvents, contains("hystrix.space.foo.SUCCESS"));
	}
	

	static class FakeEventLogger implements AsterixEventLoggerPlugin {

		Collection<String> incrementedEvents = new ArrayList<String>();
		
		@Override
		public void increment(String event) {
			incrementedEvents.add(event);
		}

	}

}
