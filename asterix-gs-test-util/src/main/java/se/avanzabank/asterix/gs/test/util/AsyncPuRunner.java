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

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.openspaces.core.GigaSpace;

public class AsyncPuRunner implements PuRunner {

	private PuRunner puRunner;
	private final ExecutorService worker = Executors.newSingleThreadExecutor();
	
	public AsyncPuRunner(PuRunner puRunner) {
		this.puRunner = puRunner;
	}

	@Override
	public void run() throws Exception {
		worker.execute(new Runnable() {
			@Override
			public void run() {
				try {
					puRunner.run();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	@Override
	public void shutdown() throws Exception {
		Future<Void> shutdownComplete = worker.submit(new Callable<Void>() {
			@Override
			public Void call() throws Exception {
				puRunner.shutdown();
				return null;
			}
		});
		worker.shutdown();
		shutdownComplete.get();
	}

	@Override
	public String getLookupGroupName() {
		return puRunner.getLookupGroupName();
	}

	@Override
	public GigaSpace getClusteredGigaSpace() {
		return get(new Callable<GigaSpace>() {
			@Override
			public GigaSpace call() throws Exception {
				return puRunner.getClusteredGigaSpace();
			}
		});
	}
	
	private <T> T get(Callable<T> callable) {
		try {
			return worker.submit(callable).get();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
