/*
 * Copyright 2008-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Metamodel;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryRewriter;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethodEvaluationContextProvider;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.util.TypeInformation;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * Unit test for {@link SimpleJpaQuery}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Tom Hombergs
 * @author Mark Paluch
 * @author Greg Turnquist
 * @author Krzysztof Krason
 * @author Erik Pellizzon
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SimpleJpaQueryUnitTests {

	private static final String USER_QUERY = "select u from User u";
	private static final SpelExpressionParser PARSER = new SpelExpressionParser();
	private static final QueryMethodEvaluationContextProvider EVALUATION_CONTEXT_PROVIDER = QueryMethodEvaluationContextProvider.DEFAULT;

	private JpaQueryMethod method;

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock QueryExtractor extractor;
	@Mock jakarta.persistence.Query query;
	@Mock TypedQuery<Long> typedQuery;
	@Mock RepositoryMetadata metadata;
	@Mock ParameterBinder binder;
	@Mock Metamodel metamodel;

	private ProjectionFactory factory = new SpelAwareProxyProjectionFactory();

	@BeforeEach
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void setUp() throws SecurityException, NoSuchMethodException {

		when(em.getMetamodel()).thenReturn(metamodel);
		when(em.createQuery(anyString())).thenReturn(query);
		when(em.createQuery(anyString(), eq(Long.class))).thenReturn(typedQuery);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(em.getDelegate()).thenReturn(em);
		when(emf.createEntityManager()).thenReturn(em);
		when(metadata.getDomainType()).thenReturn((Class) User.class);
		when(metadata.getReturnedDomainClass(Mockito.any(Method.class))).thenReturn((Class) User.class);
		when(metadata.getReturnType(Mockito.any(Method.class)))
				.thenAnswer(invocation -> TypeInformation.fromReturnTypeOf(invocation.getArgument(0)));

		Method setUp = UserRepository.class.getMethod("findByLastname", String.class);
		method = new JpaQueryMethod(setUp, metadata, factory, extractor);
	}

	@Test
	void prefersDeclaredCountQueryOverCreatingOne() throws Exception {

		method = new JpaQueryMethod(
				SimpleJpaQueryUnitTests.class.getDeclaredMethod("prefersDeclaredCountQueryOverCreatingOne"), metadata, factory,
				extractor);
		when(em.createQuery("foo", Long.class)).thenReturn(typedQuery);

		SimpleJpaQuery jpaQuery = new SimpleJpaQuery(method, em, "select u from User u", null,
				QueryRewriter.IdentityQueryRewriter.INSTANCE, EVALUATION_CONTEXT_PROVIDER, PARSER);

		assertThat(jpaQuery.createCountQuery(new JpaParametersParameterAccessor(method.getParameters(), new Object[] {})))
				.isEqualTo(typedQuery);
	}

	@Test // DATAJPA-77
	void doesNotApplyPaginationToCountQuery() throws Exception {

		when(em.createQuery(Mockito.anyString())).thenReturn(query);

		Method method = UserRepository.class.getMethod("findAllPaged", Pageable.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);

		AbstractJpaQuery jpaQuery = new SimpleJpaQuery(queryMethod, em, "select u from User u", null,
				QueryRewriter.IdentityQueryRewriter.INSTANCE, EVALUATION_CONTEXT_PROVIDER, PARSER);
		jpaQuery.createCountQuery(
				new JpaParametersParameterAccessor(queryMethod.getParameters(), new Object[] { PageRequest.of(1, 10) }));

		verify(query, times(0)).setFirstResult(anyInt());
		verify(query, times(0)).setMaxResults(anyInt());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	void discoversNativeQuery() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);
		AbstractJpaQuery jpaQuery = JpaQueryFactory.INSTANCE.fromMethodWithQueryString(queryMethod, em,
				queryMethod.getAnnotatedQuery(), null, QueryRewriter.IdentityQueryRewriter.INSTANCE,
				EVALUATION_CONTEXT_PROVIDER);

		assertThat(jpaQuery).isInstanceOf(NativeJpaQuery.class);

		when(em.createNativeQuery(anyString(), eq(User.class))).thenReturn(query);
		when(metadata.getReturnedDomainClass(method)).thenReturn((Class) User.class);

		jpaQuery.createQuery(new JpaParametersParameterAccessor(queryMethod.getParameters(), new Object[] { "Matthews" }));

		verify(em).createNativeQuery("SELECT u FROM User u WHERE u.lastname = ?1", User.class);
	}

	@Test // DATAJPA-554
	void rejectsNativeQueryWithDynamicSort() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class, Sort.class);
		assertThatExceptionOfType(InvalidJpaQueryMethodException.class).isThrownBy(() -> createJpaQuery(method));
	}

	@Test // DATAJPA-352
	@SuppressWarnings("unchecked")
	void doesNotValidateCountQueryIfNotPagingMethod() throws Exception {

		Method method = SampleRepository.class.getMethod("findByAnnotatedQuery");
		when(em.createQuery(Mockito.contains("count"))).thenThrow(IllegalArgumentException.class);

		createJpaQuery(method);
	}

	@Test // DATAJPA-352
	@SuppressWarnings("unchecked")
	void validatesAndRejectsCountQueryIfPagingMethod() throws Exception {

		Method method = SampleRepository.class.getMethod("pageByAnnotatedQuery", Pageable.class);

		when(em.createQuery(Mockito.contains("count"))).thenThrow(IllegalArgumentException.class);

		assertThatIllegalArgumentException().isThrownBy(() -> createJpaQuery(method)).withMessageContaining("Count")
				.withMessageContaining(method.getName());
	}

	@Test
	void createsASimpleJpaQueryFromAnnotation() throws Exception {

		RepositoryQuery query = createJpaQuery(SampleRepository.class.getMethod("findByAnnotatedQuery"));
		assertThat(query).isInstanceOf(SimpleJpaQuery.class);
	}

	@Test
	void createsANativeJpaQueryFromAnnotation() throws Exception {

		RepositoryQuery query = createJpaQuery(SampleRepository.class.getMethod("findNativeByLastname", String.class));
		assertThat(query).isInstanceOf(NativeJpaQuery.class);
	}

	@Test // DATAJPA-757
	void createsNativeCountQuery() throws Exception {

		when(em.createNativeQuery(anyString())).thenReturn(query);

		AbstractJpaQuery jpaQuery = createJpaQuery(
				UserRepository.class.getMethod("findUsersInNativeQueryWithPagination", Pageable.class));

		jpaQuery.doCreateCountQuery(new JpaParametersParameterAccessor(jpaQuery.getQueryMethod().getParameters(),
				new Object[] { PageRequest.of(0, 10) }));

		verify(em).createNativeQuery(anyString());
	}

	@Test // DATAJPA-885
	void projectsWithManuallyDeclaredQuery() throws Exception {

		AbstractJpaQuery jpaQuery = createJpaQuery(SampleRepository.class.getMethod("projectWithExplicitQuery"));

		jpaQuery.createQuery(new JpaParametersParameterAccessor(jpaQuery.getQueryMethod().getParameters(), new Object[0]));

		verify(em, times(0)).createQuery(anyString(), eq(Tuple.class));

		// Two times, first one is from the query validation
		verify(em, times(2)).createQuery(anyString());
	}

	@Test // DATAJPA-1307
	void jdbcStyleParametersOnlyAllowedInNativeQueries() throws Exception {

		// just verifying that it doesn't throw an exception
		createJpaQuery(SampleRepository.class.getMethod("legalUseOfJdbcStyleParameters", String.class));

		Method illegalMethod = SampleRepository.class.getMethod("illegalUseOfJdbcStyleParameters", String.class);

		assertThatExceptionOfType(IllegalArgumentException.class) //
				.isThrownBy(() -> createJpaQuery(illegalMethod));
	}

	@Test // DATAJPA-1163
	void resolvesExpressionInCountQuery() throws Exception {

		when(em.createQuery(Mockito.anyString())).thenReturn(query);

		Method method = SampleRepository.class.getMethod("findAllWithExpressionInCountQuery", Pageable.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);

		AbstractJpaQuery jpaQuery = new SimpleJpaQuery(queryMethod, em, "select u from User u",
				"select count(u.id) from #{#entityName} u", QueryRewriter.IdentityQueryRewriter.INSTANCE,
				EVALUATION_CONTEXT_PROVIDER, PARSER);
		jpaQuery.createCountQuery(
				new JpaParametersParameterAccessor(queryMethod.getParameters(), new Object[] { PageRequest.of(1, 10) }));

		verify(em).createQuery(eq("select u from User u"));
		verify(em).createQuery(eq("select count(u.id) from User u"), eq(Long.class));
	}

	private AbstractJpaQuery createJpaQuery(Method method) {

		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, factory, extractor);
		return JpaQueryFactory.INSTANCE.fromMethodWithQueryString(queryMethod, em, queryMethod.getAnnotatedQuery(), null,
				QueryRewriter.IdentityQueryRewriter.INSTANCE, EVALUATION_CONTEXT_PROVIDER);
	}

	interface SampleRepository {

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?1", nativeQuery = true)
		List<User> findNativeByLastname(String lastname);

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?1", nativeQuery = true)
		List<User> findNativeByLastname(String lastname, Sort sort);

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?1", nativeQuery = true)
		List<User> findNativeByLastname(String lastname, Pageable pageable);

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?", nativeQuery = true)
		List<User> legalUseOfJdbcStyleParameters(String lastname);

		@Query(value = "SELECT u FROM User u WHERE u.lastname = ?")
		List<User> illegalUseOfJdbcStyleParameters(String lastname);

		@Query(USER_QUERY)
		List<User> findByAnnotatedQuery();

		@Query(USER_QUERY)
		Page<User> pageByAnnotatedQuery(Pageable pageable);

		@Query("select u from User u")
		Collection<UserProjection> projectWithExplicitQuery();

		@Query(value = "select u from #{#entityName} u", countQuery = "select count(u.id) from #{#entityName} u")
		List<User> findAllWithExpressionInCountQuery(Pageable pageable);

	}

	interface UserProjection {}
}
