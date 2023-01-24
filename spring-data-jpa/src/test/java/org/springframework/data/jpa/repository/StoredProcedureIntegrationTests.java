/*
 * Copyright 2015-2023 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.currentEntityManagerIsAJpa21EntityManager;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Dummy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.DummyRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Gabriel Basilio
 * @author Krzysztof Krason
 * @see scripts/schema-stored-procedures.sql for procedure definitions.
 */
@Transactional
@ContextConfiguration(classes = StoredProcedureIntegrationTests.TestConfig.class)
@ExtendWith(SpringExtension.class)
class StoredProcedureIntegrationTests {

	private static final String NOT_SUPPORTED = "Stored procedures with REF_CURSOR are currently not supported by HSQL dialect";

	@PersistenceContext EntityManager em;
	@Autowired DummyRepository repository;

	@BeforeEach
	void setup() {
		assumeThat(currentEntityManagerIsAJpa21EntityManager(em)).isTrue();
	}

	@Test // DATAJPA-652
	void shouldExecuteAdHocProcedureWithNoInputAnd1OutputParameter() {
		assertThat(repository.adHocProcedureWithNoInputAnd1OutputParameter()).isEqualTo(42);
	}

	@Test // DATAJPA-652
	void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameter() {
		assertThat(repository.adHocProcedureWith1InputAnd1OutputParameter(23)).isEqualTo(24);
	}

	@Test // DATAJPA-652
	void shouldExecuteAdHocProcedureWith1InputAndNoOutputParameter() {
		repository.adHocProcedureWith1InputAndNoOutputParameter(42);
	}

	@Test // DATAJPA-652
	@Disabled(NOT_SUPPORTED)
	void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameterWithResultSet() {

		List<Dummy> dummies = repository.adHocProcedureWith1InputAnd1OutputParameterWithResultSet("FOO");

		assertThat(dummies).isNotNull();
		assertThat(dummies).hasSize(3);
	}

	@Test // DATAJPA-652
	@Disabled(NOT_SUPPORTED)
	void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate() {

		List<Dummy> dummies = repository.adHocProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate("FOO");

		assertThat(dummies).isNotNull();
		assertThat(dummies).hasSize(3);
	}

	@Test // DATAJPA-652
	void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameterWithUpdate() {
		repository.adHocProcedureWith1InputAndNoOutputParameterWithUpdate("FOO");
	}

	@Test // DATAJPA-652
	void shouldExecuteProcedureWithNoInputAnd1OutputParameter() {
		assertThat(repository.procedureWithNoInputAnd1OutputParameter()).isEqualTo(42);
	}

	@Test // DATAJPA-652
	void shouldExecuteProcedureWith1InputAnd1OutputParameter() {
		assertThat(repository.procedureWith1InputAnd1OutputParameter(23)).isEqualTo(24);
	}

	@Test // DATAJPA-652
	void shouldExecuteProcedureWith1InputAndNoOutputParameter() {
		repository.procedureWith1InputAndNoOutputParameter(42);
	}

	@Test // DATAJPA-652
	@Disabled(NOT_SUPPORTED)
	void shouldExecuteProcedureWith1InputAnd1OutputParameterWithResultSet() {

		List<Dummy> dummies = repository.procedureWith1InputAnd1OutputParameterWithResultSet("FOO");

		assertThat(dummies).isNotNull();
		assertThat(dummies).hasSize(3);
	}

	@Test // DATAJPA-652
	@Disabled(NOT_SUPPORTED)
	void shouldExecuteProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate() {

		List<Dummy> dummies = repository.procedureWith1InputAnd1OutputParameterWithResultSetWithUpdate("FOO");

		assertThat(dummies).isNotNull();
		assertThat(dummies).hasSize(3);
	}

	@Test // DATAJPA-652
	void shouldExecuteProcedureWith1InputAnd1OutputParameterWithUpdate() {
		repository.procedureWith1InputAndNoOutputParameterWithUpdate("FOO");
	}

	@Configuration
	@EnableJpaRepositories(basePackageClasses = DummyRepository.class,
			includeFilters = { @Filter(pattern = ".*DummyRepository", type = FilterType.REGEX) })
	static abstract class Config {}

	@ImportResource("classpath:infrastructure.xml")
	static class TestConfig extends Config {}
}
