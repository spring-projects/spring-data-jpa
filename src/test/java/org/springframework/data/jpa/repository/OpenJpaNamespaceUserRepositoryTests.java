/*
 * Copyright 2008-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.ParameterExpression;
import javax.persistence.criteria.Root;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;

/**
 * Testcase to run {@link UserRepository} integration tests on top of OpenJPA.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ContextConfiguration("classpath:openjpa.xml")
public class OpenJpaNamespaceUserRepositoryTests extends NamespaceUserRepositoryTests {

	@PersistenceContext EntityManager em;

	@Test
	public void checkQueryValidationWithOpenJpa() {

		assertThatThrownBy(() -> em.createQuery("something absurd"))
		.isInstanceOf(RuntimeException.class);

		assertThatThrownBy(() -> em.createNamedQuery("not available"))
		.isInstanceOf(RuntimeException.class);

	}

	/**
	 * Test case for https://issues.apache.org/jira/browse/OPENJPA-2018
	 */
	@SuppressWarnings({ "rawtypes" })
	@Test
	@Ignore
	public void queryUsingIn() {

		flushTestUsers();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<User> criteriaQuery = builder.createQuery(User.class);
		Root<User> root = criteriaQuery.from(User.class);
		ParameterExpression<Collection> parameter = builder.parameter(Collection.class);
		criteriaQuery.where(root.<Integer> get("id").in(parameter));

		TypedQuery<User> query = em.createQuery(criteriaQuery);
		query.setParameter(parameter, Arrays.asList(1, 2));

		List<User> resultList = query.getResultList();
		assertThat(resultList.size()).isEqualTo(2);
	}

	/**
	 * Temporarily ignored until openjpa works with hsqldb 2.x.
	 */
	@Override
	public void shouldFindUsersInNativeQueryWithPagination() {}
}
