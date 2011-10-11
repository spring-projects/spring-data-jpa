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
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.DefaultRepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;

/**
 * Unit test for {@link QueryMethod}.
 * 
 * @author Oliver Gierke
 */
@RunWith(MockitoJUnitRunner.class)
public class JpaQueryMethodUnitTests {

	static final Class<?> DOMAIN_CLASS = User.class;
	static final String METHOD_NAME = "findByFirstname";

	@Mock
	QueryExtractor extractor;
	@Mock
	RepositoryMetadata metadata;

	Method repositoryMethod, invalidReturnType, pageableAndSort, pageableTwice, sortableTwice, modifyingMethod, findWithLockMethod;

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
	
		findWithLockMethod = UserRepository.class.getMethod("findOneLocked", Integer.class);
	}

	@Test
	public void testname() {

		JpaQueryMethod method = new JpaQueryMethod(repositoryMethod, metadata, extractor);

		assertEquals("User.findByLastname", method.getNamedQueryName());
		assertThat(method.isCollectionQuery(), is(true));
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
	public void discoversLockModeCorrectly() throws Exception {
		JpaQueryMethod method = new JpaQueryMethod(findWithLockMethod, metadata, extractor);
		LockModeType lockMode = method.getLockMode();

		assertEquals(LockModeType.PESSIMISTIC_WRITE, lockMode);
	}

	@Test
	public void calculatesNamedQueryNamesCorrectly() throws SecurityException, NoSuchMethodException {

		JpaQueryMethod queryMethod = new JpaQueryMethod(repositoryMethod, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("User.findByLastname"));

		RepositoryMetadata metadata = new DefaultRepositoryMetadata(UserRepository.class);
		Method method = UserRepository.class.getMethod("renameAllUsersTo", String.class);
		queryMethod = new JpaQueryMethod(method, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("User.renameAllUsersTo"));

		method = UserRepository.class.getMethod("findSpecialUsersByLastname", String.class);
		queryMethod = new JpaQueryMethod(method, metadata, extractor);
		assertThat(queryMethod.getNamedQueryName(), is("SpecialUser.findSpecialUsersByLastname"));
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
	}
}
