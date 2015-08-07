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
package com.avanza.astrix.versioning.jackson1;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import com.avanza.astrix.versioning.jackson1.AstrixJsonApiMigration;
import com.avanza.astrix.versioning.jackson1.AstrixJsonMessageMigration;
import com.avanza.astrix.versioning.jackson1.VersionedJsonObjectMapper;
import com.avanza.astrix.versioning.jackson1.VersionedJsonObjectMapper.VersionedObjectMapperBuilder;
import com.google.common.reflect.TypeToken;



public class VersionedJsonObjectMapperTest {
	
	List<AstrixJsonApiMigration> apiMigrations = new ArrayList<>();
	
	@Test
	public void canSerializerAndDeserializeObjectsUsingObjectMapper() throws Exception {
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(apiMigrations);
		VersionedJsonObjectMapper objectMapper = objectMapperBuilder.build();
		
		String json = objectMapper.serialize(new TestPojoV1("kalle"), 1);
		
		TestPojoV1 foo = objectMapper.deserialize(json, TestPojoV1.class, 1);
		assertEquals("kalle", foo.getFoo());
	}
	
	@Test
	public void upgradesOldDocuments() throws Exception {
		this.apiMigrations.add(new TestPojoV1ToV2Migration());
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(apiMigrations);
		VersionedJsonObjectMapper objectMapper = objectMapperBuilder.build();
		
		TestPojoV1 testPojo = new TestPojoV1("kalle");
		String pojoJsonV1 = objectMapper.serialize(testPojo, 1);
		
		TestPojoV1 v1Pojo = objectMapper.deserialize(pojoJsonV1, TestPojoV1.class, 1);
		TestPojoV2 v2Pojo = objectMapper.deserialize(pojoJsonV1, TestPojoV2.class, 1);
		assertEquals("kalle", v1Pojo.getFoo());
		assertEquals("kalle", v2Pojo.getFoo());
		assertEquals("defaultBar", v2Pojo.getBar());
	}
	

	@Test
	public void downgradesToOldDocuments() throws Exception {
		this.apiMigrations.add(new TestPojoV1ToV2Migration());
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(apiMigrations);
		VersionedJsonObjectMapper objectMapper = objectMapperBuilder.build();
		
		TestPojoV2 testPojo = new TestPojoV2();
		testPojo.setBar("b1");
		testPojo.setFoo("f1");
		String pojoJsonV2 = objectMapper.serialize(testPojo, 1);
		
		TestPojoV1 v1Pojo = objectMapper.deserialize(pojoJsonV2, TestPojoV1.class, 1);
		assertEquals("f1", v1Pojo.getFoo());
	}
	
	@Test
	@SuppressWarnings("serial")
	public void deserializesGenericTypes() throws Exception {
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(apiMigrations);
		VersionedJsonObjectMapper objectMapper = objectMapperBuilder.build();
		
		TestPojoV1 testPojo1 = new TestPojoV1("p1");
		List<TestPojoV1> testPojos = new ArrayList<>();
		testPojos.add(testPojo1);
		String jsonPojo = objectMapper.serialize(testPojos, 1);
		
		TypeToken<List<TestPojoV1>> genericListType = new TypeToken<List<TestPojoV1>>() {};
		
		List<TestPojoV1> deserializedPojos = objectMapper.deserialize(jsonPojo, genericListType.getType(), 1);
		assertEquals(1, deserializedPojos.size());
		assertEquals("p1", deserializedPojos.get(0).getFoo());
	}

	@Test
	@SuppressWarnings("serial")
	public void migratesGenericTypes() throws Exception {
		apiMigrations.add(new TestPojoV1ToV2Migration());
		VersionedObjectMapperBuilder objectMapperBuilder = new VersionedObjectMapperBuilder(apiMigrations);
		VersionedJsonObjectMapper objectMapper = objectMapperBuilder.build();
		
		TestPojoV1 testPojo1 = new TestPojoV1("p1");
		List<TestPojoV1> testPojos = new ArrayList<>();
		testPojos.add(testPojo1);
		String jsonPojo = objectMapper.serialize(testPojos, 1);
		
		TypeToken<List<TestPojoV2>> genericListType = new TypeToken<List<TestPojoV2>>() {};
		
		List<TestPojoV2> deserializedPojos = objectMapper.deserialize(jsonPojo, genericListType.getType(), 1);
		assertEquals(1, deserializedPojos.size());
		assertEquals("p1", deserializedPojos.get(0).getFoo());
		assertEquals("defaultBar", deserializedPojos.get(0).getBar());
	}
	
	private final class TestPojoV1ToV2Migration implements AstrixJsonApiMigration {
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
