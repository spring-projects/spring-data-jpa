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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import javax.persistence.ParameterMode;
import javax.persistence.PersistenceContext;
import javax.persistence.StoredProcedureQuery;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for JPA 2.1 stored procedure support.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Jeff Sheets
 * @author Jens Schauder
 * @since 1.6
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryStoredProcedureIntegrationTests {

	@Autowired
	UserRepository repository;
	@PersistenceContext
	EntityManager em;

	@Test // DATAJPA-455
	public void callProcedureWithInAndOutParameters() {

		assertThat(repository.plus1inout(1)).isEqualTo(2);
	}

	@Test // DATAJPA-707
	public void callProcedureWithInAndOutParametersInvalidOutParamName() {

		assertThat(repository.plus1inoutInvalidOutParamName(1)).isEqualTo(2);
	}

	@Test // DATAJPA-455
	public void callProcedureExplicitNameWithInAndOutParameters() {

		assertThat(repository.explicitlyNamedPlus1inout(1)).isEqualTo(2);
	}

	@Test // DATAJPA-455
	public void entityAnnotatedCustomNamedProcedurePlus1IO() {

		assertThat(repository.entityAnnotatedCustomNamedProcedurePlus1IO(1)).isEqualTo(2);
	}

	@Test // DATAJPA-707
	public void entityAnnotatedCustomNamedProcedurePlus1IOInvalidOutParamName() {

		assertThatThrownBy( //
				() -> repository.entityAnnotatedCustomNamedProcedurePlus1IOInvalidOutParamName(1)) //
				.isInstanceOf(InvalidDataAccessApiUsageException.class) //
				.hasMessageContaining("Could not locate parameter registered under that name");
	}

	@Test // DATAJPA-707
	public void entityAnnotatedCustomNamedProcedurePlus1IO2TwoOutParamsButNamingOne() {

		assertThat(repository.entityAnnotatedCustomNamedProcedurePlus1IO2TwoOutParamsButNamingOne(1)).isEqualTo(3);
	}

	@Test // DATAJPA-707
	public void entityAnnotatedCustomNamedProcedurePlus1IO2() {

		Map<String, Integer> result = repository.entityAnnotatedCustomNamedProcedurePlus1IO2(1);

		assertThat(result).containsExactly(entry("res", 2), entry("res2", 3));
	}

	@Test // DATAJPA-455
	public void plainJpa21() {

		StoredProcedureQuery proc = em.createStoredProcedureQuery("plus1inout");
		proc.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		proc.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);

		proc.setParameter(1, 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue(2)).isEqualTo(2);
	}

	@Test // DATAJPA-455
	public void plainJpa21_entityAnnotatedCustomNamedProcedurePlus1IO() {

		StoredProcedureQuery proc = em.createNamedStoredProcedureQuery("User.plus1IO");

		proc.setParameter("arg", 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue("res")).isEqualTo(2);
	}

	@Test // DATAJPA-707
	public void plainJpa21_twoOutParams() {

		StoredProcedureQuery proc = em.createStoredProcedureQuery("plus1inout2");
		proc.registerStoredProcedureParameter(1, Integer.class, ParameterMode.IN);
		proc.registerStoredProcedureParameter(2, Integer.class, ParameterMode.OUT);
		proc.registerStoredProcedureParameter(3, Integer.class, ParameterMode.OUT);

		proc.setParameter(1, 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue(2)).isEqualTo(2);
		assertThat(proc.getOutputParameterValue(3)).isEqualTo(3);
	}

	@Test // DATAJPA-707
	public void plainJpa21_entityAnnotatedCustomNamedProcedurePlus1IO2() {

		StoredProcedureQuery proc = em.createNamedStoredProcedureQuery("User.plus1IO2");

		proc.setParameter("arg", 1);
		proc.execute();

		assertThat(proc.getOutputParameterValue("res")).isEqualTo(2);
		assertThat(proc.getOutputParameterValue("res2")).isEqualTo(3);
	}
}
