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
package se.avanzabank.asterix.ft;

import static org.junit.Assert.*;
import static org.hamcrest.Matchers.*;

import org.junit.Test;

import se.avanzabank.asterix.ft.plugin.HystrixFaultTolerancePlugin;
import se.avanzabank.asterix.ft.service.SimpleService;
import se.avanzabank.asterix.ft.service.SimpleServiceImpl;

public class FaultToleranceIntegrationTest {

	private HystrixFaultTolerancePlugin plugin = new HystrixFaultTolerancePlugin();
	private Class<SimpleService> api = SimpleService.class;
	private SimpleService provider = new SimpleServiceImpl();

	@Test
	public void callFtService() {
		SimpleService serviceWithFt = plugin.addFaultTolerance(api , provider, "test");
		assertThat(serviceWithFt.echo("foo"), is(equalTo("foo")));
	}

}
