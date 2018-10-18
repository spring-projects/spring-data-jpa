/*
 * Copyright 2014-2019 the original author or authors.
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
import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import javax.persistence.*;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

/**
 * Integration tests for JPA 2.1 stored procedure support.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jeff Sheets
 * @since 1.6
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryStoredProcedureTests {

	@Autowired UserRepository repository;
	@PersistenceContext EntityManager em;

	@Rule public ExpectedException exception = ExpectedException.none();

	@Test // DATAJPA-455
	public void callProcedureWithInAndOutParameters() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		assertThat(repository.plus1inout(1), is(2));
	}

	@Test // DATAJPA-707
	public void callProcedureWithInAndOutParametersInvalidOutParamName() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		assertThat(repository.plus1inoutInvalidOutParamName(1), is(2));
	}

	@Test // DATAJPA-455
	public void callProcedureExplicitNameWithInAndOutParameters() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		assertThat(repository.explicitlyNamedPlus1inout(1), is(2));
	}

	@Test // DATAJPA-455
	public void entityAnnotatedCustomNamedProcedurePlus1IO() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		assertThat(repository.entityAnnotatedCustomNamedProcedurePlus1IO(1), is(2));
	}

	@Test // DATAJPA-707
	public void entityAnnotatedCustomNamedProcedurePlus1IOInvalidOutParamName() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		exception.expect(InvalidDataAccessApiUsageException.class);
		exception.expectMessage(containsString("Could not locate parameter registered under that name"));

		repository.entityAnnotatedCustomNamedProcedurePlus1IOInvalidOutParamName(1);
	}

	@Test // DATAJPA-707
	public void entityAnnotatedCustomNamedProcedurePlus1IO2TwoOutParamsButNamingOne() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		assertThat(repository.entityAnnotatedCustomNamedProcedurePlus1IO2TwoOutParamsButNamingOne(1), is(3));
	}

	@Test // DATAJPA-707
	public void entityAnnotatedCustomNamedProcedurePlus1IO2() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		Map<String, Integer> result = repository.entityAnnotatedCustomNamedProcedurePlus1IO2(1);
		assertThat(result, hasEntry("res", 2));
		assertThat(result, hasEntry("res2", 3));
		assertThat(result.size(), is(2));
	}

	@Test // DATAJPA-455
	@Ignore
	public void plainJpa21() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		StoredProcedureQuery proc = em.createStoredProcedureQuery("plus1inout");
		proc.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		proc.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);

		proc.setParameter(1, 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue(2), is((Object) 2));
	}

	@Test // DATAJPA-455
	@Ignore
	public void plainJpa21_entityAnnotatedCustomNamedProcedurePlus1IO() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		StoredProcedureQuery proc = em.createNamedStoredProcedureQuery("User.plus1IO");

		proc.setParameter("arg", 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue("res"), is((Object) 2));
	}

	@Test // DATAJPA-707
	@Ignore
	public void plainJpa21_twoOutParams() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		StoredProcedureQuery proc = em.createStoredProcedureQuery("plus1inout2");
		proc.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		proc.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
		proc.registerStoredProcedureParameter(3, Integer.class, ParameterMode.OUT);

		proc.setParameter(1, 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue(2), is((Object) 2));
		assertThat(proc.getOutputParameterValue(3), is((Object) 3));
	}

	@Test // DATAJPA-707
	@Ignore
	public void plainJpa21_entityAnnotatedCustomNamedProcedurePlus1IO2() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		StoredProcedureQuery proc = em.createNamedStoredProcedureQuery("User.plus1IO2");

		proc.setParameter("arg", 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue("res"), is((Object) 2));
		assertThat(proc.getOutputParameterValue("res2"), is((Object) 3));
	}
}
