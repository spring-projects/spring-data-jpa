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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.type;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Fetch;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Selection;

import java.util.function.Function;

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
 * @author Christoph Strobl
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

		QueryExpression<User, Expression<String>> qe = expression(User.class,
				from -> Expressions.get(from, User::getFirstname));

		assertThat(qe.expression().getJavaType()).isEqualTo(String.class);
	}

	@Test // GH-4085
	void shouldResolveNestedLevelExpression() {

		QueryExpression<User, Expression<Role>> qe = expression(User.class,
				from -> Expressions.get(from, TypedPropertyPath.of(User::getManager).thenMany(User::getRoles)));
		assertThat(qe.expression().getJavaType()).isEqualTo(Role.class);
	}

	@Test // GH-4085
	void shouldSelectExpression() {

		QueryExpression<User, Selection<String>> path = expression(User.class,
				from -> Expressions.select(from, User::getFirstname));

		assertThat(path.expression().getJavaType()).isEqualTo(String.class);
		assertThat(path.expression()).asInstanceOf(type(Path.class)).extracting(Path::getParentPath).isEqualTo(path.root());
	}

	@Test // GH-4085
	void shouldSelectJoinedExpression() {

		QueryExpression<User, Selection<String>> path = expression(User.class,
				from -> Expressions.select(from, TypedPropertyPath.of(User::getManager).then(User::getLastname)));

		assertThat(path.expression()).asInstanceOf(type(Path.class)).extracting(Path::getParentPath)
				.isInstanceOf(Join.class);
	}

	@Test // GH-4085
	void shouldJoin() {

		QueryExpression<User, Join<User, User>> join = expression(User.class,
				from -> Expressions.join(from, User::getManager));

		assertThat(join.expression().getJavaType()).isEqualTo(User.class);
		assertThat(join.expression().getAttribute().getName()).isEqualTo("manager");
	}

	@Test // GH-4085
	void shouldJoinWithType() {

		QueryExpression<User, Join<User, User>> join = expression(User.class,
				from -> Expressions.join(from, JoinType.LEFT, it -> it.join(User::getManager)));

		assertThat(join.expression().getJavaType()).isEqualTo(User.class);
		assertThat(join.expression().getJoinType()).isEqualTo(JoinType.LEFT);
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

	<T, R, S extends Selection<R>> QueryExpression<T, S> expression(Class<T> type, Function<Root<T>, S> callable) {

		CriteriaBuilder cb = entityManager.getCriteriaBuilder();
		CriteriaQuery<Object> query = cb.createQuery();
		Root<T> from = query.from(type);

		QueryExpression<T, S> qe = new QueryExpression<>(from, callable.apply(from));

		entityManager.createQuery(query); // validate the query
		return qe;

	}

	private record QueryExpression<T, S>(Root<T> root, S expression) {
	}

}
