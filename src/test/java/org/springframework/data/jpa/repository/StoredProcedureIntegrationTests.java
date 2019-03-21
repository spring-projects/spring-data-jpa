/*
 * Copyright 2015-2019 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan.Filter;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.jpa.domain.sample.Dummy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.DummyRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @see scripts/schema-stored-procedures.sql for procedure definitions.
 */
@Transactional
@ContextConfiguration(classes = StoredProcedureIntegrationTests.TestConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class StoredProcedureIntegrationTests {

	private static final String NOT_SUPPORTED = "Stored procedures with ResultSets are currently not supported for any JPA provider";

	@PersistenceContext EntityManager em;
	@Autowired DummyRepository repository;

	@Configuration
	@EnableJpaRepositories(basePackageClasses = DummyRepository.class, includeFilters = { @Filter(
			pattern = ".*DummyRepository", type = FilterType.REGEX) })
	static abstract class Config {}

	@ImportResource("classpath:infrastructure.xml")
	static class TestConfig extends Config {}

	@Before
	public void setup() {
		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));
	}

	@Test // DATAJPA-652
	public void shouldExecuteAdHocProcedureWithNoInputAnd1OutputParameter() {
		assertThat(repository.adHocProcedureWithNoInputAnd1OutputParameter(), is(42));
	}

	@Test // DATAJPA-652
	public void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameter() {
		assertThat(repository.adHocProcedureWith1InputAnd1OutputParameter(23), is(24));
	}

	@Test // DATAJPA-652
	public void shouldExecuteAdHocProcedureWith1InputAndNoOutputParameter() {
		repository.adHocProcedureWith1InputAndNoOutputParameter(42);
	}

	@Test // DATAJPA-652
	@Ignore(NOT_SUPPORTED)
	public void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameterWithResultSet() {

		List<Dummy> dummies = repository.adHocProcedureWith1InputAnd1OutputParameterWithResultSet("FOO");

		assertThat(dummies, is(notNullValue()));
		assertThat(dummies.size(), is(equalTo(3)));
	}

	@Test // DATAJPA-652
	@Ignore(NOT_SUPPORTED)
	public void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate() {

		List<Dummy> dummies = repository.adHocProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate("FOO");

		assertThat(dummies, is(notNullValue()));
		assertThat(dummies.size(), is(equalTo(3)));
	}

	@Test // DATAJPA-652
	public void shouldExecuteAdHocProcedureWith1InputAnd1OutputParameterWithUpdate() {
		repository.adHocProcedureWith1InputAndNoOutputParameterWithUpdate("FOO");
	}

	@Test // DATAJPA-652
	public void shouldExecuteProcedureWithNoInputAnd1OutputParameter() {
		assertThat(repository.procedureWithNoInputAnd1OutputParameter(), is(42));
	}

	@Test // DATAJPA-652
	public void shouldExecuteProcedureWith1InputAnd1OutputParameter() {
		assertThat(repository.procedureWith1InputAnd1OutputParameter(23), is(24));
	}

	@Test // DATAJPA-652
	public void shouldExecuteProcedureWith1InputAndNoOutputParameter() {
		repository.procedureWith1InputAndNoOutputParameter(42);
	}

	@Test // DATAJPA-652
	@Ignore(NOT_SUPPORTED)
	public void shouldExecuteProcedureWith1InputAnd1OutputParameterWithResultSet() {

		List<Dummy> dummies = repository.procedureWith1InputAnd1OutputParameterWithResultSet("FOO");

		assertThat(dummies, is(notNullValue()));
		assertThat(dummies.size(), is(equalTo(3)));
	}

	@Test // DATAJPA-652
	@Ignore(NOT_SUPPORTED)
	public void shouldExecuteProcedureWith1InputAnd1OutputParameterWithResultSetWithUpdate() {

		List<Dummy> dummies = repository.procedureWith1InputAnd1OutputParameterWithResultSetWithUpdate("FOO");

		assertThat(dummies, is(notNullValue()));
		assertThat(dummies.size(), is(equalTo(3)));
	}

	@Test // DATAJPA-652
	public void shouldExecuteProcedureWith1InputAnd1OutputParameterWithUpdate() {
		repository.procedureWith1InputAndNoOutputParameterWithUpdate("FOO");
	}
}
