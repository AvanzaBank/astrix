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
package com.avanza.astrix.context.versioning;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.avanza.astrix.beans.service.ObjectSerializerDefinition;
import com.avanza.astrix.core.AstrixObjectSerializer;
import com.avanza.astrix.provider.versioning.AstrixJsonApiMigration;
import com.avanza.astrix.provider.versioning.AstrixJsonMessageMigration;
import com.avanza.astrix.provider.versioning.AstrixVersioned;
import com.avanza.astrix.provider.versioning.JacksonObjectMapperBuilder;
import com.avanza.astrix.versioning.plugin.Jackson1ObjectSerializerConfigurer;

public class JacksonVersioningPluginTest {
	
	@Test
	public void serializesV2Objects() throws Exception {
		AstrixObjectSerializer astrixObjectSerializer = new Jackson1SerializerPlugin().create(ObjectSerializerDefinition.versionedService(2, TestObjectMapperConfigurer.class));
		
		Object serialized = astrixObjectSerializer.serialize(new TestPojoV2("foo", "bar"), 2);
		TestPojoV2 deserializedPojo = astrixObjectSerializer.deserialize(serialized, TestPojoV2.class, 2);
		assertEquals("foo", deserializedPojo.getFoo());
		assertEquals("bar", deserializedPojo.getBar());
	}
	
	@Test
	public void deserializesFromV1ObjectsByUpgrading() throws Exception {
		AstrixObjectSerializer astrixObjectSerializer = new Jackson1SerializerPlugin().create(ObjectSerializerDefinition.versionedService(2, TestObjectMapperConfigurer.class));
		
		Object serializedV1 = astrixObjectSerializer.serialize(new TestPojoV1("foo"), 1);
		TestPojoV2 deserializedPojo = astrixObjectSerializer.deserialize(serializedV1, TestPojoV2.class, 1);
		assertEquals("foo", deserializedPojo.getFoo());
		assertEquals("defaultBar", deserializedPojo.getBar());
	}
	
	@Test
	public void serializesToV1ObjectsByDowngrading() throws Exception {
		AstrixObjectSerializer astrixObjectSerializer = new Jackson1SerializerPlugin().create(ObjectSerializerDefinition.versionedService(2, TestObjectMapperConfigurer.class));
		
		Object serializedV1 = astrixObjectSerializer.serialize(new TestPojoV2("foo", "bar"), 1); // bar will be removed during serialization
		TestPojoV2 deserializedPojo = astrixObjectSerializer.deserialize(serializedV1, TestPojoV2.class, 2);
		assertEquals("foo", deserializedPojo.getFoo());
		assertEquals(null, deserializedPojo.getBar()); // bar is stripped during downgrade
	}
	
	@AstrixVersioned(
		version = 2,
		objectSerializerConfigurer = TestObjectMapperConfigurer.class
	)
	public static class FakeDescriptor {
	}
	
	public static class TestObjectMapperConfigurer implements Jackson1ObjectSerializerConfigurer {

		@Override
		public List<? extends AstrixJsonApiMigration> apiMigrations() {
			return Arrays.asList(new TestPojoV1ToV2Migration());
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
