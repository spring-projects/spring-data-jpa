/*
 * Copyright 2008-2018 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;
import javax.persistence.metamodel.Metamodel;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.ExtensionAwareEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit test for {@link SimpleJpaQuery}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class SimpleJpaQueryUnitTests {

	static final String USER_QUERY = "select u from User u";
	static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private static final EvaluationContextProvider EVALUATION_CONTEXT_PROVIDER = new ExtensionAwareEvaluationContextProvider();

	JpaQueryMethod method;

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock QueryExtractor extractor;
	@Mock javax.persistence.Query query;
	@Mock TypedQuery<Long> typedQuery;
	@Mock RepositoryMetadata metadata;
	@Mock ParameterBinder binder;
	@Mock Metamodel metamodel;

	ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	public @Rule ExpectedException exception = ExpectedException.none();

	@Before
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUp() throws SecurityException, NoSuchMethodException {

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.createQuery(anyString())).thenReturn(query);
		when(em.createQuery(anyString(), eq(Long.class))).thenReturn(typedQuery);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(em.getDelegate()).thenReturn(em);
		when(emf.createEntityManager()).thenReturn(em);
		when(metadata.getDomainType()).thenReturn((Class) User.class);
		when(metadata.getReturnedDomainClass(Mockito.any(Method.class))).thenReturn((Class) User.class);

		Method setUp = UserRepository.class.getMethod("findByLastname", String.class);
		method = new JpaQueryMethod(setUp, metadata, factory, extractor);
	}

	@Test
	public void prefersDeclaredCountQueryOverCreatingOne() throws Exception {

		method = new JpaQueryMethod(SimpleJpaQueryUnitTests.class.getMethod("prefersDeclaredCountQueryOverCreatingOne"),
				metadata, factory, extractor);
		when(em.createQuery("foo", Long.class)).thenReturn(typedQuery);

		SimpleJpaQuery jpaQuery = new SimpleJpaQuery(method, em, "select u from User u", EVALUATION_CONTEXT_PROVIDER,
				PARSER);

		assertThat(jpaQuery.createCountQuery(new Object[] {}), is((javax.persistence.Query) typedQuery));
	}

	@Test // DATAJPA-77
	public void doesNotApplyPaginationToCountQuery() throws Exception {

		when(em.createQuery(Mockito.anyString())).thenReturn(query);

		Method method = UserRepository.class.getMethod("findAllPaged", Pageable.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);

		AbstractJpaQuery jpaQuery = new SimpleJpaQuery(queryMethod, em, "select u from User u", EVALUATION_CONTEXT_PROVIDER,
				PARSER);
		jpaQuery.createCountQuery(new Object[] { PageRequest.of(1, 10) });

		verify(query, times(0)).setFirstResult(anyInt());
		verify(query, times(0)).setMaxResults(anyInt());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void discoversNativeQuery() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);
		AbstractJpaQuery jpaQuery = JpaQueryFactory.INSTANCE.fromQueryAnnotation(queryMethod, em,
				EVALUATION_CONTEXT_PROVIDER);

		assertThat(jpaQuery instanceof NativeJpaQuery, is(true));

		when(em.createNativeQuery(anyString(), eq(User.class))).thenReturn(query);
		when(metadata.getReturnedDomainClass(method)).thenReturn((Class) User.class);

		jpaQuery.createQuery(new Object[] { "Matthews" });

		verify(em).createNativeQuery("SELECT u FROM User u WHERE u.lastname = ?1", User.class);
	}

	@Test(expected = InvalidJpaQueryMethodException.class) // DATAJPA-554
	public void rejectsNativeQueryWithDynamicSort() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class, Sort.class);
		createJpaQuery(method);
	}

	@Test // DATAJPA-352
	@SuppressWarnings("unchecked")
	public void doesNotValidateCountQueryIfNotPagingMethod() throws Exception {

		Method method = SampleRepository.class.getMethod("findByAnnotatedQuery");
		when(em.createQuery(Mockito.contains("count"))).thenThrow(IllegalArgumentException.class);

		createJpaQuery(method);
	}

	@Test // DATAJPA-352
	@SuppressWarnings("unchecked")
	public void validatesAndRejectsCountQueryIfPagingMethod() throws Exception {

		Method method = SampleRepository.class.getMethod("pageByAnnotatedQuery", Pageable.class);

		when(em.createQuery(Mockito.contains("count"))).thenThrow(IllegalArgumentException.class);
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Count");
		exception.expectMessage(method.getName());

		createJpaQuery(method);
	}

	@Test
	public void createsASimpleJpaQueryFromAnnotation() throws Exception {

		RepositoryQuery query = createJpaQuery(SampleRepository.class.getMethod("findByAnnotatedQuery"));
		assertThat(query instanceof SimpleJpaQuery, is(true));
	}

	@Test
	public void createsANativeJpaQueryFromAnnotation() throws Exception {

		RepositoryQuery query = createJpaQuery(SampleRepository.class.getMethod("findNativeByLastname", String.class));
		assertThat(query instanceof NativeJpaQuery, is(true));
	}

	@Test // DATAJPA-757
	public void createsNativeCountQuery() throws Exception {

		when(em.createNativeQuery(anyString())).thenReturn(query);

		AbstractJpaQuery jpaQuery = createJpaQuery(
				UserRepository.class.getMethod("findUsersInNativeQueryWithPagination", Pageable.class));

		jpaQuery.doCreateCountQuery(new Object[] { PageRequest.of(0, 10) });

		verify(em).createNativeQuery(anyString());
	}

	@Test // DATAJPA-885
	public void projectsWithManuallyDeclaredQuery() throws Exception {

		AbstractJpaQuery jpaQuery = createJpaQuery(SampleRepository.class.getMethod("projectWithExplicitQuery"));

		jpaQuery.createQuery(new Object[0]);

		verify(em, times(0)).createQuery(anyString(), eq(Tuple.class));

		// Two times, first one is from the query validation
		verify(em, times(2)).createQuery(anyString());
	}

	private AbstractJpaQuery createJpaQuery(Method method) {

		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);
		return JpaQueryFactory.INSTANCE.fromQueryAnnotation(queryMethod, em, EVALUATION_CONTEXT_PROVIDER);
	}

	interface SampleRepository {

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?1", nativeQuery = true)
		List<User> findNativeByLastname(String lastname);

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?1", nativeQuery = true)
		List<User> findNativeByLastname(String lastname, Sort sort);

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?1", nativeQuery = true)
		List<User> findNativeByLastname(String lastname, Pageable pageable);

		@Query(USER_QUERY)
		List<User> findByAnnotatedQuery();

		@Query(USER_QUERY)
		Page<User> pageByAnnotatedQuery(Pageable pageable);

		@Query("select u from User u")
		Collection<UserProjection> projectWithExplicitQuery();
	}

	interface UserProjection {}
}
