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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;

import jakarta.persistence.EntityManager;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for executing finders, thus testing various query lookup strategies.
 *
 * @author Oliver Gierke
 * @author Krzysztof Krason
 * @see QueryLookupStrategy
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = "classpath:config/namespace-application-context.xml")
@Transactional
class UserRepositoryFinderTests {

	@Autowired UserRepository userRepository;
	@Autowired RoleRepository roleRepository;
	@Autowired EntityManager em;

	PersistenceProvider provider;

	private User dave;
	private User carter;
	private User oliver;
	private Role drummer;
	private Role guitarist;
	private Role singer;

	@BeforeEach
	void setUp() {

		drummer = roleRepository.save(new Role("DRUMMER"));
		guitarist = roleRepository.save(new Role("GUITARIST"));
		singer = roleRepository.save(new Role("SINGER"));

		dave = userRepository.save(new User("Dave", "Matthews", "dave@dmband.com", singer));
		carter = userRepository.save(new User("Carter", "Beauford", "carter@dmband.com", singer, drummer));
		oliver = userRepository.save(new User("Oliver August", "Matthews", "oliver@dmband.com"));

		provider = PersistenceProvider.fromEntityManager(em);
	}

	@AfterEach
	void clearUp() {

		userRepository.deleteAll();
		roleRepository.deleteAll();
	}

	/**
	 * Tests creation of a simple query.
	 */
	@Test
	void testSimpleCustomCreatedFinder() {

		User user = userRepository.findByEmailAddressAndLastname("dave@dmband.com", "Matthews");
		assertThat(user).isEqualTo(dave);
	}

	/**
	 * Tests that the repository returns {@code null} for not found objects for finder methods that return a single domain
	 * object.
	 */
	@Test
	void returnsNullIfNothingFound() {

		User user = userRepository.findByEmailAddress("foobar");
		assertThat(user).isNull();
	}

	/**
	 * Tests creation of a simple query consisting of {@code AND} and {@code OR} parts.
	 */
	@Test
	void testAndOrFinder() {

		List<User> users = userRepository.findByEmailAddressAndLastnameOrFirstname("dave@dmband.com", "Matthews", "Carter");

		assertThat(users).isNotNull();
		assertThat(users).containsExactlyInAnyOrder(dave, carter);
	}

	@Test
	void executesPagingMethodToPageCorrectly() {

		Page<User> page = userRepository.findByLastname(PageRequest.of(0, 1), "Matthews");

		assertThat(page.getNumberOfElements()).isOne();
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	void executesPagingMethodToListCorrectly() {

		List<User> list = userRepository.findByFirstname("Carter", PageRequest.of(0, 1));
		assertThat(list).containsExactly(carter);
	}

	@Test
	void executesInKeywordForPageCorrectly() {

		Page<User> page = userRepository.findByFirstnameIn(PageRequest.of(0, 1), "Dave", "Oliver August");

		assertThat(page.getNumberOfElements()).isOne();
		assertThat(page.getTotalElements()).isEqualTo(2L);
		assertThat(page.getTotalPages()).isEqualTo(2);
	}

	@Test
	void executesNotInQueryCorrectly() {

		List<User> result = userRepository.findByFirstnameNotIn(Arrays.asList("Dave", "Carter"));

		assertThat(result).containsExactly(oliver);
	}

	@Test // DATAJPA-92
	void findsByLastnameIgnoringCase() {

		List<User> result = userRepository.findByLastnameIgnoringCase("BeAUfoRd");

		assertThat(result).containsExactly(carter);
	}

	@Test // DATAJPA-92
	void findsByLastnameIgnoringCaseLike() {

		List<User> result = userRepository.findByLastnameIgnoringCaseLike("BeAUfo%");

		assertThat(result).containsExactly(carter);
	}

	@Test // DATAJPA-92
	void findByLastnameAndFirstnameAllIgnoringCase() {

		List<User> result = userRepository.findByLastnameAndFirstnameAllIgnoringCase("MaTTheWs", "DaVe");

		assertThat(result).containsExactly(dave);
	}

	@Test // DATAJPA-94
	void respectsPageableOrderOnQueryGenerateFromMethodName() {

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
	void executesQueryToSlice() {

		Slice<User> slice = userRepository.findSliceByLastname("Matthews", PageRequest.of(0, 1, ASC, "firstname"));

		assertThat(slice.getContent()).containsExactly(dave);
		assertThat(slice.hasNext()).isTrue();
	}

	@Test // DATAJPA-1554
	void executesQueryToSliceWithUnpaged() {

		Slice<User> slice = userRepository.findSliceByLastname("Matthews", Pageable.unpaged());

		assertThat(slice).containsExactlyInAnyOrder(dave, oliver);
		assertThat(slice.getNumberOfElements()).isEqualTo(2);
		assertThat(slice.hasNext()).isFalse();
	}

	@Test // DATAJPA-830
	void executesMethodWithNotContainingOnStringCorrectly() {

		assertThat(userRepository.findByLastnameNotContaining("u")) //
				.containsExactly(dave, oliver);
	}

	@Test // DATAJPA-1519
	void parametersForContainsGetProperlyEscaped() {

		assertThat(userRepository.findByFirstnameContaining("liv%")) //
				.isEmpty();
	}

	@Test // DATAJPA-1519
	void escapingInLikeSpels() {

		User extra = new User("extra", "Matt_ew", "extra");

		userRepository.save(extra);

		assertThat(userRepository.findContainingEscaped("att_")).containsExactly(extra);
	}

	@Test // DATAJPA-1522
	void escapingInLikeSpelsInThePresenceOfEscapeCharacters() {

		User withEscapeCharacter = userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		assertThat(userRepository.findContainingEscaped("att\\x")).containsExactly(withEscapeCharacter);
	}

	@Test // DATAJPA-1522
	void escapingInLikeSpelsInThePresenceOfEscapedWildcards() {

		userRepository.save(new User("extra", "Matt\\xew", "extra1"));
		User withEscapedWildcard = userRepository.save(new User("extra", "Matt\\_ew", "extra2"));

		assertThat(userRepository.findContainingEscaped("att\\_")).containsExactly(withEscapedWildcard);
	}

	@Test // DATAJPA-829
	void translatesContainsToMemberOf() {

		assertThat(userRepository.findByRolesContaining(singer)) //
				.containsExactlyInAnyOrder(dave, carter);

		assertThat(userRepository.findByRolesContaining(drummer)) //
				.containsExactly(carter);
	}

	@Test // DATAJPA-829
	void translatesNotContainsToNotMemberOf() {

		assertThat(userRepository.findByRolesNotContaining(drummer)) //
				.containsExactlyInAnyOrder(dave, oliver);
	}

	@Test // DATAJPA-974
	void executesQueryWithProjectionContainingReferenceToPluralAttribute() {

		assertThat(userRepository.findRolesAndFirstnameBy()) //
				.isNotNull();
	}

	@Test // DATAJPA-1023, DATACMNS-959
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	void rejectsStreamExecutionIfNoSurroundingTransactionActive() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> userRepository.findAllByCustomQueryAndStream());
	}

	@Test // DATAJPA-1334
	void executesNamedQueryWithConstructorExpression() {
		userRepository.findByNamedQueryWithConstructorExpression();
	}
}
