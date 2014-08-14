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
package se.avanzabank.service.suite.versioning;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codehaus.jackson.node.ObjectNode;

import se.avanzabank.service.suite.provider.versioning.AstrixJsonMessageMigration;
/**
 * A message migrator is responsible for migration a message on an old version
 * to the current version, and also responsible for migration messages on the
 * current version to an older version. <p>
 * 
 * @author Elias Lindholm (elilin)
 *
 */
class JsonMessageMigrator<T> {
	
	private Class<T> type;
	private List<JsonMessageMigrationWithVersion<T>> migrationsInOrder;
	private List<JsonMessageMigrationWithVersion<T>> migrationsInReverseOrder;
	
	public JsonMessageMigrator(Class<T> type,
							   List<JsonMessageMigrationWithVersion<T>> migrations) {
		this.type = type;
		this.migrationsInOrder = new ArrayList<>(migrations);
		Collections.sort(this.migrationsInOrder);
		this.migrationsInReverseOrder = new ArrayList<>(migrationsInOrder);
		Collections.reverse(this.migrationsInReverseOrder);
		
	}

	public void upgrade(ObjectNode json, int fromVersion) {
		for (JsonMessageMigrationWithVersion<T> migration : migrationsInOrder) {
			if (migration.getVersion() < fromVersion) {
				continue;
			}
			migration.upgrade(json);
		}
	}
	
	public void downgrade(ObjectNode json, int toVersion) {
		for (JsonMessageMigrationWithVersion<T> migration : migrationsInReverseOrder) {
			if (!(migration.getVersion() >= toVersion)) {
				return;
			}
			migration.downgrade(json);
		}
	}

	public Class<T> getJavaType() {
		return type;
	}
	
	static class JsonMessageMigrationWithVersion<T> implements Comparable<JsonMessageMigrationWithVersion<T>> {
		private int version;
		private AstrixJsonMessageMigration<T> migration;

		public JsonMessageMigrationWithVersion(int version,
				AstrixJsonMessageMigration<T> migration) {
			this.version = version;
			this.migration = migration;
		}
		
		public int getVersion() {
			return version;
		}
		
		void downgrade(ObjectNode json) {
			this.migration.downgrade(json);
		}
		
		void upgrade(ObjectNode json) {
			this.migration.upgrade(json);
		}

		@Override
		public int compareTo(JsonMessageMigrationWithVersion<T> other) {
			return getVersion() - other.getVersion();
		}
		
	}
	
	static class Builder<T> {
		private Class<T> javaType;
		private List<JsonMessageMigrationWithVersion<T>> migrations = new ArrayList<>();

		public Builder(Class<T> javaType) {
			this.javaType = javaType;
		}
		
		public Builder<T> addMigration(AstrixJsonMessageMigration<T> migration, int fromVersion) {
			migrations.add(new JsonMessageMigrationWithVersion<>(fromVersion, migration));
			return this;
		}
		
		public JsonMessageMigrator<T> build() {
			return new JsonMessageMigrator<>(javaType, migrations);
		}
		
	}
	
}
