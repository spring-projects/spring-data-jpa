/*
 * Copyright 2015-2024 the original author or authors.
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
package org.springframework.data.jpa.repository.procedures;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

import java.util.Date;
import java.util.UUID;

import org.hibernate.dialect.PostgreSQLDialect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Temporal;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.jpa.util.DisabledOnHibernate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.annotation.Transactional;

import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Testcase to verify {@link org.springframework.jdbc.object.StoredProcedure}s properly handle null values.
 *
 * @author Greg Turnquist
 */
@DisabledOnHibernate("6.1") // GH-2903
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = PostgresStoredProcedureNullHandlingIntegrationTests.Config.class)
class PostgresStoredProcedureNullHandlingIntegrationTests {

	@Autowired TestModelRepository repository;

	@Test // 2544
	void invokingNullOnNonTemporalStoredProcedureParameterShouldWork() {
		repository.countUuid(null);
	}

	@Test // 2544
	void invokingNullOnTemporalStoredProcedureParameterShouldWork() {
		repository.countLocalDate(null);
	}

	@Entity
	class TestModel {

		@Id
		@GeneratedValue(strategy = GenerationType.AUTO) //
		private long id;
		private UUID uuid;
		private Date date;

		public TestModel(long id, UUID uuid, Date date) {

			this.id = id;
			this.uuid = uuid;
			this.date = date;
		}

		protected TestModel() {}

		public long getId() {
			return this.id;
		}

		public UUID getUuid() {
			return this.uuid;
		}

		public Date getDate() {
			return this.date;
		}

		public void setId(long id) {
			this.id = id;
		}

		public void setUuid(UUID uuid) {
			this.uuid = uuid;
		}

		public void setDate(Date date) {
			this.date = date;
		}

		public String toString() {
			return "PostgresStoredProcedureNullHandlingIntegrationTests.TestModel(id=" + this.getId() + ", uuid="
					+ this.getUuid() + ", date=" + this.getDate() + ")";
		}
	}

	@Transactional
	public interface TestModelRepository extends JpaRepository<TestModel, Long> {

		@Procedure("countByUuid")
		void countUuid(UUID this_uuid);

		@Procedure("countByLocalDate")
		void countLocalDate(@Temporal Date this_local_date);
	}

	@EnableJpaRepositories(considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = TestModelRepository.class))
	@EnableTransactionManagement
	static class Config extends StoredProcedureConfigSupport {

		public Config() {
			super(PostgreSQLDialect.class, new ClassPathResource("scripts/postgres-nullable-stored-procedures.sql"));
		}

		@SuppressWarnings("resource")
		@Bean(initMethod = "start", destroyMethod = "stop")
		public PostgreSQLContainer<?> container() {

			return new PostgreSQLContainer<>("postgres:15.3") //
					.withUsername("postgres");
		}
	}
}
