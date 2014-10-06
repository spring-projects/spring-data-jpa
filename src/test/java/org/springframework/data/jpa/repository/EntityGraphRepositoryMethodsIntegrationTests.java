/*
 * Copyright 2014 the original author or authors.
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
import org.springframework.data.jpa.repository.sample.RepositoryMethodsWithEntityGraphConfigJpaRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:config/namespace-autoconfig-context.xml")
@Transactional
public class EntityGraphRepositoryMethodsIntegrationTests {

	@Autowired EntityManager em;
	@Autowired RepositoryMethodsWithEntityGraphConfigJpaRepository repository;

	User tom;
	Role role;

	@Before
	public void setup() {

		tom = new User("Thomas", "Darimont", "tdarimont@example.org");
		role = new Role("Developer");
		em.persist(role);
		tom.getRoles().add(role);
	}

	/**
	 * @see DATAJPA-612
	 */
	@Test
	public void shouldRespectConfiguredJpaEntityGraph() {

		Assume.assumeTrue(currentEntityManagerIsAJpa21EntityManager(em));

		tom = repository.save(tom);

		List<User> result = repository.findAll();

		assertThat(result.size(), is(1));
		assertThat(Persistence.getPersistenceUtil().isLoaded(result.get(0).getRoles()), is(true));
		assertThat(result.get(0), is(tom));
	}
}
