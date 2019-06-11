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
import static org.springframework.data.domain.Sort.Direction.*;

import java.util.Arrays;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for executing finders, thus testing various query lookup strategies.
 *
 * @author Oliver Gierke
 * @see QueryLookupStrategy
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
public class UserRepositoryFinderTests {

	@Autowired UserRepository userRepository;
	@Autowired RoleRepository roleRepository;

	User dave, carter, oliver;
	Role drummer, guitarist, singer;

	@Before
	public void setUp() {

		drummer = roleRepository.save(new Role("DRUMMER"));
		guitarist = roleRepository.save(new Role("GUITARIST"));
		singer = roleRepository.save(new Role("SINGER"));

		dave = userRepository.save(new User("Dave", "Matthews", "dave@dmband.com", singer));
		carter = userRepository.save(new User("Carter", "Beauford", "carter@dmband.com", singer, drummer));
		oliver = userRepository.save(new User("Oliver August", "Matthews", "oliver@dmband.com"));
	}

	@After
	public void clearUp() {

		userRepository.deleteAll();
		roleRepository.deleteAll();
	}

	/**
	 * Tests creation of a simple query.
	 */
	@Test
	public void testSimpleCustomCreatedFinder() {

		User user = userRepository.findByEmailAddressAndLastname("dave@dmband.com", "Matthews");
		assertThat(user).isEqualTo(dave);
	}

	/**
	 * Tests that the repository returns {@code null} for not found objects for finder methods that return a single domain
	 * object.
	 */
	@Test
	public void returnsNullIfNothingFound() {

		User user = userRepository.findByEmailAddress("foobar");
		assertThat(user).isNull();
	}

	/**
	 * Tests creation of a simple query consisting of {@code AND} and {@code OR} parts.
	 */
	@Test
	public void testAndOrFinder() {

		List<User> users = userRepository.findByEmailAddressAndLastnameOrFirstname("dave@dmband.com", "Matthews", "Carter");

		assertThat(users).isNotNull();
		assertThat(users).containsExactlyInAnyOrder(dave, carter);
	}

	@Test
	public void executesPagingMethodToPageCorrectly() {

		Page<User> page = userRepository.findByLastname(PageRequest.of(0, 1), "Matthews");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	public void executesPagingMethodToListCorrectly() {

		List<User> list = userRepository.findByFirstname("Carter", PageRequest.of(0, 1));
		assertThat(list).containsExactly(carter);
	}

	@Test
	public void executesInKeywordForPageCorrectly() {

		Page<User> page = userRepository.findByFirstnameIn(PageRequest.of(0, 1), "Dave", "Oliver August");

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	public void executesNotInQueryCorrectly() throws Exception {

		List<User> result = userRepository.findByFirstnameNotIn(Arrays.asList("Dave", "Carter"));

		assertThat(result).containsExactly(oliver);
	}

	@Test // DATAJPA-92
	public void findsByLastnameIgnoringCase() {

		List<User> result = userRepository.findByLastnameIgnoringCase("BeAUfoRd");

		assertThat(result).containsExactly(carter);
	}

	@Test // DATAJPA-92
	public void findsByLastnameIgnoringCaseLike() throws Exception {

		List<User> result = userRepository.findByLastnameIgnoringCaseLike("BeAUfo%");

		assertThat(result).containsExactly(carter);
	}

	@Test // DATAJPA-92
	public void findByLastnameAndFirstnameAllIgnoringCase() {

		List<User> result = userRepository.findByLastnameAndFirstnameAllIgnoringCase("MaTTheWs", "DaVe");

		assertThat(result).containsExactly(dave);
	}

	@Test // DATAJPA-94
	public void respectsPageableOrderOnQueryGenerateFromMethodName() {

		Page<User> ascending = userRepository.findByLastnameIgnoringCase( //
				PageRequest.of(0, 10, Sort.by(ASC, "firstname")), //
				"Matthews" //
		);
		Page<User> descending = userRepository.findByLastnameIgnoringCase( //
				PageRequest.of(0, 10, Sort.by(DESC, "firstname")), //
				"Matthews" //
		);

		assertThat(ascending).containsExactly(dave, oliver);
		assertThat(descending).containsExactly(oliver, dave);
	}

	@Test // DATAJPA-486
	public void executesQueryToSlice() {

		Slice<User> slice = userRepository.findSliceByLastname("Matthews", PageRequest.of(0, 1, ASC, "firstname"));

		assertThat(slice.getContent()).containsExactly(dave);
		assertThat(slice.hasNext()).isTrue();
	}

	@Test // DATAJPA-1554
	public void executesQueryToSliceWithUnpaged() {

		Slice<User> slice = userRepository.findSliceByLastname("Matthews", Pageable.unpaged());

		assertThat(slice).containsExactlyInAnyOrder(dave, oliver);
		assertThat(slice.getNumberOfElements()).isEqualTo(2);
		assertThat(slice.hasNext()).isEqualTo(false);
	}

	@Test // DATAJPA-830
	public void executesMethodWithNotContainingOnStringCorrectly() {

		assertThat(userRepository.findByLastnameNotContaining("u")) //
				.containsExactly(dave, oliver);
	}

	@Test // DATAJPA-1519
	public void parametersForContainsGetProperlyEscaped() {

		assertThat(userRepository.findByFirstnameContaining("liv%")) //
				.isEmpty();
	}

	@Test // DATAJPA-1519
	public void escapingInLikeSpels() {

		User extra = new User("extra", "Matt_ew", "extra");

		userRepository.save(extra);

		assertThat(userRepository.findContainingEscaped("att_")).containsExactly(extra);
	}

	@Test // DATAJPA-1522
	public void escapingInLikeSpelsInThePresenceOfEscapeCharacters() {

		User withEscapeCharacter = userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		assertThat(userRepository.findContainingEscaped("att\\x")).containsExactly(withEscapeCharacter);
	}

	@Test // DATAJPA-1522
	public void escapingInLikeSpelsInThePresenceOfEscapedWildcards() {

		userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		User withEscapedWildcard = userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		assertThat(userRepository.findContainingEscaped("att\\_")).containsExactly(withEscapedWildcard);
	}

	@Test // DATAJPA-829
	public void translatesContainsToMemberOf() {

		assertThat(userRepository.findByRolesContaining(singer)) //
				.containsExactlyInAnyOrder(dave, carter);

		assertThat(userRepository.findByRolesContaining(drummer)) //
				.containsExactly(carter);
	}

	@Test // DATAJPA-829
	public void translatesNotContainsToNotMemberOf() {

		assertThat(userRepository.findByRolesNotContaining(drummer)) //
				.containsExactlyInAnyOrder(dave, oliver);
	}

	@Test // DATAJPA-974
	public void executesQueryWithProjectionContainingReferenceToPluralAttribute() {

		assertThat(userRepository.findRolesAndFirstnameBy()) //
				.isNotNull();
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-1023, DATACMNS-959
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void rejectsStreamExecutionIfNoSurroundingTransactionActive() {
		userRepository.findAllByCustomQueryAndStream();
	}

	@Test // DATAJPA-1334
	public void executesNamedQueryWithConstructorExpression() {
		userRepository.findByNamedQueryWithConstructorExpression();
	}
}
