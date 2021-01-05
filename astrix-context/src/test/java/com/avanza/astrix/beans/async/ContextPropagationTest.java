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
package com.avanza.astrix.beans.async;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import org.junit.Test;
import com.avanza.astrix.core.function.CheckedCommand;

public class ContextPropagationTest {
	private final List<String> operations = new ArrayList<>();
	private final ContextPropagator propagator = new ContextPropagator() {
		@Override
		public <T> CheckedCommand<T> wrap(CheckedCommand<T> call) {
			return call;
		}

		@Override
		public Runnable wrap(Runnable runnable) {
			operations.add("wrap");
			return () -> {
				operations.add("before runnable");
				runnable.run();
				operations.add("after runnable");
			};
		}
	};
	private final ContextPropagation propagation = new ContextPropagation(Collections.singletonList(propagator));

	@Test
	public void shouldWrapRunnable() {
		// Arrange
		operations.add("before");
		final Runnable action = propagation.wrap(() -> {
			operations.add("action");
		});
		operations.add("after");

		// Act
		action.run();

		// Assert
		final List<String> expected = Arrays.asList(
				"before",
				"wrap",
				"after",
				"before runnable",
				"action",
				"after runnable"
		);
		assertEquals(expected, operations);
	}

	@Test
	public void shouldWrapConsumer() {
		// Arrange
		operations.add("before");
		final Consumer<String> consumer = propagation.wrap(v -> {
			operations.add("consumer: " + v);
		});
		operations.add("after");

		// Act
		consumer.accept("value");

		// Assert
		final List<String> expected = Arrays.asList(
				"before",
				"wrap",
				"after",
				"before runnable",
				"consumer: value",
				"after runnable"
		);
		assertEquals(expected, operations);
	}
}
