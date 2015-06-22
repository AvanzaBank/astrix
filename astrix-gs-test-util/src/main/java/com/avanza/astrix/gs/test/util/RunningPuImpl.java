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
package com.avanza.astrix.gs.test.util;

import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.openspaces.core.GigaSpace;

public class RunningPuImpl implements RunningPu {

	private final PuRunner runner;
	private volatile State state;
	
	enum State {
		NEW {
			@Override
			State start(PuRunner runner) throws Exception {
				runner.run();
				return RUNNING;
			}
			@Override
			State stop(PuRunner runner) {
				return CLOSED;
			}
		},
		RUNNING {
			@Override
			State start(PuRunner runner) {
				throw new IllegalStateException("Pu already running!");
			}
			@Override
			State stop(PuRunner runner) throws Exception {
				runner.shutdown();
				return CLOSED;
			}
		},
		CLOSED {
			@Override
			State start(PuRunner runner) throws Exception {
				runner.run();
				return RUNNING;
			}
			@Override
			State stop(PuRunner runner) {
				return CLOSED;
			}
		};
		abstract State start(PuRunner runner) throws Exception;
		abstract State stop(PuRunner runner) throws Exception;
	}
	
	public RunningPuImpl(PuRunner runner) {
		this.runner = runner;
		this.state = State.NEW;
	}

	@Override
	public Statement apply(final Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				System.setProperty("com.gs.jini_lus.groups", runner.getLookupGroupName());
				try {
					if (runner.autostart()) {
						start();
					}
					base.evaluate();
				} finally {
					try {
						stop();
					} catch (Exception e) {
						// Ignore
					}
				}
			}

		};
	}

	@Override
	public void close() throws Exception {
		stop();
	}
	
	@Override
	public synchronized void start() throws Exception {
		this.state = this.state.start(this.runner);
	}
	
	@Override
	public synchronized void stop() throws Exception {
		this.state = this.state.stop(this.runner);
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
