/*
 * Copyright 2025-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.aot;

import static jakarta.persistence.GenerationType.IDENTITY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.data.jpa.repository.aot.AotMetamodel.initProperties;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.util.Map;

import org.hibernate.jpa.boot.internal.PersistenceUnitInfoDescriptor;
import org.junit.jupiter.api.Test;
import org.springframework.data.jpa.repository.aot.AotMetamodel.NoOpConnectionProvider;
import org.springframework.data.jpa.repository.aot.AotMetamodel.SpringDataJpaAotDialect;
import org.springframework.orm.jpa.persistenceunit.SpringPersistenceUnitInfo;

/**
 * Unit tests for {@link AotMetamodel}.
 *
 * @author Mark Paluch
 * @author Christoph Strobl
 */
class AotMetamodelUnitTests {

	@Test // GH-4103
	void dialectSupportsSequences() {
		assertThat(AotMetamodel.SpringDataJpaAotDialect.INSTANCE.getSequenceSupport().supportsSequences()).isTrue();
		assertThat(AotMetamodel.SpringDataJpaAotDialect.INSTANCE.getSequenceSupport().supportsPooledSequences()).isTrue();
	}

	@Test // GH-4092
	void initializesPropertiesWithDefaults() {

		assertThat(initProperties(Map.of())) //
				.containsEntry("hibernate.dialect", SpringDataJpaAotDialect.INSTANCE) //
				.containsEntry("hibernate.boot.allow_jdbc_metadata_access", false) //
				.containsEntry("hibernate.connection.provider_class", NoOpConnectionProvider.INSTANCE) //
				.containsEntry("hibernate.jpa_callbacks.enabled", false) //
				.containsEntry("hibernate.query.startup_check", false);
	}

	@Test // GH-4092
	void allowsDialectOverridesViaProperties() {

		assertThat(initProperties(Map.of("hibernate.dialect", "H2Dialect", "jpa.bla.bla", "42")))
				.containsEntry("hibernate.dialect", "H2Dialect") //
				.containsEntry("jpa.bla.bla", "42");
	}

	@Test // GH-4092, GH-4130
	void preventsPropertyOverrides/* for cases we know cause trouble */() {

		assertThat(initProperties(Map.of(//
				"hibernate.boot.allow_jdbc_metadata_access", "true", //
				"hibernate.connection.provider_class", "DatasourceConnectionProviderImpl", //
				"hibernate.jpa_callbacks.enabled", "true", //
				"hibernate.query.startup_check", "true", //
				"jakarta.persistence.schema-generation.database.action", "create-drop")))
				.containsEntry("hibernate.boot.allow_jdbc_metadata_access", false) //
				.containsEntry("hibernate.connection.provider_class", NoOpConnectionProvider.INSTANCE) //
				.containsEntry("hibernate.jpa_callbacks.enabled", false) //
				.containsEntry("hibernate.query.startup_check", false) //
				.containsEntry("jakarta.persistence.schema-generation.database.action", "none");
	}

	@Test // GH-4207
	void springAotDialectDoesNotFailOnSelfReferencingEntity() {

		SpringPersistenceUnitInfo persistenceUnitInfo = new SpringPersistenceUnitInfo(this.getClass().getClassLoader());
		persistenceUnitInfo.setPersistenceUnitName("AotMetamodel");
		persistenceUnitInfo.addManagedClassName(EntityEntity.class.getName());
		PersistenceUnitInfoDescriptor persistenceUnit = new PersistenceUnitInfoDescriptor(
				persistenceUnitInfo.asStandardPersistenceUnitInfo());

		assertThatNoException().isThrownBy(() -> {

			EntityManagerFactory emf = AotMetamodel.init(() -> persistenceUnit, Map.of()).get();
			emf.getMetamodel().managedType(EntityEntity.class);
		});
	}

	@Entity
	@Table(name = "entity")
	public static class EntityEntity {

		@Id
		@GeneratedValue(strategy = IDENTITY) private int id;

		@ManyToOne(fetch = FetchType.LAZY)
		@JoinTable(name = "entity_relationship", joinColumns = @JoinColumn(name = "child_entity_id"),
				inverseJoinColumns = @JoinColumn(name = "parent_entity_id"))

		private EntityEntity parentEntity;

		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public EntityEntity getParentEntity() {
			return parentEntity;
		}

		public void setParentEntity(EntityEntity parentEntity) {
			this.parentEntity = parentEntity;
		}
	}

}
