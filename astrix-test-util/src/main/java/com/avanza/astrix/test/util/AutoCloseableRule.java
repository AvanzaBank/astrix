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
package com.avanza.astrix.test.util;

import java.util.LinkedList;
import java.util.List;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class AutoCloseableRule implements TestRule {

	private final List<AutoCloseable> autoClosables = new LinkedList<AutoCloseable>();
	
	@Override
	public Statement apply(final Statement base, Description description) {
		return new Statement() {
			@Override
			public void evaluate() throws Throwable {
				try {
					base.evaluate();
				} finally {
					for (AutoCloseable ac : autoClosables) {
						AstrixTestUtil.closeSafe(ac);
					}
				}
			}

		};
	}

	public <T extends AutoCloseable> T add(T autoClosable) {
		autoClosables.add(autoClosable);
		return autoClosable;
	}

}
