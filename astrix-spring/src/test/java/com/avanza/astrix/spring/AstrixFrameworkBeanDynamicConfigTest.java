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
package com.avanza.astrix.spring;

import static com.avanza.astrix.context.AstrixContextTestUtil.getInternalInstance;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import org.junit.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.avanza.astrix.beans.config.AstrixConfig;
import com.avanza.astrix.config.DynamicConfig;
import com.avanza.astrix.config.MapConfigSource;
import com.avanza.astrix.context.AstrixContext;

public class AstrixFrameworkBeanDynamicConfigTest {

	@Configuration
	public static class ParentSpringConfiguration {
		@Bean
		public DynamicConfig dynamicConfig() {
			return DynamicConfig.create(MapConfigSource.of("key", "parent"));
		}
	}

	@Configuration
	public static class ExampleAppConfiguration {
		@Bean
		public AstrixFrameworkBean astrixFrameworkBean() {
			return new AstrixFrameworkBean();
		}
	}

	private final DynamicConfig customDynamicConfig = DynamicConfig.create(MapConfigSource.of("key", "custom"));

	private static DynamicConfig getDynamicConfig(AstrixContext astrixContext) {
		return getInternalInstance(astrixContext, AstrixConfig.class).getConfig();
	}

	@Test
	public void shouldGetDynamicConfigFromCustomSpringConfig() {
		// Arrange
		try (var c = new AnnotationConfigApplicationContext()) {
			c.registerBean(DynamicConfig.class, () -> customDynamicConfig);
			c.register(ExampleAppConfiguration.class);
			c.refresh();

			// Act
			String actual = getDynamicConfig(c.getBean(AstrixContext.class))
					.getStringProperty("key", "").get();

			// Assert
			assertEquals("custom", actual);
		}
	}

	@Test
	public void shouldGetDynamicConfigFromCustomSpringConfigThatAlsoHasParent() {
		// Arrange
		try (var parent = new AnnotationConfigApplicationContext(ParentSpringConfiguration.class)) {
			try (var c = new AnnotationConfigApplicationContext()) {
				c.setParent(parent);
				c.registerBean(DynamicConfig.class, () -> customDynamicConfig);
				c.register(ExampleAppConfiguration.class);
				c.refresh();

				// Act
				String actual = getDynamicConfig(c.getBean(AstrixContext.class))
						.getStringProperty("key", "").get();

				// Assert
				assertEquals("custom", actual);
			}
		}
	}

	@Test
	public void shouldGetDynamicConfigEvenIfSpringConfigDoesNotDefineIt() {
		// Arrange
		try (var c = new AnnotationConfigApplicationContext(ExampleAppConfiguration.class)) {

			// Act
			String actual = getDynamicConfig(c.getBean(AstrixContext.class))
					.getStringProperty("key", "").get();

			// Assert
			assertEquals("", actual);
		}
	}

	@Test
	public void shouldGetDynamicConfigFromParentContextIfSpringConfigDoesNotDefineIt() {
		// Arrange
		try (var parent = new AnnotationConfigApplicationContext(ParentSpringConfiguration.class)) {
			try (var c = new AnnotationConfigApplicationContext()) {
				c.setParent(parent);
				c.register(ExampleAppConfiguration.class);
				c.refresh();

				// Act
				String actual = getDynamicConfig(c.getBean(AstrixContext.class))
						.getStringProperty("key", "").get();

				// Assert
				assertEquals("parent", actual);
			}
		}
	}

	@Test
	public void shouldNotAllowMultipleDynamicConfigSpringBeans() {
		// Arrange
		try (var c = new AnnotationConfigApplicationContext()) {
			c.registerBean("config1", DynamicConfig.class, () -> customDynamicConfig);
			c.registerBean("config2", DynamicConfig.class, () -> customDynamicConfig);
			c.register(ExampleAppConfiguration.class);

			// Act
			var expected = assertThrows(IllegalArgumentException.class, c::refresh);

			// Assert
			assertThat(expected.getMessage(), containsString("Multiple DynamicConfig instances found in ApplicationContext"));
		}
	}
}
