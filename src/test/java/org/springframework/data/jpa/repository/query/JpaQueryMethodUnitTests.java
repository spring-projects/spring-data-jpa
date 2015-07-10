/*
 * Copyright 2008-2011 the original author or authors.
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
import static org.mockito.Mockito.*;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Method;
import java.util.List;

import javax.persistence.LockModeType;
import javax.persistence.QueryHint;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.EntityGraph.EntityGraphType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit test for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaQueryMethodUnitTests {

	static final Class<?> DOMAIN_CLASS = User.class;
	static final String METHOD_NAME = "findByFirstname";

	@Mock QueryExtractor extractor;
	@Mock RepositoryMetadata metadata;

	Method repositoryMethod, invalidReturnType, pageableAndSort, pageableTwice, sortableTwice, modifyingMethod,
			nativeQuery, namedQuery, findWithLockMethod, invalidNamedParameter, findsProjections, findsProjection,
			withMetaAnnotation, queryMethodWithCustomEntityFetchGraph;

	/**
	 * @throws Exception
	 */
	@Before
	public void setUp() throws Exception {

		repositoryMethod = UserRepository.class.getMethod("findByLastname", String.class);

		invalidReturnType = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Pageable.class);
		pageableAndSort = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Pageable.class, Sort.class);
		pageableTwice = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Pageable.class, Pageable.class);

		sortableTwice = InvalidRepository.class.getMethod(METHOD_NAME, String.class, Sort.class, Sort.class);
		modifyingMethod = UserRepository.class.getMethod("renameAllUsersTo", String.class);

		nativeQuery = ValidRepository.class.getMethod("findByLastname", String.class);
		namedQuery = ValidRepository.class.getMethod("findByNamedQuery");

		findWithLockMethod = ValidRepository.class.getMethod("findOneLocked", Integer.class);
		invalidNamedParameter = InvalidRepository.class.getMethod("findByAnnotatedQuery", String.class);

		findsProjections = ValidRepository.class.getMethod("findsProjections");
		findsProjection = ValidRepository.class.getMethod("findsProjection");

		withMetaAnnotation = ValidRepository.class.getMethod("withMetaAnnotation");

		queryMethodWithCustomEntityFetchGraph = ValidRepository.class.getMethod("queryMethodWithCustomEntityFetchGraph",
				Integer.class);
	}

	@Test
	public void testname() {

		JpaQueryMethod method = new JpaQueryMethod(repositoryMethod, new DefaultRepositoryMetadata(UserRepository.class),
				extractor);

		assertEquals("User.findByLastname", method.getNamedQueryName());
		assertThat(method.isCollectionQuery(), is(true));
		assertThat(method.getAnnotatedQuery(), is(nullValue()));
		assertThat(method.isNativeQuery(), is(false));
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullRepositoryMethod() {

		new JpaQueryMethod(null, metadata, extractor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void preventsNullQueryExtractor() {

		new JpaQueryMethod(repositoryMethod, metadata, null);
	}

	@Test
	public void returnsCorrectName() {

		JpaQueryMethod method = new JpaQueryMethod(repositoryMethod, metadata, extractor);
		assertEquals(repositoryMethod.getName(), method.getName());
	}

	@Test
	public void returnsQueryIfAvailable() throws Exception {

		JpaQueryMethod method = new JpaQueryMethod(repositoryMethod, metadata, extractor);

		assertNull(method.getAnnotatedQuery());

		Method repositoryMethod = UserRepository.class.getMethod("findByAnnotatedQuery", String.class);

		assertNotNull(new JpaQueryMethod(repositoryMethod, metadata, extractor).getAnnotatedQuery());
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsInvalidReturntypeOnPagebleFinder() {

		new JpaQueryMethod(invalidReturnType, metadata, extractor);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsPageableAndSortInFinderMethod() {

		new JpaQueryMethod(pageableAndSort, metadata, extractor);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsTwoPageableParameters() {

		new JpaQueryMethod(pageableTwice, metadata, extractor);
	}

	@Test(expected = IllegalStateException.class)
	public void rejectsTwoSortableParameters() {

		new JpaQueryMethod(sortableTwice, metadata, extractor);
	}

	@Test
	public void recognizesModifyingMethod() {

		JpaQueryMethod method = new JpaQueryMethod(modifyingMethod, metadata, extractor);
		assertTrue(method.isModifyingQuery());
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsModifyingMethodWithPageable() throws Exception {

		Method method = InvalidRepository.class.getMethod("updateMethod", String.class, Pageable.class);

		new JpaQueryMethod(method, metadata, extractor);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsModifyingMethodWithSort() throws Exception {

		Method method = InvalidRepository.class.getMethod("updateMethod", String.class, Sort.class);

		new JpaQueryMethod(method, metadata, extractor);
	}

	@Test
	public void discoversHintsCorrectly() {

		JpaQueryMethod method = new JpaQueryMethod(repositoryMethod, metadata, extractor);
		List<QueryHint> hints = method.getHints();

		assertNotNull(hints);
		assertThat(hints.get(0).name(), is("foo"));
		assertThat(hints.get(0).value(), is("bar"));
	}

	@Test
	public void calculatesNamedQueryNamesCorrectly() throws SecurityException, NoSuchMethodException {

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);

		JpaQueryMethod queryMethod = new JpaQueryMethod(repositoryMethod, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("User.findByLastname"));

		Method method = UserRepository.class.getMethod("renameAllUsersTo", String.class);
		queryMethod = new JpaQueryMethod(method, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("User.renameAllUsersTo"));

		method = UserRepository.class.getMethod("findSpecialUsersByLastname", String.class);
		queryMethod = new JpaQueryMethod(method, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("SpecialUser.findSpecialUsersByLastname"));
	}

	/**
	 * @see DATAJPA-117
	 */
	@Test
	public void discoversNativeQuery() {

		JpaQueryMethod method = new JpaQueryMethod(nativeQuery, metadata, extractor);
		assertThat(method.isNativeQuery(), is(true));
	}

	/**
	 * @see DATAJPA-129
	 */
	@Test
	public void considersAnnotatedNamedQueryName() {
		JpaQueryMethod queryMethod = new JpaQueryMethod(namedQuery, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("HateoasAwareSpringDataWebConfiguration.bar"));
	}

	/**
	 * @see DATAJPA-73
	 */
	@Test
	public void discoversLockModeCorrectly() throws Exception {

		JpaQueryMethod method = new JpaQueryMethod(findWithLockMethod, metadata, extractor);
		LockModeType lockMode = method.getLockModeType();

		assertEquals(LockModeType.PESSIMISTIC_WRITE, lockMode);
	}

	/**
	 * @see DATAJPA-142
	 */
	@Test
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void returnsDefaultCountQueryName() {

		when(metadata.getReturnedDomainClass(repositoryMethod)).thenReturn((Class) User.class);

		JpaQueryMethod method = new JpaQueryMethod(repositoryMethod, metadata, extractor);
		assertThat(method.getNamedCountQueryName(), is("User.findByLastname.count"));
	}

	/**
	 * @see DATAJPA-142
	 */
	@Test
	public void returnsDefaultCountQueryNameBasedOnConfiguredNamedQueryName() {

		JpaQueryMethod method = new JpaQueryMethod(namedQuery, metadata, extractor);
		assertThat(method.getNamedCountQueryName(), is("HateoasAwareSpringDataWebConfiguration.bar.count"));
	}

	/**
	 * @see DATAJPA-185
	 */
	@Test
	public void rejectsInvalidNamedParameter() {

		try {
			new JpaQueryMethod(invalidNamedParameter, metadata, extractor);
			fail();
		} catch (IllegalStateException e) {
			// Parameter from query
			assertThat(e.getMessage(), containsString("foo"));
			// Parameter name from annotation
			assertThat(e.getMessage(), containsString("param"));
			// Method name
			assertThat(e.getMessage(), containsString("findByAnnotatedQuery"));
		}
	}

	/**
	 * @see DATAJPA-207
	 */
	@Test
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void returnsTrueIfReturnTypeIsEntity() {

		when(metadata.getDomainType()).thenReturn((Class) User.class);
		when(metadata.getReturnedDomainClass(findsProjections)).thenReturn((Class) Integer.class);
		when(metadata.getReturnedDomainClass(findsProjection)).thenReturn((Class) Integer.class);

		assertThat(new JpaQueryMethod(findsProjections, metadata, extractor).isQueryForEntity(), is(false));
		assertThat(new JpaQueryMethod(findsProjection, metadata, extractor).isQueryForEntity(), is(false));
	}

	/**
	 * @see DATAJPA-345
	 */
	@Test
	public void detectsLockAndQueryHintsOnIfUsedAsMetaAnnotation() {

		JpaQueryMethod method = new JpaQueryMethod(withMetaAnnotation, metadata, extractor);

		assertThat(method.getLockModeType(), is(LockModeType.OPTIMISTIC_FORCE_INCREMENT));
		assertThat(method.getHints(), hasSize(1));
		assertThat(method.getHints().get(0).name(), is("foo"));
		assertThat(method.getHints().get(0).value(), is("bar"));
	}

	/**
	 * @see DATAJPA-466
	 */
	@Test
	public void shouldStoreJpa21FetchGraphInformationAsHint() {
		
		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass(queryMethodWithCustomEntityFetchGraph);

		JpaQueryMethod method = new JpaQueryMethod(queryMethodWithCustomEntityFetchGraph, metadata, extractor);

		assertThat(method.getEntityGraph(), is(notNullValue()));
		assertThat(method.getEntityGraph().getName(), is("User.propertyLoadPath"));
		assertThat(method.getEntityGraph().getType(), is(EntityGraphType.LOAD));
	}

	/**
	 * @see DATAJPA-612
	 */
	@Test
	public void shouldFindEntityGraphAnnotationOnOverriddenSimpleJpaRepositoryMethod() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method)any());
		
		JpaQueryMethod method = new JpaQueryMethod(JpaRepositoryOverride.class.getMethod("findAll"), metadata, extractor);

		assertThat(method.getEntityGraph(), is(notNullValue()));
		assertThat(method.getEntityGraph().getName(), is("User.detail"));
		assertThat(method.getEntityGraph().getType(), is(EntityGraphType.FETCH));
	}

	/**
	 * @see DATAJPA-689
	 */
	@Test
	public void shouldFindEntityGraphAnnotationOnOverriddenSimpleJpaRepositoryMethodFindOne() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method)any());
		
		JpaQueryMethod method = new JpaQueryMethod(JpaRepositoryOverride.class.getMethod("findOne", Long.class), metadata, extractor);

		assertThat(method.getEntityGraph(), is(notNullValue()));
		assertThat(method.getEntityGraph().getName(), is("User.detail"));
		assertThat(method.getEntityGraph().getType(), is(EntityGraphType.FETCH));
	}
	
	/**
	 * DATAJPA-696
	 */
	@Test
	public void shouldFindEntityGraphAnnotationOnQueryMethodGetOneByWithDerivedName() throws Exception {

		doReturn(User.class).when(metadata).getDomainType();
		doReturn(User.class).when(metadata).getReturnedDomainClass((Method)any());
		
		JpaQueryMethod method = new JpaQueryMethod(JpaRepositoryOverride.class.getMethod("getOneById", Long.class), metadata, extractor);

		assertThat(method.getEntityGraph(), is(notNullValue()));
		assertThat(method.getEntityGraph().getName(), is("User.getOneById"));
		assertThat(method.getEntityGraph().getType(), is(EntityGraphType.FETCH));
	}

	/**
	 * @see DATAJPA-758
	 */
	@Test
	public void allowsPositionalBindingEvenIfParametersAreNamed() throws Exception {

		new JpaQueryMethod(ValidRepository.class.getMethod("queryWithPositionalBinding", String.class), metadata,
				extractor);
	}

	/**
	 * Interface to define invalid repository methods for testing.
	 * 
	 * @author Oliver Gierke
	 */
	static interface InvalidRepository {

		// Invalid return type
		User findByFirstname(String firstname, Pageable pageable);

		// Should not use Pageable *and* Sort
		Page<User> findByFirstname(String firstname, Pageable pageable, Sort sort);

		// Must not use two Pageables
		Page<User> findByFirstname(String firstname, Pageable first, Pageable second);

		// Must not use two Pageables
		Page<User> findByFirstname(String firstname, Sort first, Sort second);

		// Not backed by a named query or @Query annotation
		@Modifying
		void updateMethod(String firstname);

		// Modifying and Pageable is not allowed
		@Modifying
		Page<String> updateMethod(String firstname, Pageable pageable);

		// Modifying and Sort is not allowed
		@Modifying
		void updateMethod(String firstname, Sort sort);

		// Typo in named parameter
		@Query("select u from User u where u.firstname = :foo")
		List<User> findByAnnotatedQuery(@Param("param") String param);
	}

	static interface ValidRepository {

		@Query(value = "query", nativeQuery = true)
		List<User> findByLastname(String lastname);

		@Query(name = "HateoasAwareSpringDataWebConfiguration.bar")
		List<User> findByNamedQuery();

		@Lock(LockModeType.PESSIMISTIC_WRITE)
		@Query("select u from User u where u.id = ?1")
		List<User> findOneLocked(Integer primaryKey);

		List<Integer> findsProjections();

		Integer findsProjection();

		@CustomAnnotation
		void withMetaAnnotation();

		/**
		 * @see DATAJPA-466
		 */
		@EntityGraph(value = "User.propertyLoadPath", type = EntityGraphType.LOAD)
		User queryMethodWithCustomEntityFetchGraph(Integer id);

		@Query("select u from User u where u.firstname = ?1")
		User queryWithPositionalBinding(@Param("firstname") String firstname);
	}

	static interface JpaRepositoryOverride extends JpaRepository<User, Long> {

		/**
		 * DATAJPA-612
		 */
		@Override
		@EntityGraph("User.detail")
		List<User> findAll();

		/**
		 * DATAJPA-689
		 */
		@EntityGraph("User.detail")
		User findOne(Long id);
		
		/**
		 * DATAJPA-696
		 */
		@EntityGraph
		User getOneById(Long id);
	}

	@Lock(LockModeType.OPTIMISTIC_FORCE_INCREMENT)
	@QueryHints(@QueryHint(name = "foo", value = "bar"))
	@Retention(RetentionPolicy.RUNTIME)
	static @interface CustomAnnotation {

	}
}
