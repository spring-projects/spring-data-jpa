/*
 * Copyright 2014-2018 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;

import org.junit.Assume;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for JPA 2.1 stored procedure support.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @since 1.6
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryStoredProcedureTests {

	@Autowired UserRepository repository;
	@PersistenceContext EntityManager em;

	@Test // DATAJPA-455
	public void callProcedureWithInAndOutParameters() {

		assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		assertThat(repository.plus1inout(1), is(2));
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
}
