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

import static org.junit.Assert.assertTrue;

import org.hamcrest.Description;
import org.junit.Test;

import com.avanza.astrix.test.util.Poller;
import com.avanza.astrix.test.util.Probe;

public class AstrixBeanStateWorkerTest {
	
	@Test
	public void workerIsInterruptedWhenContextDestroyed() throws Exception {
		AstrixContextImpl context = new TestAstrixConfigurer().configure();
		final AstrixBeanStateWorker worker = context.getInstance(AstrixBeanStateWorker.class);
		worker.start();
		assertTrue(worker.isAlive());
		context.destroy();
		new Poller(2000, 10).check(new Probe() {
			@Override
			public boolean isSatisfied() {
				return !worker.isAlive(); 
			}
			@Override
			public void sample() {
			}
			@Override
			public void describeFailureTo(Description description) {
				description.appendText("AstrixBeanStateWorker should stop when context destroyed");
			}
		});
	}

}
