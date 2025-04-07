/*
 * Copyright 2025 the original author or authors.
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
package org.springframework.data.jpa.repository.aot;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import org.hibernate.proxy.HibernateProxy;
import org.hibernate.query.QueryTypeMismatchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.Limit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.SpecialUser;
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
	User luke, leia, han, chewbacca, yoda, vader, kylo;
	Role smuggler, jedi, imperium;

	@Configuration
	static class JpaRepositoryContributorConfiguration extends AotFragmentTestConfigurationSupport {
		public JpaRepositoryContributorConfiguration() {
			super(UserRepository.class);
		}
	}

	@BeforeEach
	void beforeEach() {

		em.createQuery("DELETE FROM %s".formatted(User.class.getName())).executeUpdate();
		em.createQuery("DELETE FROM %s".formatted(Role.class.getName())).executeUpdate();

		smuggler = em.merge(new Role("Smuggler"));
		jedi = em.merge(new Role("Jedi"));
		imperium = em.merge(new Role("Imperium"));

		luke = new User("Luke", "Skywalker", "luke@jedi.org");
		luke.addRole(jedi);
		em.persist(luke);

		leia = new User("Leia", "Organa", "leia@resistance.gov");
		em.persist(leia);

		han = new User("Han", "Solo", "han@smuggler.net");
		han.setManager(luke);
		em.persist(han);

		chewbacca = new User("Chewbacca", "n/a", "chewie@smuggler.net");
		chewbacca.setManager(han);
		chewbacca.addRole(smuggler);
		em.persist(chewbacca);

		yoda = new User("Yoda", "n/a", "yoda@jedi.org");
		em.persist(yoda);

		vader = new User("Anakin", "Skywalker", "vader@empire.com");
		em.persist(vader);

		kylo = new User("Ben", "Solo", "kylo@new-empire.com");
		em.persist(kylo);

		em.flush();
		em.clear();
	}

	@Test // GH-3830
	void testDerivedFinderWithoutArguments() {

		List<User> users = fragment.findUserNoArgumentsBy();
		assertThat(users).hasSize(7).hasOnlyElementsOfType(User.class);
	}

	@Test // GH-3830
	void testFindDerivedQuerySingleEntity() {

		User user = fragment.findOneByEmailAddress("luke@jedi.org");
		assertThat(user.getLastname()).isEqualTo("Skywalker");
	}

	@Test // GH-3830
	void testFindDerivedFinderOptionalEntity() {

		Optional<User> user = fragment.findOptionalOneByEmailAddress("yoda@jedi.org");
		assertThat(user).isNotNull().containsInstanceOf(User.class)
				.hasValueSatisfying(it -> assertThat(it).extracting(User::getFirstname).isEqualTo("Yoda"));
	}

	@Test // GH-3830
	void testDerivedCount() {

		Long value = fragment.countUsersByLastname("Skywalker");
		assertThat(value).isEqualTo(2L);
	}

	@Test // GH-3830
	void testDerivedExists() {

		Boolean exists = fragment.existsUserByLastname("Skywalker");
		assertThat(exists).isTrue();
	}

	@Test // GH-3830
	void testDerivedFinderReturningList() {

		List<User> users = fragment.findByLastnameStartingWith("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("luke@jedi.org", "vader@empire.com",
				"kylo@new-empire.com", "han@smuggler.net");
	}

	@Test // GH-3830
	void shouldReturnStream() {

		Stream<User> users = fragment.streamByLastnameLike("S%");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("luke@jedi.org", "vader@empire.com",
				"kylo@new-empire.com", "han@smuggler.net");
	}

	@Test // GH-3830
	void testLimitedDerivedFinder() {

		List<User> users = fragment.findTop2ByLastnameStartingWith("S");
		assertThat(users).hasSize(2);
	}

	@Test // GH-3830
	void testSortedDerivedFinder() {

		List<User> users = fragment.findByLastnameStartingWithOrderByEmailAddress("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
				"luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
	void testDerivedFinderWithLimitArgument() {

		List<User> users = fragment.findByLastnameStartingWith("S", Limit.of(2));
		assertThat(users).hasSize(2);
	}

	@Test // GH-3830
	void testDerivedFinderWithSort() {

		List<User> users = fragment.findByLastnameStartingWith("S", Sort.by("emailAddress"));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
				"luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
	void testDerivedFinderWithSortAndLimit() {

		List<User> users = fragment.findByLastnameStartingWith("S", Sort.by("emailAddress"), Limit.of(2));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test // GH-3830
	void testDerivedFinderReturningListWithPageable() {

		List<User> users = fragment.findByLastnameStartingWith("S", PageRequest.of(0, 2, Sort.by("emailAddress")));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test // GH-3830
	void testDerivedFinderReturningPage() {

		Page<User> page = fragment.findPageOfUsersByLastnameStartingWith("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test // GH-3830
	void testDerivedFinderReturningSlice() {

		Slice<User> slice = fragment.findSliceOfUserByLastnameStartingWith("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getSize()).isEqualTo(2);
		assertThat(slice.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderReturningSingleValueWithQuery() {

		User user = fragment.findAnnotatedQueryByEmailAddress("yoda@jedi.org");
		assertThat(user).isNotNull().extracting(User::getFirstname).isEqualTo("Yoda");
	}

	@Test // GH-3830
	void testAnnotatedFinderReturningListWithQuery() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderUsingNamedParameterPlaceholderReturningListWithQuery() {

		List<User> users = fragment.findAnnotatedQueryByLastnameParameter("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
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

	@Test // GH-3830
	void testAnnotatedMultilineFinderWithQuery() {

		List<User> users = fragment.findAnnotatedMultilineQueryByLastname("S");
		assertThat(users).extracting(User::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderWithQueryAndLimit() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", Limit.of(2));
		assertThat(users).hasSize(2);
	}

	@Test // GH-3830
	void testAnnotatedFinderWithQueryAndSort() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", Sort.by("emailAddress"));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com",
				"luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderWithQueryLimitAndSort() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", Limit.of(2), Sort.by("emailAddress"));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderReturningListWithPageable() {

		List<User> users = fragment.findAnnotatedQueryByLastname("S", PageRequest.of(0, 2, Sort.by("emailAddress")));
		assertThat(users).extracting(User::getEmailAddress).containsExactly("han@smuggler.net", "kylo@new-empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderReturningPage() {

		Page<User> page = fragment.findAnnotatedQueryPageOfUsersByLastname("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test // GH-3830
	void testPagingAnnotatedQueryWithSort() {

		Page<User> page = fragment.findAnnotatedQueryPageWithStaticSort("S", PageRequest.of(0, 2, Sort.unsorted()));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(User::getEmailAddress).containsExactly("luke@jedi.org",
				"vader@empire.com");
	}

	@Test // GH-3830
	void testAnnotatedFinderReturningSlice() {

		Slice<User> slice = fragment.findAnnotatedQuerySliceOfUsersByLastname("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));
		assertThat(slice.hasNext()).isTrue();
		assertThat(slice.getSize()).isEqualTo(2);
		assertThat(slice.getContent()).extracting(User::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");
	}

	@Test // GH-3830
	void shouldResolveTemplatedQuery() {

		User user = fragment.findTemplatedByEmailAddress("han@smuggler.net");

		assertThat(user).isNotNull();
		assertThat(user.getFirstname()).isEqualTo("Han");
	}

	@Test // GH-3830
	void shouldEvaluateExpressionByName() {

		User user = fragment.findValueExpressionNamedByEmailAddress("han@smuggler.net");

		assertThat(user).isNotNull();
		assertThat(user.getFirstname()).isEqualTo("Han");
	}

	@Test // GH-3830
	void shouldEvaluateExpressionByPosition() {

		User user = fragment.findValueExpressionPositionalByEmailAddress("han@smuggler.net");

		assertThat(user).isNotNull();
		assertThat(user.getFirstname()).isEqualTo("Han");
	}

	@Test // GH-3830
	void testDerivedFinderReturningListOfProjections() {

		List<UserDtoProjection> users = fragment.findUserProjectionByLastnameStartingWith("S");
		assertThat(users).extracting(UserDtoProjection::getEmailAddress).containsExactlyInAnyOrder("han@smuggler.net",
				"kylo@new-empire.com", "luke@jedi.org", "vader@empire.com");
	}

	@Test // GH-3830
	void testDerivedFinderReturningPageOfProjections() {

		Page<UserDtoProjection> page = fragment.findUserProjectionByLastnameStartingWith("S",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(page.getTotalElements()).isEqualTo(4);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).extracting(UserDtoProjection::getEmailAddress).containsExactly("han@smuggler.net",
				"kylo@new-empire.com");

		Page<UserDtoProjection> noResults = fragment.findUserProjectionByLastnameStartingWith("a",
				PageRequest.of(0, 2, Sort.by("emailAddress")));

		assertThat(noResults).isEmpty();
	}

	@Test // GH-3830
	void shouldApplySqlResultSetMapping() {

		User.EmailDto result = fragment.findEmailDtoByNativeQuery(kylo.getId());

		assertThat(result.getOne()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyNamedDto() {

		// named queries cannot be rewritten
		assertThatExceptionOfType(QueryTypeMismatchException.class)
				.isThrownBy(() -> fragment.findNamedDtoEmailAddress(kylo.getEmailAddress()));
	}

	@Test // GH-3830
	void shouldApplyDerivedDto() {

		UserRepository.Names names = fragment.findDtoByEmailAddress(kylo.getEmailAddress());

		assertThat(names.lastname()).isEqualTo(kylo.getLastname());
		assertThat(names.firstname()).isEqualTo(kylo.getFirstname());
	}

	@Test // GH-3830
	void shouldApplyDerivedDtoPage() {

		Page<UserRepository.Names> names = fragment.findDtoPageByEmailAddress(kylo.getEmailAddress(), PageRequest.of(0, 1));

		assertThat(names).hasSize(1);
		assertThat(names.getContent().get(0).lastname()).isEqualTo(kylo.getLastname());
	}

	@Test // GH-3830
	void shouldApplyAnnotatedDto() {

		UserRepository.Names names = fragment.findAnnotatedDtoEmailAddress(kylo.getEmailAddress());

		assertThat(names.lastname()).isEqualTo(kylo.getLastname());
		assertThat(names.firstname()).isEqualTo(kylo.getFirstname());
	}

	@Test // GH-3830
	void shouldApplyAnnotatedDtoPage() {

		Page<UserRepository.Names> names = fragment.findAnnotatedDtoPageByEmailAddress(kylo.getEmailAddress(),
				PageRequest.of(0, 1));

		assertThat(names).hasSize(1);
		assertThat(names.getContent().get(0).lastname()).isEqualTo(kylo.getLastname());
	}

	@Test // GH-3830
	void shouldApplyDerivedQueryInterfaceProjection() {

		UserRepository.EmailOnly result = fragment.findEmailProjectionById(kylo.getId());

		assertThat(result.getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyInterfaceProjectionPage() {

		Page<UserRepository.EmailOnly> result = fragment.findProjectedPageByEmailAddress(kylo.getEmailAddress(),
				PageRequest.of(0, 1));

		assertThat(result).hasSize(1);
		assertThat(result.getContent().get(0).getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyInterfaceProjectionSlice() {

		Slice<UserRepository.EmailOnly> result = fragment.findProjectedSliceByEmailAddress(kylo.getEmailAddress(),
				PageRequest.of(0, 1));

		assertThat(result).hasSize(1);
		assertThat(result.getContent().get(0).getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyInterfaceProjectionToDerivedQueryStream() {

		Stream<UserRepository.EmailOnly> result = fragment.streamProjectedByEmailAddress(kylo.getEmailAddress());

		assertThat(result).hasSize(1).map(UserRepository.EmailOnly::getEmailAddress).contains(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyAnnotatedQueryInterfaceProjection() {

		UserRepository.EmailOnly result = fragment.findAnnotatedEmailProjectionByEmailAddress(kylo.getEmailAddress());

		assertThat(result.getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyAnnotatedInterfaceProjectionQueryPage() {

		Page<UserRepository.EmailOnly> result = fragment.findAnnotatedProjectedPageByEmailAddress(kylo.getEmailAddress(),
				PageRequest.of(0, 1));

		assertThat(result).hasSize(1);
		assertThat(result.getContent().get(0).getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyNativeInterfaceProjection() {

		UserRepository.EmailOnly result = fragment.findEmailProjectionByNativeQuery(kylo.getId());

		assertThat(result.getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void shouldApplyNamedQueryInterfaceProjection() {

		UserRepository.EmailOnly result = fragment.findNamedProjectionEmailAddress(kylo.getEmailAddress());

		assertThat(result.getEmailAddress()).isEqualTo(kylo.getEmailAddress());
	}

	@Test // GH-3830
	void testDerivedDeleteSingle() {

		User result = fragment.deleteByEmailAddress("yoda@jedi.org");

		assertThat(result).isNotNull().extracting(User::getEmailAddress).isEqualTo("yoda@jedi.org");

		Object yodaShouldBeGone = em
				.createQuery("SELECT u FROM %s u WHERE u.emailAddress = 'yoda@jedi.org'".formatted(User.class.getName()))
				.getSingleResultOrNull();
		assertThat(yodaShouldBeGone).isNull();
	}

	@Test // GH-3830
	void shouldOmitAnnotatedDeleteReturningDomainType() {

		assertThatException().isThrownBy(() -> fragment.deleteAnnotatedQueryByEmailAddress("foo"))
				.withRootCauseInstanceOf(NoSuchMethodException.class);
	}

	@Test // GH-3830
	void shouldApplyModifying() {

		int affected = fragment.renameAllUsersTo("Jones");

		assertThat(affected).isEqualTo(7);

		Object yodaShouldBeGone = em
				.createQuery("SELECT u FROM %s u WHERE u.lastname = 'n/a'".formatted(User.class.getName()))
				.getSingleResultOrNull();
		assertThat(yodaShouldBeGone).isNull();
	}

	@Test // GH-3830
	void nativeQuery() {

		Page<String> page = fragment.findByNativeQueryWithPageable(PageRequest.of(0, 2));

		assertThat(page.getTotalElements()).isEqualTo(7);
		assertThat(page.getSize()).isEqualTo(2);
		assertThat(page.getContent()).containsExactly("Anakin", "Ben");
	}

	@Test // GH-3830
	void shouldUseNamedQuery() {

		User user = fragment.findByEmailAddress("luke@jedi.org");
		assertThat(user.getLastname()).isEqualTo("Skywalker");
	}

	@Test // GH-3830
	void shouldUseNamedQueryAndDeriveCountQuery() {

		Page<User> user = fragment.findPagedByEmailAddress(PageRequest.of(0, 1), "luke@jedi.org");

		assertThat(user).hasSize(1);
		assertThat(user.getTotalElements()).isEqualTo(1);
	}

	@Test // GH-3830
	void shouldUseNamedQueryAndProvidedCountQuery() {

		Page<User> user = fragment.findPagedWithCountByEmailAddress(PageRequest.of(0, 1), "luke@jedi.org");

		assertThat(user).hasSize(1);
		assertThat(user.getTotalElements()).isEqualTo(1);
	}

	@Test // GH-3830
	void shouldUseNamedQueryAndNamedCountQuery() {

		Page<User> user = fragment.findPagedWithNamedCountByEmailAddress(PageRequest.of(0, 1), "luke@jedi.org");

		assertThat(user).hasSize(1);
		assertThat(user.getTotalElements()).isEqualTo(1);
	}

	@Test // GH-3830
	void shouldApplyQueryHints() {
		assertThatIllegalArgumentException().isThrownBy(() -> fragment.findHintedByLastname("Skywalker"))
				.withMessageContaining("No enum constant jakarta.persistence.CacheStoreMode.foo");
	}

	@Test // GH-3830
	void shouldApplyNamedEntityGraph() {

		User chewie = fragment.findWithNamedEntityGraphByFirstname("Chewbacca");

		assertThat(chewie.getManager()).isInstanceOf(HibernateProxy.class);
		assertThat(chewie.getRoles()).isNotInstanceOf(HibernateProxy.class);
	}

	@Test // GH-3830
	void shouldApplyDeclaredEntityGraph() {

		User chewie = fragment.findWithDeclaredEntityGraphByFirstname("Chewbacca");

		assertThat(chewie.getRoles()).isNotInstanceOf(HibernateProxy.class);

		User han = chewie.getManager();
		assertThat(han.getRoles()).isNotInstanceOf(HibernateProxy.class);
		assertThat(han.getManager()).isInstanceOf(HibernateProxy.class);
	}

	@Test // GH-3830
	void shouldQuerySubtype() {

		SpecialUser snoopy = new SpecialUser();
		snoopy.setFirstname("Snoopy");
		snoopy.setLastname("n/a");
		snoopy.setEmailAddress("dog@home.com");
		em.persist(snoopy);

		SpecialUser result = fragment.findByEmailAddress("dog@home.com", SpecialUser.class);

		assertThat(result).isNotNull();
		assertThat(result).isInstanceOf(SpecialUser.class);
	}

	@Test // GH-3830
	void shouldApplyQueryRewriter() {

		User result = fragment.findAndApplyQueryRewriter(kylo.getEmailAddress());

		assertThat(result).isNotNull();

		Page<User> page = fragment.findAndApplyQueryRewriter(kylo.getEmailAddress(), Pageable.unpaged());

		assertThat(page).isNotEmpty();
	}

	void todo() {

		// dynamic projections: Not implemented
		// keyset scrolling

	}

}
