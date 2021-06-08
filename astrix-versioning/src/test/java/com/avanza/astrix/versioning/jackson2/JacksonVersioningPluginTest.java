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
package com.avanza.astrix.versioning.jackson2;

import com.avanza.astrix.versioning.core.AstrixObjectSerializer;
import com.avanza.astrix.versioning.core.ObjectSerializerDefinition;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;


class JacksonVersioningPluginTest {
	
	@Test
	void serializesV2Objects() {
		AstrixObjectSerializer astrixObjectSerializer = new Jackson2SerializerPlugin().create(ObjectSerializerDefinition.versionedService(2, TestObjectMapperConfigurer.class));
		
		Object serialized = astrixObjectSerializer.serialize(new TestPojoV2("foo", "bar"), 2);
		TestPojoV2 deserializedPojo = astrixObjectSerializer.deserialize(serialized, TestPojoV2.class, 2);
		assertEquals("foo", deserializedPojo.getFoo());
		assertEquals("bar", deserializedPojo.getBar());
	}
	
	@Test
	void deserializesFromV1ObjectsByUpgrading() {
		AstrixObjectSerializer astrixObjectSerializer = new Jackson2SerializerPlugin().create(ObjectSerializerDefinition.versionedService(2, TestObjectMapperConfigurer.class));
		
		Object serializedV1 = astrixObjectSerializer.serialize(new TestPojoV1("foo"), 1);
		TestPojoV2 deserializedPojo = astrixObjectSerializer.deserialize(serializedV1, TestPojoV2.class, 1);
		assertEquals("foo", deserializedPojo.getFoo());
		assertEquals("defaultBar", deserializedPojo.getBar());
	}
	
	@Test
	void serializesToV1ObjectsByDowngrading() {
		AstrixObjectSerializer astrixObjectSerializer = new Jackson2SerializerPlugin().create(ObjectSerializerDefinition.versionedService(2, TestObjectMapperConfigurer.class));
		
		Object serializedV1 = astrixObjectSerializer.serialize(new TestPojoV2("foo", "bar"), 1); // bar will be removed during serialization
		TestPojoV2 deserializedPojo = astrixObjectSerializer.deserialize(serializedV1, TestPojoV2.class, 2);
		assertEquals("foo", deserializedPojo.getFoo());
		assertNull(deserializedPojo.getBar()); // bar is stripped during downgrade
	}
	
	public static class FakeDescriptor {
	}
	
	public static class TestObjectMapperConfigurer implements Jackson2ObjectSerializerConfigurer {

		@Override
		public List<? extends AstrixJsonApiMigration> apiMigrations() {
			return singletonList(new TestPojoV1ToV2Migration());
		}

		@Override
		public void configure(JacksonObjectMapperBuilder objectMapperBuilder) {
		}
	}
	
	public static class TestPojoV1ToV2Migration implements AstrixJsonApiMigration {
		@Override
		public AstrixJsonMessageMigration<?>[] getMigrations() {
			return new AstrixJsonMessageMigration<?>[] {
				new AstrixJsonMessageMigration<TestPojoV2>() {
					@Override
					public Class<TestPojoV2> getJavaType() {
						return TestPojoV2.class;
					}
					@Override
					public void upgrade(ObjectNode json) {
						json.put("bar", "defaultBar");
					}
					@Override
					public void downgrade(ObjectNode json) {
						json.remove("bar");
					}
				}
			};
		}
		@Override
		public int fromVersion() {
			return 1;
		}
	}

	public static class TestPojoV1 {
		private String foo;
		
		public TestPojoV1() {
		}
		
		public TestPojoV1(String foo) {
			this.foo = foo;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
		
		public String getFoo() {
			return foo;
		}
	}
	
	public static class TestPojoV2 {
		private String foo;
		private String bar;
		
		public TestPojoV2() {
		}
		
		public TestPojoV2(String foo, String bar) {
			this.foo = foo;
			this.bar = bar;
		}

		public void setFoo(String foo) {
			this.foo = foo;
		}
		
		public String getFoo() {
			return foo;
		}
		
		public void setBar(String bar) {
			this.bar = bar;
		}
		
		public String getBar() {
			return bar;
		}
	}

}
