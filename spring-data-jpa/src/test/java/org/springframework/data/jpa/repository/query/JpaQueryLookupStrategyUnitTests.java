/*
 * Copyright 2012-2021 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.NamedQueries;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;

/**
 * Unit tests for {@link JpaQueryLookupStrategy}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Réda Housni Alaoui
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class JpaQueryLookupStrategyUnitTests {

	private static final QueryMethodEvaluationContextProvider EVALUATION_CONTEXT_PROVIDER = QueryMethodEvaluationContextProvider.DEFAULT;

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock QueryExtractor extractor;
	@Mock NamedQueries namedQueries;
	@Mock Metamodel metamodel;
	@Mock ProjectionFactory projectionFactory;

	private JpaQueryMethodFactory queryMethodFactory;

	@BeforeEach
	void setUp() {

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);
		when(em.getDelegate()).thenReturn(em);
		queryMethodFactory = new DefaultJpaQueryMethodFactory(extractor);
	}

	@Test // DATAJPA-226
	void invalidAnnotatedQueryCausesException() throws Exception {

		QueryLookupStrategy strategy = JpaQueryLookupStrategy.create(em, queryMethodFactory, Key.CREATE_IF_NOT_FOUND,
				EVALUATION_CONTEXT_PROVIDER, EscapeCharacter.DEFAULT);
		Method method = UserRepository.class.getMethod("findByFoo", String.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		Throwable reference = new RuntimeException();
		when(em.createQuery(anyString())).thenThrow(reference);

		assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> strategy.resolveQuery(method, metadata, projectionFactory, namedQueries))
				.withCause(reference);
	}

	@Test // DATAJPA-554
	void sholdThrowMorePreciseExceptionIfTryingToUsePaginationInNativeQueries() throws Exception {

		QueryLookupStrategy strategy = JpaQueryLookupStrategy.create(em, queryMethodFactory, Key.CREATE_IF_NOT_FOUND,
				EVALUATION_CONTEXT_PROVIDER, EscapeCharacter.DEFAULT);
		Method method = UserRepository.class.getMethod("findByInvalidNativeQuery", String.class, Sort.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		assertThatExceptionOfType(InvalidJpaQueryMethodException.class)
				.isThrownBy(() -> strategy.resolveQuery(method, metadata, projectionFactory, namedQueries))
				.withMessageContaining("Cannot use native queries with dynamic sorting in method")
				.withMessageContaining(method.toString());
	}

	@Test // GH-2217
	void considersNamedCountQuery() throws Exception {

		QueryLookupStrategy strategy = JpaQueryLookupStrategy.create(em, queryMethodFactory, Key.CREATE_IF_NOT_FOUND,
				EVALUATION_CONTEXT_PROVIDER, EscapeCharacter.DEFAULT);

		when(namedQueries.hasQuery("foo.count")).thenReturn(true);
		when(namedQueries.getQuery("foo.count")).thenReturn("foo count");

		when(namedQueries.hasQuery("User.findByNamedQuery")).thenReturn(true);
		when(namedQueries.getQuery("User.findByNamedQuery")).thenReturn("select foo");

		Method method = UserRepository.class.getMethod("findByNamedQuery", String.class, Pageable.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		RepositoryQuery repositoryQuery = strategy.resolveQuery(method, metadata, projectionFactory, namedQueries);
		assertThat(repositoryQuery).isInstanceOf(SimpleJpaQuery.class);
		SimpleJpaQuery query = (SimpleJpaQuery) repositoryQuery;
		assertThat(query.getQuery().getQueryString()).isEqualTo("select foo");
		assertThat(query.getCountQuery().getQueryString()).isEqualTo("foo count");
	}

	@Test // GH-2217
	void considersNamedCountOnStringQueryQuery() throws Exception {

		QueryLookupStrategy strategy = JpaQueryLookupStrategy.create(em, queryMethodFactory, Key.CREATE_IF_NOT_FOUND,
				EVALUATION_CONTEXT_PROVIDER, EscapeCharacter.DEFAULT);

		when(namedQueries.hasQuery("foo.count")).thenReturn(true);
		when(namedQueries.getQuery("foo.count")).thenReturn("foo count");

		Method method = UserRepository.class.getMethod("findByStringQueryWithNamedCountQuery", String.class,
				Pageable.class);
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		RepositoryQuery repositoryQuery = strategy.resolveQuery(method, metadata, projectionFactory, namedQueries);
		assertThat(repositoryQuery).isInstanceOf(SimpleJpaQuery.class);
		SimpleJpaQuery query = (SimpleJpaQuery) repositoryQuery;
		assertThat(query.getCountQuery().getQueryString()).isEqualTo("foo count");
	}

	@Test // GH-2319
	void prefersDeclaredQuery() throws Exception {

		QueryLookupStrategy strategy = JpaQueryLookupStrategy.create(em, queryMethodFactory, Key.CREATE_IF_NOT_FOUND,
				EVALUATION_CONTEXT_PROVIDER, EscapeCharacter.DEFAULT);
		Method method = UserRepository.class.getMethod("annotatedQueryWithQueryAndQueryName");
		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		RepositoryQuery repositoryQuery = strategy.resolveQuery(method, metadata, projectionFactory, namedQueries);

		assertThat(repositoryQuery).isInstanceOf(AbstractStringBasedJpaQuery.class);
	}

	interface UserRepository extends Repository<User, Integer> {

		@Query("something absurd")
		User findByFoo(String foo);

		@Query(value = "select u.* from User u", nativeQuery = true)
		List<User> findByInvalidNativeQuery(String param, Sort sort);

		@Query(countName = "foo.count")
		Page<User> findByNamedQuery(String foo, Pageable pageable);

		@Query(value = "foo.query", countName = "foo.count")
		Page<User> findByStringQueryWithNamedCountQuery(String foo, Pageable pageable);

		@Query(value = "something absurd", name = "my-query-name")
		User annotatedQueryWithQueryAndQueryName();
	}
}
