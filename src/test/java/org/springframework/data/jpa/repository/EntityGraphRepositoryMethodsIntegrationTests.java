/*
 * Copyright 2014-2015 the original author or authors.
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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.jpa.support.EntityManagerTestUtils.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Persistence;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.RepositoryMethodsWithEntityGraphConfigRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for RepositoryMethodsWithEntityGraphConfigJpaRepository.
 * 
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:config/namespace-autoconfig-context.xml")
@Transactional
public class EntityGraphRepositoryMethodsIntegrationTests {

	@Autowired EntityManager em;
	@Autowired RepositoryMethodsWithEntityGraphConfigRepository repository;

	User tom;
	User ollie;
	Role role;

	@Before
	public void setup() {

		tom = new User("Thomas", "Darimont", "tdarimont@example.org");
		ollie = new User("Oliver", "Gierke", "ogierke@example.org");

		role = new Role("Developer");
		em.persist(role);
		tom.getRoles().add(role);
		tom = repository.save(tom);

		ollie = repository.save(ollie);
		tom.getColleagues().add(ollie);
	}

	/**
	 * @see DATAJPA-612
	 */
	@Test
	public void shouldRespectConfiguredJpaEntityGraph() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		List<User> result = repository.findAll();

		assertThat(result.size(), is(2));
		assertThat(Persistence.getPersistenceUtil().isLoaded(result.get(0).getRoles()), is(true));
		assertThat(result.get(0), is(tom));
	}

	/**
	 * @see DATAJPA-689
	 */
	@Test
	public void shouldRespectConfiguredJpaEntityGraphInFindOne() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		User user = repository.findOne(tom.getId());

		assertThat(user, is(notNullValue()));
		assertThat("colleages should be fetched with 'user.detail' fetchgraph",
				Persistence.getPersistenceUtil().isLoaded(user.getColleagues()), is(true));
	}

	/**
	 * @see DATAJPA-696
	 */
	@Test
	public void shouldRespectInferFetchGraphFromMethodName() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		User user = repository.getOneWithDefinedEntityGraphById(tom.getId());

		assertThat(user, is(notNullValue()));
		assertThat("colleages should be fetched with 'user.detail' fetchgraph",
				Persistence.getPersistenceUtil().isLoaded(user.getColleagues()), is(true));
	}

	/**
	 * @see DATAJPA-696
	 */
	@Test
	public void shouldRespectDynamicFetchGraphForGetOneWithAttributeNamesById() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		User user = repository.getOneWithAttributeNamesById(tom.getId());

		assertThat(user, is(notNullValue()));
		assertThat("colleages should be fetched with 'user.detail' fetchgraph",
				Persistence.getPersistenceUtil().isLoaded(user.getColleagues()), is(true));
	}
}
