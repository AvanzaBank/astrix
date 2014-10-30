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

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openspaces.core.GigaSpace;

public class RunningPuImpl implements RunningPu {

	private PuRunner runner;
	
	public RunningPuImpl(PuRunner runner) {
		this.runner = runner;
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				System.setProperty("com.gs.jini_lus.groups", runner.getLookupGroupName());
				try {
					runner.run();
					base.evaluate();
				} finally {
					try {
						runner.shutdown();
					} catch (Exception e) {
						// Ignore
					}
				}
			}

		};
	}

	@Override
	public void close() throws Exception {
		this.runner.shutdown();
	}

	@Override
	public String getLookupGroupName() {
		return this.runner.getLookupGroupName();
	}

	@Override
	public GigaSpace getClusteredGigaSpace() {
		return this.runner.getClusteredGigaSpace();
	}

}
