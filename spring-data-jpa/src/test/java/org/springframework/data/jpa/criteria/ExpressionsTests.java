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
package org.springframework.data.jpa.criteria;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.core.TypedPropertyPath;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * Integration tests for {@link Expressions}.
 *
 * @author Mark Paluch
 */
@SpringJUnitConfig(locations = "classpath:hibernate-h2-infrastructure.xml")
class ExpressionsTests {

	@Autowired EntityManager entityManager;

	@BeforeEach
	void setUp() {
		System.setProperty("spring.data.lambda-reader.filter-stacktrace", "false");
	}

	@Test // GH-4085
	void shouldResolveTopLevelExpression() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Expression<String> expression = Expressions.get(from, User::getFirstname);
		assertThat(expression.getJavaType()).isEqualTo(String.class);
	}

	@Test // GH-4085
	void shouldResolveNestedLevelExpression() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Expression<Role> expression = Expressions.get(from,
				TypedPropertyPath.of(User::getManager).thenMany(User::getRoles));
		assertThat(expression.getJavaType()).isEqualTo(Role.class);
	}

	@Test // GH-4085
	void shouldSelectExpression() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Path<?> path = (Path<?>) Expressions.select(from, User::getFirstname);

		assertThat(path.getJavaType()).isEqualTo(String.class);
		assertThat(path.getParentPath()).isEqualTo(from);
	}

	@Test // GH-4085
	void shouldSelectJoinedExpression() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Path<?> path = (Path<?>) Expressions.select(from, TypedPropertyPath.of(User::getManager).then(User::getLastname));

		assertThat(path.getParentPath()).isInstanceOf(Join.class);
	}

	@Test // GH-4085
	void shouldJoin() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Join<User, User> join = Expressions.join(from, User::getManager);

		assertThat(join.getJavaType()).isEqualTo(User.class);
		assertThat(join.getAttribute().getName()).isEqualTo("manager");
	}

	@Test // GH-4085
	void shouldJoinWithType() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Join<User, User> join = Expressions.join(from, JoinType.LEFT, it -> it.join(User::getManager));

		assertThat(join.getJavaType()).isEqualTo(User.class);
		assertThat(join.getJoinType()).isEqualTo(JoinType.LEFT);
	}

	@Test // GH-4085
	void shouldFetch() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Fetch<User, User> fetch = Expressions.fetch(from, User::getManager);

		assertThat(fetch.getAttribute().getName()).isEqualTo("manager");
	}

	@Test // GH-4085
	void shouldFetchWithType() {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<User> from = query.from(User.class);

		Fetch<User, User> fetch = Expressions.fetch(from, JoinType.LEFT, it -> it.fetch(User::getManager));

		assertThat(fetch.getAttribute().getName()).isEqualTo("manager");
		assertThat(fetch.getJoinType()).isEqualTo(JoinType.LEFT);
	}

}
