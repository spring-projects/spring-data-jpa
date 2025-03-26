/*
 * Copyright 2024 the original author or authors.
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
package org.springframework.data.jpa.repository.aot.generated;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration tests for the {@link UserRepository} AOT fragment.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 */
@SpringJUnitConfig(classes = JpaRepositoryContributorIntegrationTests.JpaRepositoryContributorConfiguration.class)
@Transactional
class JpaRepositoryContributorIntegrationTests {

	@Autowired UserRepository fragment;
	@Autowired EntityManager em;

	@Configuration
	static class JpaRepositoryContributorConfiguration extends AotFragmentTestConfigurationSupport {
		public JpaRepositoryContributorConfiguration() {
			super(UserRepository.class);
		}
	}

	@BeforeEach
	void beforeEach() {

		em.createQuery("DELETE FROM %s".formatted(User.class.getName())).executeUpdate();

		User luke = new User("Luke", "Skywalker", "luke@jedi.org");
		em.persist(luke);

		User leia = new User("Leia", "Organa", "leia@resistance.gov");
		em.persist(leia);

		User han = new User("Han", "Solo", "han@smuggler.net");
		em.persist(han);

		User chewbacca = new User("Chewbacca", "n/a", "chewie@smuggler.net");
		em.persist(chewbacca);

		User yoda = new User("Yoda", "n/a", "yoda@jedi.org");
		em.persist(yoda);

		User vader = new User("Anakin", "Skywalker", "vader@empire.com");
		em.persist(vader);

		User kylo = new User("Ben", "Solo", "kylo@new-empire.com");
		em.persist(kylo);
	}

	@Test
	void testFindDerivedFinderSingleEntity() {

		User user = fragment.findByEmailAddress("luke@jedi.org");
		assertThat(user.getLastname()).isEqualTo("Skywalker");
	}

	@Test
	void testFindDerivedFinderOptionalEntity() {

		Optional<User> user = fragment.findOptionalOneByEmailAddress("yoda@jedi.org");
		assertThat(user).isNotNull().containsInstanceOf(User.class)
				.hasValueSatisfying(it -> assertThat(it).extracting(User::getFirstname).isEqualTo("Yoda"));
	}

	@Test
	void testDerivedCount() {

		Long value = fragment.countUsersByLastname("Skywalker");
		assertThat(value).isEqualTo(2L);
	}

	@Test
	void testDerivedExists() {

		Boolean exists = fragment.existsUserByLastname("Skywalker");
		assertThat(exists).isTrue();
	}

	@Test
	void testDerivedFinderWithoutArguments() {

		List<User> users = fragment.findUserNoArgumentsBy();
		assertThat(users).hasSize(7).hasOnlyElementsOfType(User.class);
	}

	@Test
	void testDerivedFinderReturningList() {

		List<User> users = fragment.findByLastnameStartingWith("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("luke@jedi.org", "vader@empire.com",
				"kylo@new-empire.com", "han@smuggler.net");
	}

	@Test
	void testLimitedDerivedFinder() {

		List<User> users = fragment.findTop2ByLastnameStartingWith("S");
		assertThat(users).hasSize(2);
	}

	@Test
	void testSortedDerivedFinder() {

		List<User> users = fragment.findByLastnameStartingWithOrderByEmailAddress("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
				"luke@jedi.org", "vader@empire.com");
	}

	@Test
	void testDerivedFinderWithLimitArgument() {

		List<User> users = fragment.findByLastnameStartingWith("S", Limit.of(2));
		assertThat(users).hasSize(2);
	}

	@Test
	void testDerivedFinderWithSort() {

		List<User> users = fragment.findByLastnameStartingWith("S", Sort.by("emailAddress"));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
				"luke@jedi.org", "vader@empire.com");
	}

	@Test
	void testDerivedFinderWithSortAndLimit() {

		List<User> users = fragment.findByLastnameStartingWith("S", Sort.by("emailAddress"), Limit.of(2));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test
	void testDerivedFinderReturningListWithPageable() {

		List<User> users = fragment.findByLastnameStartingWith("S", PageRequest.of(0, 2, Sort.by("emailAddress")));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test
	void testDerivedFinderReturningPage() {

		Page<User> page = fragment.findPageOfUsersByLastnameStartingWith("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test
	void testDerivedFinderReturningSlice() {

		Slice<User> slice = fragment.findSliceOfUserByLastnameStartingWith("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getSize()).isEqualTo(2);
		assertThat(slice.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test
	void testAnnotatedFinderReturningSingleValueWithQuery() {

		User user = fragment.findAnnotatedQueryByEmailAddress("yoda@jedi.org");
		assertThat(user).isNotNull().extracting(User::getFirstname).isEqualTo("Yoda");
	}

	@Test
	void testAnnotatedFinderReturningListWithQuery() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test
	void testAnnotatedFinderUsingNamedParameterPlaceholderReturningListWithQuery() {

		List<User> users = fragment.findAnnotatedQueryByLastnameParameter("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test
	void shouldApplyAnnotatedLikeStartsEnds() {

		// start with case
		List<User> users = fragment.findAnnotatedLikeStartsEnds("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");

		// ends case
		users = fragment.findAnnotatedLikeStartsEnds("a");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("leia@resistance.gov",
				"chewie@smuggler.net", "yoda@jedi.org");
	}

	@Test
	void testAnnotatedMultilineFinderWithQuery() {

		List<User> users = fragment.findAnnotatedMultilineQueryByLastname("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test
	void testAnnotatedFinderWithQueryAndLimit() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", Limit.of(2));
		assertThat(users).hasSize(2);
	}

	@Test
	void testAnnotatedFinderWithQueryAndSort() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", Sort.by("emailAddress"));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
				"luke@jedi.org", "vader@empire.com");
	}

	@Test
	void testAnnotatedFinderWithQueryLimitAndSort() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", Limit.of(2), Sort.by("emailAddress"));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test
	void testAnnotatedFinderReturningListWithPageable() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", PageRequest.of(0, 2, Sort.by("emailAddress")));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test
	void testAnnotatedFinderReturningPage() {

		Page<User> page = fragment.findAnnotatedQueryPageOfUsersByLastname("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test
	void testPagingAnnotatedQueryWithSort() {

		Page<User> page = fragment.findAnnotatedQueryPageWithStaticSort("S", PageRequest.of(0, 2, Sort.unsorted()));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("luke@jedi.org",
				"vader@empire.com");
	}

	@Test
	void testAnnotatedFinderReturningSlice() {

		Slice<User> slice = fragment.findAnnotatedQuerySliceOfUsersByLastname("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getSize()).isEqualTo(2);
		assertThat(slice.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test
	void testDerivedFinderReturningListOfProjections() {

		List<UserDtoProjection> users = fragment.findUserProjectionByLastnameStartingWith("S");
		assertThat(users).extracting(UserDtoProjection::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test
	void shouldApplyQueryHints() {
		assertThatIllegalArgumentException().isThrownBy(() -> fragment.findHintedByLastname("Skywalker"))
				.withMessageContaining("No enum constant jakarta.persistence.CacheStoreMode.foo");
	}

	@Test
	void testDerivedFinderReturningPageOfProjections() {

		Page<UserDtoProjection> page = fragment.findUserProjectionByLastnameStartingWith("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(UserDtoProjection::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");

		Page<UserDtoProjection> noResults = fragment.findUserProjectionByLastnameStartingWith("a",
				PageRequest.of(0, 2, Sort.by("emailAddress")));
	}

	// modifying

	@Test
	void testDerivedDeleteSingle() {

		User result = fragment.deleteByEmailAddress("yoda@jedi.org");

		assertThat(result).isNotNull().extracting(User::getEmailAddress).isEqualTo("yoda@jedi.org");

		Object yodaShouldBeGone = em
				.createQuery("SELECT u FROM %s u WHERE u.emailAddress = 'yoda@jedi.org'".formatted(User.class.getName()))
				.getSingleResultOrNull();
		assertThat(yodaShouldBeGone).isNull();
	}

	// native queries

	@Test
	void nativeQuery() {

		Page<String> page = fragment.findByNativeQueryWithPageable(PageRequest.of(0, 2));

		assertThat(page.getTotalElements()).isEqualTo(7);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).containsExactly("Anakin", "Ben");
	}

	// old stuff below

	void todo() {

		// expressions, templated query with #{#entityName}
		// synthetic parameters (keyset scrolling! yuck!)
		// interface projections
		// named queries
		// dynamic projections
		// class type parameter

		// entity graphs
		// native queries
		// delete
		// @Modifying
		// flush / clear
	}

}
