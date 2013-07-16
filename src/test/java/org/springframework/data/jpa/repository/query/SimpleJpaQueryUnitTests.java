/*
 * Copyright 2008-2013 the original author or authors.
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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.TypedQuery;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.support.DefaultJpaEntityMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityMetadata;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.Parameters;

/**
 * Unit test for {@link SimpleJpaQuery}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class SimpleJpaQueryUnitTests {

	static final String USER_QUERY = "select u from User u";

	JpaQueryMethod method;

	@Mock EntityManager em;
	@Mock EntityManagerFactory emf;
	@Mock QueryExtractor extractor;
	@Mock TypedQuery<Long> query;
	@Mock RepositoryMetadata metadata;
	@Mock ParameterBinder binder;

	public @Rule ExpectedException exception = ExpectedException.none();

	@Before
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void setUp() throws SecurityException, NoSuchMethodException {

		when(em.createQuery(anyString())).thenReturn(query);
		when(em.createQuery(anyString(), eq(Long.class))).thenReturn(query);
		when(em.getEntityManagerFactory()).thenReturn(emf);
		when(emf.createEntityManager()).thenReturn(em);
		when(metadata.getDomainType()).thenReturn((Class) User.class);
		when(metadata.getReturnedDomainClass(Mockito.any(Method.class))).thenReturn((Class) User.class);

		Method setUp = UserRepository.class.getMethod("findByLastname", String.class);
		method = new JpaQueryMethod(setUp, metadata, extractor);
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void prefersDeclaredCountQueryOverCreatingOne() throws Exception {

		method = mock(JpaQueryMethod.class);
		when(method.getCountQuery()).thenReturn("foo");
		when(method.getParameters()).thenReturn(
				new Parameters(SimpleJpaQueryUnitTests.class.getMethod("prefersDeclaredCountQueryOverCreatingOne")));
		when(method.getEntityInformation()).thenReturn((JpaEntityMetadata) new DefaultJpaEntityMetadata<User>(User.class));
		when(em.createQuery("foo", Long.class)).thenReturn(query);

		SimpleJpaQuery jpaQuery = new SimpleJpaQuery(method, em, "select u from User u");

		assertThat(jpaQuery.createCountQuery(new Object[] {}), is(query));
	}

	/**
	 * @see DATAJPA-77
	 */
	@Test
	public void doesNotApplyPaginationToCountQuery() throws Exception {

		when(em.createQuery(Mockito.anyString())).thenReturn(query);

		Method method = UserRepository.class.getMethod("findAllPaged", Pageable.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, extractor);

		AbstractJpaQuery jpaQuery = new SimpleJpaQuery(queryMethod, em, "select u from User u");
		jpaQuery.createCountQuery(new Object[] { new PageRequest(1, 10) });

		verify(query, times(0)).setFirstResult(anyInt());
		verify(query, times(0)).setMaxResults(anyInt());
	}

	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void discoversNativeQuery() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, extractor);
		SimpleJpaQuery jpaQuery = new SimpleJpaQuery(queryMethod, em);

		Class<?> type = Mockito.any();
		when(em.createNativeQuery(Mockito.anyString(), type)).thenReturn(query);
		when(metadata.getReturnedDomainClass(method)).thenReturn((Class) User.class);

		jpaQuery.createQuery(new Object[] { "Matthews" });

		verify(em).createNativeQuery("SELECT u FROM User u WHERE u.lastname = ?1", User.class);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsNativeQueryWithDynamicSort() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class, Sort.class);
		createSimpleJpaQuery(method);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsNativeQueryWithPageable() throws Exception {

		Method method = SampleRepository.class.getMethod("findNativeByLastname", String.class, Pageable.class);
		createSimpleJpaQuery(method);
	}

	/**
	 * @see DATAJPA-352
	 * @throws Exception
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void doesNotValidateCountQueryIfNotPagingMethod() throws Exception {

		Method method = SampleRepository.class.getMethod("findByAnnotatedQuery");
		when(em.createQuery(contains("count"))).thenThrow(IllegalArgumentException.class);

		createSimpleJpaQuery(method);
	}

	/**
	 * @see DATAJPA-352
	 */
	@Test
	@SuppressWarnings("unchecked")
	public void validatesAndRejectsCountQueryIfPagingMethod() throws Exception {

		Method method = SampleRepository.class.getMethod("pageByAnnotatedQuery", Pageable.class);

		when(em.createQuery(contains("count"))).thenThrow(IllegalArgumentException.class);
		exception.expect(IllegalArgumentException.class);
		exception.expectMessage("Count");
		exception.expectMessage(method.getName());

		createSimpleJpaQuery(method);
	}

	private void createSimpleJpaQuery(Method method) {

		JpaQueryMethod queryMethod = new JpaQueryMethod(method, metadata, extractor);
		new SimpleJpaQuery(queryMethod, em);
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
	}
}
