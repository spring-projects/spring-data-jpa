/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.*;

import java.util.Arrays;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for executing finders, thus testing various query lookup strategies.
 * 
 * @see QueryLookupStrategy
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class UserRepositoryFinderTests {

	@Autowired UserRepository userRepository;

	User dave, carter, oliver;

	@Before
	public void setUp() {

		// This one matches both criterias
		dave = new User("Dave", "Matthews", "dave@dmband.com");
		userRepository.save(dave);

		// This one matches only the second one
		carter = new User("Carter", "Beauford", "carter@dmband.com");
		userRepository.save(carter);

		oliver = new User("Oliver August", "Matthews", "oliver@dmband.com");
		userRepository.save(oliver);
	}

	/**
	 * Tests creation of a simple query.
	 */
	@Test
	public void testSimpleCustomCreatedFinder() {

		User user = userRepository.findByEmailAddressAndLastname("dave@dmband.com", "Matthews");
		assertEquals(dave, user);
	}

	/**
	 * Tests that the repository returns {@code null} for not found objects for finder methods that return a single domain
	 * object.
	 */
	@Test
	public void returnsNullIfNothingFound() {

		User user = userRepository.findByEmailAddress("foobar");
		assertEquals(null, user);
	}

	/**
	 * Tests creation of a simple query consisting of {@code AND} and {@code OR} parts.
	 */
	@Test
	public void testAndOrFinder() {

		List<User> users = userRepository.findByEmailAddressAndLastnameOrFirstname("dave@dmband.com", "Matthews", "Carter");

		assertNotNull(users);
		assertEquals(2, users.size());
		assertTrue(users.contains(dave));
		assertTrue(users.contains(carter));
	}

	@Test
	public void executesPagingMethodToPageCorrectly() {

		Page<User> page = userRepository.findByLastname(new PageRequest(0, 1), "Matthews");
		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getTotalElements(), is(2L));
		assertThat(page.getTotalPages(), is(2));
	}

	@Test
	public void executesPagingMethodToListCorrectly() {

		List<User> list = userRepository.findByFirstname("Carter", new PageRequest(0, 1));
		assertThat(list.size(), is(1));
	}

	@Test
	public void executesInKeywordForPageCorrectly() {

		Page<User> page = userRepository.findByFirstnameIn(new PageRequest(0, 1), "Dave", "Oliver August");

		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getTotalElements(), is(2L));
		assertThat(page.getTotalPages(), is(2));
	}

	@Test
	public void executesNotInQueryCorrectly() throws Exception {

		List<User> result = userRepository.findByFirstnameNotIn(Arrays.asList("Dave", "Carter"));
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(oliver));
	}

	/**
	 * @see DATAJPA-92
	 */
	@Test
	public void findsByLastnameIgnoringCase() throws Exception {
		List<User> result = userRepository.findByLastnameIgnoringCase("BeAUfoRd");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(carter));
	}

	/**
	 * @see DATAJPA-92
	 */
	@Test
	public void findsByLastnameIgnoringCaseLike() throws Exception {
		List<User> result = userRepository.findByLastnameIgnoringCaseLike("BeAUfo%");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(carter));
	}

	/**
	 * @see DATAJPA-92
	 */
	@Test
	public void findByLastnameAndFirstnameAllIgnoringCase() throws Exception {
		List<User> result = userRepository.findByLastnameAndFirstnameAllIgnoringCase("MaTTheWs", "DaVe");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(dave));
	}

	/**
	 * @see DATAJPA-94
	 */
	@Test
	public void respectsPageableOrderOnQueryGenerateFromMethodName() throws Exception {
		Page<User> ascending = userRepository.findByLastnameIgnoringCase(
				new PageRequest(0, 10, new Sort(ASC, "firstname")), "Matthews");
		Page<User> descending = userRepository.findByLastnameIgnoringCase(new PageRequest(0, 10,
				new Sort(DESC, "firstname")), "Matthews");
		assertThat(ascending.getTotalElements(), is(2L));
		assertThat(descending.getTotalElements(), is(2L));
		assertThat(ascending.getContent().get(0).getFirstname(), is(not(equalTo(descending.getContent().get(0)
				.getFirstname()))));
		assertThat(ascending.getContent().get(0).getFirstname(), is(equalTo(descending.getContent().get(1).getFirstname())));
		assertThat(ascending.getContent().get(1).getFirstname(), is(equalTo(descending.getContent().get(0).getFirstname())));
	}

	/**
	 * @see DATAJPA-486
	 */
	@Test
	public void executesQueryToSlice() {

		Slice<User> slice = userRepository.findSliceByLastname("Matthews", new PageRequest(0, 1, ASC, "firstname"));

		assertThat(slice.getContent(), hasItem(dave));
		assertThat(slice.hasNext(), is(true));
	}
}
