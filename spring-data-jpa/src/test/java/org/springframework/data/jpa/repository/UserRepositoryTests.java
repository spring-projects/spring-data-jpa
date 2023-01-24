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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.data.domain.Example.of;
import static org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import static org.springframework.data.domain.ExampleMatcher.StringMatcher;
import static org.springframework.data.domain.ExampleMatcher.matching;
import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.domain.Sort.Direction.DESC;
import static org.springframework.data.jpa.domain.Specification.not;
import static org.springframework.data.jpa.domain.Specification.where;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.userHasAgeLess;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.userHasFirstname;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.userHasFirstnameLike;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.userHasLastname;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.userHasLastnameLikeWithSort;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.assertj.core.api.SoftAssertions;
import org.hibernate.LazyInitializationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Address;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.SpecialUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension.SampleSecurityContextHolder;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.jpa.repository.sample.UserRepository.NameOnly;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base integration test class for {@code UserRepository}. Loads a basic (non-namespace) Spring configuration file as
 * well as Hibernate configuration to execute tests.
 * <p>
 * To test further persistence providers subclass this class and provide a custom provider configuration.
 *
 * @author Oliver Gierke
 * @author Kevin Raymond
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Mark Paluch
 * @author Kevin Peters
 * @author Jens Schauder
 * @author Andrey Kovalev
 * @author Sander Krabbenborg
 * @author Jesse Wouters
 * @author Greg Turnquist
 * @author Diego Krupitza
 * @author Daniel Shuy
 * @author Simon Paradies
 * @author Geoffrey Deremetz
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
class UserRepositoryTests {

	@PersistenceContext EntityManager em;

	// CUT
	@Autowired UserRepository repository;

	// Test fixture
	private User firstUser;
	private User secondUser;
	private User thirdUser;
	private User fourthUser;
	private Integer id;
	private Role adminRole;

	@BeforeEach
	void setUp() throws Exception {

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);
		Thread.sleep(10);
		thirdUser = new User("Dave", "Matthews", "no@email.com");
		thirdUser.setAge(43);
		fourthUser = new User("kevin", "raymond", "no@gmail.com");
		fourthUser.setAge(31);
		adminRole = new Role("admin");

		SampleSecurityContextHolder.clear();
	}

	@Test
	void testCreation() {

		Query countQuery = em.createQuery("select count(u) from User u");
		Long before = (Long) countQuery.getSingleResult();

		flushTestUsers();

		assertThat((Long) countQuery.getSingleResult()).isEqualTo(before + 4L);
	}

	@Test
	void testRead() {

		flushTestUsers();

		assertThat(repository.findById(id)).map(User::getFirstname).contains(firstUser.getFirstname());
	}

	@Test
	void findsAllByGivenIds() {

		flushTestUsers();

		assertThat(repository.findAllById(asList(firstUser.getId(), secondUser.getId()))) //
				.containsExactlyInAnyOrder(firstUser, secondUser);
	}

	@Test
	void testReadByIdReturnsNullForNotFoundEntities() {

		flushTestUsers();

		assertThat(repository.findById(id * 27)).isNotPresent();
	}

	@Test
	void savesCollectionCorrectly() {

		assertThat(repository.saveAll(asList(firstUser, secondUser, thirdUser))) //
				.containsExactlyInAnyOrder(firstUser, secondUser, thirdUser);
	}

	@Test // gh-2148
	void savesAndFlushesCollectionCorrectly() {

		assertThat(repository.saveAllAndFlush(asList(firstUser, secondUser, thirdUser))) //
				.containsExactlyInAnyOrder(firstUser, secondUser, thirdUser);
	}

	@Test
	void savingEmptyCollectionIsNoOp() {
		assertThat(repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test // gh-2148
	void savingAndFlushingEmptyCollectionIsNoOp() {
		assertThat(repository.saveAllAndFlush(new ArrayList<>())).isEmpty();
	}

	@Test
	void testUpdate() {

		flushTestUsers();

		User foundPerson = repository.findById(id).get();
		foundPerson.setLastname("Schlicht");

		assertThat(repository.findById(id)).map(User::getFirstname).contains(foundPerson.getFirstname());
	}

	@Test
	void existReturnsWhetherAnEntityCanBeLoaded() {

		flushTestUsers();
		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(id * 27)).isFalse();
	}

	@Test
	void deletesAUserById() {

		flushTestUsers();

		repository.deleteById(firstUser.getId());

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.findById(id)).isNotPresent();
	}

	@Test
	void testDelete() {

		flushTestUsers();

		repository.delete(firstUser);

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.findById(id)).isNotPresent();
	}

	@Test
	void returnsAllSortedCorrectly() {

		flushTestUsers();

		assertThat(repository.findAll(Sort.by(ASC, "lastname"))).hasSize(4).containsExactly(secondUser, firstUser,
				thirdUser, fourthUser);
	}

	@Test // DATAJPA-296
	void returnsAllIgnoreCaseSortedCorrectly() {

		flushTestUsers();

		Order order = new Order(ASC, "firstname").ignoreCase();

		assertThat(repository.findAll(Sort.by(order))) //
				.hasSize(4)//
				.containsExactly(thirdUser, secondUser, fourthUser, firstUser);
	}

	@Test
	void deleteCollectionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteAll(asList(firstUser, secondUser));

		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test
	void batchDeleteCollectionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteAllInBatch(asList(firstUser, secondUser));

		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test // DATAJPA-1818
	void deleteCollectionOfEntitiesById() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteAllById(asList(firstUser.getId(), secondUser.getId()));

		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test
	void deleteEmptyCollectionDoesNotDeleteAnything() {

		assertDeleteCallDoesNotDeleteAnything(new ArrayList<>());
	}

	@Test
	void executesManipulatingQuery() {

		flushTestUsers();
		repository.renameAllUsersTo("newLastname");

		long expected = repository.count();
		assertThat(repository.findByLastname("newLastname")).hasSize(Long.valueOf(expected).intValue());
	}

	@Test
	void testFinderInvocationWithNullParameter() {

		flushTestUsers();

		repository.findByLastname(null);
	}

	@Test
	void testFindByLastname() {

		flushTestUsers();

		assertThat(repository.findByLastname("Gierke")).containsOnly(firstUser);
	}

	/**
	 * Tests, that searching by the email address of the reference user returns exactly that instance.
	 */
	@Test
	void testFindByEmailAddress() {

		flushTestUsers();

		assertThat(repository.findByEmailAddress("gierke@synyx.de")).isEqualTo(firstUser);
	}

	/**
	 * Tests reading all users.
	 */
	@Test
	void testReadAll() {

		flushTestUsers();

		assertThat(repository.count()).isEqualTo(4L);
		assertThat(repository.findAll()).contains(firstUser, secondUser, thirdUser, fourthUser);
	}

	/**
	 * Tests that all users get deleted by triggering {@link UserRepository#deleteAll()}.
	 */
	@Test
	void deleteAll() {

		flushTestUsers();

		repository.deleteAll();

		assertThat(repository.count()).isZero();
	}

	@Test // DATAJPA-137
	void deleteAllInBatch() {

		flushTestUsers();

		repository.deleteAllInBatch();

		assertThat(repository.count()).isZero();
	}

	/**
	 * Tests cascading persistence.
	 */
	@Test
	void testCascadesPersisting() {

		// Create link prior to persisting
		firstUser.addColleague(secondUser);

		// Persist
		flushTestUsers();

		// Fetches first user from database
		User firstReferenceUser = repository.findById(firstUser.getId()).get();
		assertThat(firstReferenceUser).isEqualTo(firstUser);

		// Fetch colleagues and assert link
		Set<User> colleagues = firstReferenceUser.getColleagues();
		assertThat(colleagues).containsOnly(secondUser);
	}

	/**
	 * Tests, that persisting a relationsship without cascade attributes throws a {@code DataAccessException}.
	 */
	@Test
	void testPreventsCascadingRolePersisting() {

		firstUser.addRole(new Role("USER"));

		assertThatExceptionOfType(DataAccessException.class).isThrownBy(this::flushTestUsers);
	}

	/**
	 * Tests cascading on {@literal merge} operation.
	 */
	@Test
	void testMergingCascadesCollegueas() {

		firstUser.addColleague(secondUser);
		flushTestUsers();

		firstUser.addColleague(new User("Florian", "Hopf", "hopf@synyx.de"));
		firstUser = repository.save(firstUser);

		User reference = repository.findById(firstUser.getId()).get();
		Set<User> colleagues = reference.getColleagues();

		assertThat(colleagues).hasSize(2);
	}

	@Test
	void testCountsCorrectly() {

		long count = repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		repository.save(user);

		assertThat(repository.count()).isEqualTo(count + 1);
	}

	@Test
	void testInvocationOfCustomImplementation() {

		repository.someCustomMethod(new User());
	}

	@Test
	void testOverwritingFinder() {

		repository.findByOverrridingMethod();
	}

	@Test
	void testUsesQueryAnnotation() {

		assertThat(repository.findByAnnotatedQuery("gierke@synyx.de")).isNull();
	}

	@Test
	void testExecutionOfProjectingMethod() {

		flushTestUsers();
		assertThat(repository.countWithFirstname("Oliver")).isOne();
	}

	@Test
	void executesSpecificationCorrectly() {

		flushTestUsers();
		assertThat(repository.findAll(where(userHasFirstname("Oliver")))).hasSize(1);
	}

	@Test
	void executesSingleEntitySpecificationCorrectly() {

		flushTestUsers();
		assertThat(repository.findOne(userHasFirstname("Oliver"))).contains(firstUser);
	}

	@Test
	void returnsNullIfNoEntityFoundForSingleEntitySpecification() {

		flushTestUsers();
		assertThat(repository.findOne(userHasLastname("Beauford"))).isNotPresent();
	}

	@Test
	void throwsExceptionForUnderSpecifiedSingleEntitySpecification() {

		flushTestUsers();

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
				.isThrownBy(() -> repository.findOne(userHasFirstnameLike("e")));
	}

	@Test // GH-1943
	void executesCombinedSpecificationsCorrectly() {

		flushTestUsers();
		Specification<User> spec1 = userHasFirstname("Oliver").or(userHasLastname("Arrasz"));
		List<User> users1 = repository.findAll(spec1);
		assertThat(users1).hasSize(2);

		Specification<User> spec2 = Specification.anyOf( //
				userHasFirstname("Oliver"), //
				userHasLastname("Arrasz"));
		List<User> users2 = repository.findAll(spec2);
		assertThat(users2).hasSize(2);

		assertThat(users1).containsExactlyInAnyOrderElementsOf(users2);
	}

	@Test // DATAJPA-253
	void executesNegatingSpecificationCorrectly() {

		flushTestUsers();
		Specification<User> spec = not(userHasFirstname("Oliver")).and(userHasLastname("Arrasz"));

		assertThat(repository.findAll(spec)).containsOnly(secondUser);
	}

	@Test // GH-1943
	void executesCombinedSpecificationsWithPageableCorrectly() {

		flushTestUsers();
		Specification<User> spec1 = userHasFirstname("Oliver").or(userHasLastname("Arrasz"));

		Page<User> users1 = repository.findAll(spec1, PageRequest.of(0, 1));
		assertThat(users1.getSize()).isOne();
		assertThat(users1.hasPrevious()).isFalse();
		assertThat(users1.getTotalElements()).isEqualTo(2L);

		Specification<User> spec2 = Specification.anyOf( //
				userHasFirstname("Oliver"), //
				userHasLastname("Arrasz"));

		Page<User> users2 = repository.findAll(spec2, PageRequest.of(0, 1));
		assertThat(users2.getSize()).isOne();
		assertThat(users2.hasPrevious()).isFalse();
		assertThat(users2.getTotalElements()).isEqualTo(2L);

		assertThat(users1).containsExactlyInAnyOrderElementsOf(users2);
	}

	@Test
	void executesMethodWithAnnotatedNamedParametersCorrectly() {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertThat(repository.findByLastnameOrFirstname("Oliver", "Arrasz")).contains(firstUser, secondUser);
	}

	@Test
	void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertThat(repository.findByFirstnameOrLastname("Oliver", "Arrasz")).containsOnly(firstUser, secondUser);
	}

	@Test
	void executesLikeAndOrderByCorrectly() {

		flushTestUsers();

		assertThat(repository.findByLastnameLikeOrderByFirstnameDesc("%r%")).hasSize(3).containsExactly(fourthUser,
				firstUser, secondUser);
	}

	@Test
	void executesNotLikeCorrectly() {

		flushTestUsers();

		assertThat(repository.findByLastnameNotLike("%er%")).containsOnly(secondUser, thirdUser, fourthUser);
	}

	@Test
	void executesSimpleNotCorrectly() {

		flushTestUsers();

		assertThat(repository.findByLastnameNot("Gierke")).containsOnly(secondUser, thirdUser, fourthUser);
	}

	@Test
	void returnsSameListIfNoSpecGiven() {

		flushTestUsers();
		assertSameElements(repository.findAll(), repository.findAll((Specification<User>) null));
	}

	@Test
	void returnsSameListIfNoSortIsGiven() {

		flushTestUsers();
		assertSameElements(repository.findAll(Sort.unsorted()), repository.findAll());
	}

	@Test
	void returnsSamePageIfNoSpecGiven() {

		Pageable pageable = PageRequest.of(0, 1);

		flushTestUsers();
		assertThat(repository.findAll((Specification<User>) null, pageable)).isEqualTo(repository.findAll(pageable));
	}

	@Test
	void returnsAllAsPageIfNoPageableIsGiven() {

		flushTestUsers();
		assertThat(repository.findAll(Pageable.unpaged())).isEqualTo(new PageImpl<>(repository.findAll()));
	}

	@Test
	void removeDetachedObject() {

		flushTestUsers();

		em.detach(firstUser);
		repository.delete(firstUser);

		assertThat(repository.count()).isEqualTo(3L);
	}

	@Test
	void executesPagedSpecificationsCorrectly() {

		Page<User> result = executeSpecWithSort(Sort.unsorted());
		assertThat(result.getContent()).isSubsetOf(firstUser, thirdUser);
	}

	@Test
	void executesPagedSpecificationsWithSortCorrectly() {

		Page<User> result = executeSpecWithSort(Sort.by(Direction.ASC, "lastname"));

		assertThat(result.getContent()).contains(firstUser).doesNotContain(secondUser, thirdUser);
	}

	@Test
	void executesPagedSpecificationWithSortCorrectly2() {

		Page<User> result = executeSpecWithSort(Sort.by(Direction.DESC, "lastname"));

		assertThat(result.getContent()).contains(thirdUser).doesNotContain(secondUser, firstUser);
	}

	@Test
	void executesQueryMethodWithDeepTraversalCorrectly() {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.saveAll(asList(firstUser, thirdUser));

		assertThat(repository.findByManagerLastname("Arrasz")).containsOnly(firstUser);
		assertThat(repository.findByManagerLastname("Gierke")).containsOnly(thirdUser);
	}

	@Test
	void executesFindByColleaguesLastnameCorrectly() {

		flushTestUsers();

		firstUser.addColleague(secondUser);
		thirdUser.addColleague(firstUser);
		repository.saveAll(asList(firstUser, thirdUser));

		assertThat(repository.findByColleaguesLastname(secondUser.getLastname())).containsOnly(firstUser);

		assertThat(repository.findByColleaguesLastname("Gierke")).containsOnly(thirdUser, secondUser);
	}

	@Test
	void executesFindByNotNullLastnameCorrectly() {

		flushTestUsers();

		assertThat(repository.findByLastnameNotNull()).containsOnly(firstUser, secondUser, thirdUser, fourthUser);
	}

	@Test
	void executesFindByNullLastnameCorrectly() {

		flushTestUsers();
		User forthUser = repository.save(new User("Foo", null, "email@address.com"));

		assertThat(repository.findByLastnameNull()).containsOnly(forthUser);
	}

	@Test
	void findsSortedByLastname() {

		flushTestUsers();

		assertThat(repository.findByEmailAddressLike("%@%", Sort.by(Direction.ASC, "lastname"))).containsExactly(secondUser,
				firstUser, thirdUser, fourthUser);
	}

	@Test
	void findsUsersBySpringDataNamedQuery() {

		flushTestUsers();

		assertThat(repository.findBySpringDataNamedQuery("Gierke")).containsOnly(firstUser);
	}

	@Test // DATADOC-86
	void readsPageWithGroupByClauseCorrectly() {

		flushTestUsers();

		Page<String> result = repository.findByLastnameGrouped(PageRequest.of(0, 10));
		assertThat(result.getTotalPages()).isOne();
	}

	@Test
	void executesLessThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		assertThat(repository.findByAgeLessThanEqual(35)).containsOnly(firstUser, secondUser, fourthUser);
	}

	@Test
	void executesGreaterThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		assertThat(repository.findByAgeGreaterThanEqual(35)).containsOnly(secondUser, thirdUser);
	}

	@Test // DATAJPA-117
	void executesNativeQueryCorrectly() {

		flushTestUsers();

		assertThat(repository.findNativeByLastname("Matthews")).containsOnly(thirdUser);
	}

	@Test // DATAJPA-132
	void executesFinderWithTrueKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		assertThat(repository.findByActiveTrue()).containsOnly(secondUser, thirdUser, fourthUser);
	}

	@Test // DATAJPA-132
	void executesFinderWithFalseKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		assertThat(repository.findByActiveFalse()).containsOnly(firstUser);
	}

	/**
	 * Ignored until the query declaration is supported by OpenJPA.
	 */
	@Test
	void executesAnnotatedCollectionMethodCorrectly() {

		flushTestUsers();
		firstUser.addColleague(thirdUser);
		repository.save(firstUser);

		List<User> result = repository.findColleaguesFor(firstUser);
		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-188
	void executesFinderWithAfterKeywordCorrectly() {

		flushTestUsers();

		assertThat(repository.findByCreatedAtAfter(secondUser.getCreatedAt())).containsOnly(thirdUser, fourthUser);
	}

	@Test // DATAJPA-188
	void executesFinderWithBeforeKeywordCorrectly() {

		flushTestUsers();

		assertThat(repository.findByCreatedAtBefore(thirdUser.getCreatedAt())).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-180
	void executesFinderWithStartingWithCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameStartingWith("Oli")).containsOnly(firstUser);
	}

	@Test // DATAJPA-180
	void executesFinderWithEndingWithCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameEndingWith("er")).containsOnly(firstUser);
	}

	@Test // DATAJPA-180
	void executesFinderWithContainingCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameContaining("a")).containsOnly(secondUser, thirdUser);
	}

	@Test // DATAJPA-201
	void allowsExecutingPageableMethodWithUnpagedArgument() {

		flushTestUsers();

		assertThat(repository.findByFirstname("Oliver", null)).containsOnly(firstUser);

		Page<User> page = repository.findByFirstnameIn(Pageable.unpaged(), "Oliver");
		assertThat(page.getNumberOfElements()).isOne();
		assertThat(page.getContent()).contains(firstUser);

		page = repository.findAll(Pageable.unpaged());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getContent()).contains(firstUser, secondUser, thirdUser, fourthUser);
	}

	@Test // DATAJPA-207
	void executesNativeQueryForNonEntitiesCorrectly() {

		flushTestUsers();

		List<Integer> result = repository.findOnesByNativeQuery();

		assertThat(result).hasSize(4);
		assertThat(result).contains(1);
	}

	@Test // DATAJPA-232
	void handlesIterableOfIdsCorrectly() {

		flushTestUsers();

		Set<Integer> set = new HashSet<>();
		set.add(firstUser.getId());
		set.add(secondUser.getId());

		assertThat(repository.findAllById(set)).containsOnly(firstUser, secondUser);
	}

	void flushTestUsers() {

		em.persist(adminRole);

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);
		fourthUser = repository.save(fourthUser);

		repository.flush();

		id = firstUser.getId();

		assertThat(id).isNotNull();
		assertThat(secondUser.getId()).isNotNull();
		assertThat(thirdUser.getId()).isNotNull();
		assertThat(fourthUser.getId()).isNotNull();

		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(secondUser.getId())).isTrue();
		assertThat(repository.existsById(thirdUser.getId())).isTrue();
		assertThat(repository.existsById(fourthUser.getId())).isTrue();
	}

	private static <T> void assertSameElements(Collection<T> first, Collection<T> second) {

		for (T element : first) {
			assertThat(element).isIn(second);
		}

		for (T element : second) {
			assertThat(element).isIn(first);
		}
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<User> collection) {

		flushTestUsers();
		long count = repository.count();

		repository.deleteAll(collection);
		assertThat(repository.count()).isEqualTo(count);
	}

	@Test
	void ordersByReferencedEntityCorrectly() {

		flushTestUsers();
		firstUser.setManager(thirdUser);
		repository.save(firstUser);

		Page<User> all = repository.findAll(PageRequest.of(0, 10, Sort.by("manager.id")));

		assertThat(all.getContent()).isNotEmpty();
	}

	@Test // DATAJPA-252
	void bindsSortingToOuterJoinCorrectly() {

		flushTestUsers();

		// Managers not set, make sure adding the sort does not rule out those Users
		Page<User> result = repository.findAllPaged(PageRequest.of(0, 10, Sort.by("manager.lastname")));
		assertThat(result.getContent()).hasSize((int) repository.count());
	}

	@Test // DATAJPA-277
	void doesNotDropNullValuesOnPagedSpecificationExecution() {

		flushTestUsers();

		Page<User> page = repository.findAll(new Specification<User>() {
			@Override
			public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("lastname"), "Gierke");
			}
		}, PageRequest.of(0, 20, Sort.by("manager.lastname")));

		assertThat(page.getNumberOfElements()).isOne();
		assertThat(page).containsOnly(firstUser);
	}

	@Test // DATAJPA-346
	void shouldGenerateLeftOuterJoinInfindAllWithPaginationAndSortOnNestedPropertyPath() {

		firstUser.setManager(null);
		secondUser.setManager(null);
		thirdUser.setManager(firstUser); // manager Oliver
		fourthUser.setManager(secondUser); // manager Joachim

		flushTestUsers();

		Page<User> pages = repository.findAll(PageRequest.of(0, 4, Sort.by(Sort.Direction.ASC, "manager.firstname")));
		assertThat(pages.getSize()).isEqualTo(4);
		assertThat(pages.getContent().get(0).getManager()).isNull();
		assertThat(pages.getContent().get(1).getManager()).isNull();
		assertThat(pages.getContent().get(2).getManager().getFirstname()).isEqualTo("Joachim");
		assertThat(pages.getContent().get(3).getManager().getFirstname()).isEqualTo("Oliver");
		assertThat(pages.getTotalElements()).isEqualTo(4L);
	}

	@Test // DATAJPA-292
	void executesManualQueryWithPositionLikeExpressionCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByFirstnameLike("Da");

		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-292
	void executesManualQueryWithNamedLikeExpressionCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByFirstnameLikeNamed("Da");

		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-231
	void executesDerivedCountQueryToLong() {

		flushTestUsers();

		assertThat(repository.countByLastname("Matthews")).isOne();
	}

	@Test // DATAJPA-231
	void executesDerivedCountQueryToInt() {

		flushTestUsers();

		assertThat(repository.countUsersByFirstname("Dave")).isOne();
	}

	@Test // DATAJPA-231
	void executesDerivedExistsQuery() {

		flushTestUsers();

		assertThat(repository.existsByLastname("Matthews")).isTrue();
		assertThat(repository.existsByLastname("Hans Peter")).isFalse();
	}

	@Test // DATAJPA-332, DATAJPA-1168
	void findAllReturnsEmptyIterableIfNoIdsGiven() {

		assertThat(repository.findAllById(Collections.<Integer> emptySet())).isEmpty();
	}

	@Test // DATAJPA-391
	void executesManuallyDefinedQueryWithFieldProjection() {

		flushTestUsers();
		List<String> lastname = repository.findFirstnamesByLastname("Matthews");

		assertThat(lastname).containsOnly("Dave");
	}

	@Test // DATAJPA-83
	void looksUpEntityReference() {

		flushTestUsers();

		User result = repository.getOne(firstUser.getId());
		assertThat(result).isEqualTo(firstUser);
	}

	@Test // GH-1697
	void looksUpEntityReferenceUsingGetById() {

		flushTestUsers();

		User result = repository.getById(firstUser.getId());
		assertThat(result).isEqualTo(firstUser);
	}

	@Test // GH-2232
	void looksUpEntityReferenceUsingGetReferenceById() {

		flushTestUsers();

		User result = repository.getReferenceById(firstUser.getId());
		assertThat(result).isEqualTo(firstUser);
	}

	@Test // DATAJPA-415
	void invokesQueryWithVarargsParametersCorrectly() {

		flushTestUsers();

		Collection<User> result = repository.findByIdIn(firstUser.getId(), secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-415
	void shouldSupportModifyingQueryWithVarArgs() {

		flushTestUsers();

		repository.updateUserActiveState(false, firstUser.getId(), secondUser.getId(), thirdUser.getId(),
				fourthUser.getId());

		long expectedCount = repository.count();
		assertThat(repository.findByActiveFalse()).hasSize((int) expectedCount);
		assertThat(repository.findByActiveTrue()).isEmpty();
	}

	@Test // DATAJPA-405
	void executesFinderWithOrderClauseOnly() {

		flushTestUsers();

		assertThat(repository.findAllByOrderByLastnameAsc()).containsOnly(secondUser, firstUser, thirdUser, fourthUser);
	}

	@Test // DATAJPA-427
	void sortByAssociationPropertyShouldUseLeftOuterJoin() {

		secondUser.getColleagues().add(firstUser);
		fourthUser.getColleagues().add(thirdUser);
		flushTestUsers();

		List<User> result = repository.findAll(Sort.by(Sort.Direction.ASC, "colleagues.id"));

		assertThat(result).hasSize(4);
	}

	@Test // DATAJPA-427
	void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {

		secondUser.getColleagues().add(firstUser);
		fourthUser.getColleagues().add(thirdUser);
		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "colleagues.id")));

		assertThat(page.getContent()).hasSize(4);
	}

	@Test // DATAJPA-427
	void sortByEmbeddedProperty() {

		thirdUser.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));
		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(thirdUser);
	}

	@Test // DATAJPA-454
	void findsUserByBinaryDataReference() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		List<User> result = repository.findByBinaryData(data);
		assertThat(result).containsOnly(firstUser);
		assertThat(result.get(0).getBinaryData()).isEqualTo(data);
	}

	@Test // DATAJPA-461
	void customFindByQueryWithPositionalVarargsParameters() {

		flushTestUsers();

		Collection<User> result = repository.findByIdsCustomWithPositionalVarArgs(firstUser.getId(), secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-461
	void customFindByQueryWithNamedVarargsParameters() {

		flushTestUsers();

		Collection<User> result = repository.findByIdsCustomWithNamedVarArgs(firstUser.getId(), secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-464
	void saveAndFlushShouldSupportReturningSubTypesOfRepositoryEntity() {

		repository.deleteAll();
		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		SpecialUser savedUser = repository.saveAndFlush(user);

		assertThat(user.getFirstname()).isEqualTo(savedUser.getFirstname());
		assertThat(user.getEmailAddress()).isEqualTo(savedUser.getEmailAddress());
	}

	@Test // gh-2148
	void saveAllAndFlushShouldSupportReturningSubTypesOfRepositoryEntity() {

		repository.deleteAll();
		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		List<SpecialUser> savedUsers = repository.saveAllAndFlush(Collections.singletonList(user));

		assertThat(user.getFirstname()).isEqualTo(savedUsers.get(0).getFirstname());
		assertThat(user.getEmailAddress()).isEqualTo(savedUsers.get(0).getEmailAddress());
	}

	@Test // DATAJPA-218
	void findAllByUntypedExampleShouldReturnSubTypesOfRepositoryEntity() {

		flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		repository.saveAndFlush(user);

		List<User> result = repository
				.findAll(Example.of(new User(), ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "dateOfBirth")));

		assertThat(result).hasSize(5);
	}

	@Test // DATAJPA-218
	void findAllByTypedUserExampleShouldReturnSubTypesOfRepositoryEntity() {

		flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		repository.saveAndFlush(user);

		Example<User> example = Example.of(new User(), matching().withIgnorePaths("age", "createdAt", "dateOfBirth"));
		List<User> result = repository.findAll(example);

		assertThat(result).hasSize(5);
	}

	@Test // DATAJPA-218
	void findAllByTypedSpecialUserExampleShouldReturnSubTypesOfRepositoryEntity() {

		flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		repository.saveAndFlush(user);

		Example<SpecialUser> example = Example.of(new SpecialUser(),
				matching().withIgnorePaths("age", "createdAt", "dateOfBirth"));
		List<SpecialUser> result = repository.findAll(example);

		assertThat(result).hasSize(1);
	}

	@Test // DATAJPA-491
	void sortByNestedAssociationPropertyWithSortInPageable() {

		firstUser.setManager(thirdUser);
		thirdUser.setManager(fourthUser);

		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, //
				Sort.by(Sort.Direction.ASC, "manager.manager.firstname")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-510
	void sortByNestedAssociationPropertyWithSortOrderIgnoreCaseInPageable() {

		firstUser.setManager(thirdUser);
		thirdUser.setManager(fourthUser);

		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, //
				Sort.by(new Sort.Order(Direction.ASC, "manager.manager.firstname").ignoreCase())));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-496
	void findByElementCollectionAttribute() {

		firstUser.getAttributes().add("cool");
		secondUser.getAttributes().add("hip");
		thirdUser.getAttributes().add("rockstar");

		flushTestUsers();

		List<User> result = repository.findByAttributesIn(new HashSet<>(asList("cool", "hip")));

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-460
	void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		flushTestUsers();

		List<User> result = repository.deleteByLastname(firstUser.getLastname());
		assertThat(result).containsOnly(firstUser);
	}

	@Test // DATAJPA-460
	void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		flushTestUsers();

		repository.deleteByLastname(firstUser.getLastname());
		assertThat(repository.countByLastname(firstUser.getLastname())).isZero();
	}

	@Test // DATAJPA-460
	void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		flushTestUsers();

		assertThat(repository.removeByLastname(firstUser.getLastname())).isOne();
	}

	@Test // DATAJPA-460
	void deleteByShouldReturnZeroInCaseNoEntityHasBeenRemovedAndReturnTypeIsNumber() {

		flushTestUsers();

		assertThat(repository.removeByLastname("bubu")).isZero();
	}

	@Test // DATAJPA-460
	void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		flushTestUsers();

		assertThat(repository.deleteByLastname("dorfuaeB")).isEmpty();
	}

	/**
	 * @see <a href="https://issues.apache.org/jira/browse/OPENJPA-2484">OPENJPA-2484</a>
	 */
	@Test // DATAJPA-505
	@Disabled
	void findBinaryDataByIdJpaQl() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		byte[] result = repository.findBinaryDataByIdNative(firstUser.getId());

		assertThat(result).hasSameSizeAs(data);
		assertThat(result).isEqualTo(data);
	}

	@Test // DATAJPA-506
	void findBinaryDataByIdNative() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		byte[] result = repository.findBinaryDataByIdNative(firstUser.getId());

		assertThat(result).isEqualTo(data);
		assertThat(result).hasSameSizeAs(data);
	}

	@Test // DATAJPA-456
	void findPaginatedExplicitQueryWithCountQueryProjection() {

		firstUser.setFirstname(null);

		flushTestUsers();

		Page<User> result = repository.findAllByFirstnameLike("", PageRequest.of(0, 10));

		assertThat(result.getContent()).hasSize(3);
	}

	@Test // DATAJPA-456
	void findPaginatedNamedQueryWithCountQueryProjection() {

		flushTestUsers();

		Page<User> result = repository.findByNamedQueryAndCountProjection("Gierke", PageRequest.of(0, 10));

		assertThat(result.getContent()).hasSize(1);
	}

	@Test // DATAJPA-551
	void findOldestUser() {

		flushTestUsers();

		User oldest = thirdUser;

		assertThat(repository.findFirstByOrderByAgeDesc()).isEqualTo(oldest);
		assertThat(repository.findFirst1ByOrderByAgeDesc()).isEqualTo(oldest);
	}

	@Test // DATAJPA-551
	void findYoungestUser() {

		flushTestUsers();

		User youngest = firstUser;

		assertThat(repository.findTopByOrderByAgeAsc()).isEqualTo(youngest);
		assertThat(repository.findTop1ByOrderByAgeAsc()).isEqualTo(youngest);
	}

	@Test // DATAJPA-551
	void find2OldestUsers() {

		flushTestUsers();

		User oldest1 = thirdUser;
		User oldest2 = secondUser;

		assertThat(repository.findFirst2ByOrderByAgeDesc()).contains(oldest1, oldest2);
		assertThat(repository.findTop2ByOrderByAgeDesc()).contains(oldest1, oldest2);
	}

	@Test // DATAJPA-551
	void find2YoungestUsers() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;

		assertThat(repository.findFirst2UsersBy(Sort.by(ASC, "age"))).contains(youngest1, youngest2);
		assertThat(repository.findTop2UsersBy(Sort.by(ASC, "age"))).contains(youngest1, youngest2);
	}

	@Test // DATAJPA-551
	void find3YoungestUsersPageableWithPageSize2() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Page<User> firstPage = repository.findFirst3UsersBy(PageRequest.of(0, 2, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = repository.findFirst3UsersBy(PageRequest.of(1, 2, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test // DATAJPA-551
	void find2YoungestUsersPageableWithPageSize3() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Page<User> firstPage = repository.findFirst2UsersBy(PageRequest.of(0, 3, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Page<User> secondPage = repository.findFirst2UsersBy(PageRequest.of(1, 3, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test // DATAJPA-551
	void find3YoungestUsersPageableWithPageSize2Sliced() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Slice<User> firstPage = repository.findTop3UsersBy(PageRequest.of(0, 2, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<User> secondPage = repository.findTop3UsersBy(PageRequest.of(1, 2, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test // DATAJPA-551
	void find2YoungestUsersPageableWithPageSize3Sliced() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Slice<User> firstPage = repository.findTop2UsersBy(PageRequest.of(0, 3, ASC, "age"));
		assertThat(firstPage.getContent()).contains(youngest1, youngest2);

		Slice<User> secondPage = repository.findTop2UsersBy(PageRequest.of(1, 3, ASC, "age"));
		assertThat(secondPage.getContent()).contains(youngest3);
	}

	@Test // DATAJPA-912
	void pageableQueryReportsTotalFromResult() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(PageRequest.of(0, 10));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = repository.findAll(PageRequest.of(1, 3));
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test // DATAJPA-912
	void pageableQueryReportsTotalFromCount() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(PageRequest.of(0, 4));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = repository.findAll(PageRequest.of(10, 10));
		assertThat(secondPage.getContent()).isEmpty();
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test // DATAJPA-506
	void invokesQueryWithWrapperType() {

		flushTestUsers();

		Optional<User> result = repository.findOptionalByEmailAddress("gierke@synyx.de");

		assertThat(result).isPresent();
		assertThat(result).contains(firstUser);
	}

	@Test // DATAJPA-564
	void shouldFindUserByFirstnameAndLastnameWithSpelExpressionInStringBasedQuery() {

		flushTestUsers();
		List<User> users = repository.findByFirstnameAndLastnameWithSpelExpression("Oliver", "ierk");

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	void shouldFindUserByLastnameWithSpelExpressionInStringBasedQuery() {

		flushTestUsers();
		List<User> users = repository.findByLastnameWithSpelExpression("ierk");

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	void shouldFindBySpELExpressionWithoutArgumentsWithQuestionmark() {

		flushTestUsers();
		List<User> users = repository.findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	void shouldFindBySpELExpressionWithoutArgumentsWithColon() {

		flushTestUsers();
		List<User> users = repository.findOliverBySpELExpressionWithoutArgumentsWithColon();

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	void shouldFindUsersByAgeForSpELExpression() {

		flushTestUsers();
		List<User> users = repository.findUsersByAgeForSpELExpressionByIndexedParameter(35);

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	void shouldfindUsersByFirstnameForSpELExpressionWithParameterNameVariableReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpression("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	void shouldFindCurrentUserWithCustomQueryDependingOnSecurityContext() {

		flushTestUsers();

		SampleSecurityContextHolder.getCurrent().setPrincipal(secondUser);
		List<User> users = repository.findCurrentUserWithCustomQuery();

		assertThat(users).containsOnly(secondUser);

		SampleSecurityContextHolder.getCurrent().setPrincipal(firstUser);
		users = repository.findCurrentUserWithCustomQuery();

		assertThat(users).contains(firstUser);
	}

	@Test // DATAJPA-564
	void shouldFindByFirstnameAndCurrentUserWithCustomQuery() {

		flushTestUsers();

		SampleSecurityContextHolder.getCurrent().setPrincipal(secondUser);
		List<User> users = repository.findByFirstnameAndCurrentUserWithCustomQuery("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterNameVariableReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpressionWithParameterVariableOnly("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterIndexReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpressionWithParameterIndexOnly("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	void shouldFindUsersInNativeQueryWithPagination() {

		flushTestUsers();

		Page<User> users = repository.findUsersInNativeQueryWithPagination(PageRequest.of(0, 3));

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(users.getContent()).extracting(User::getFirstname).containsExactly("Dave", "Joachim", "kevin");

		users = repository.findUsersInNativeQueryWithPagination(PageRequest.of(1, 3));

		softly.assertThat(users.getContent()).extracting(User::getFirstname).containsExactly("Oliver");

		softly.assertAll();
	}

	@Test // DATAJPA-1140
	void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsStringInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(firstUser,
				firstUser.getLastname());

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	void shouldFindUsersByFirstnameAsStringAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository.findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(firstUser.getFirstname(),
				firstUser);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpressionInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(firstUser,
				firstUser.getLastname());

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	void shouldFindUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository
				.findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(firstUser.getFirstname(), firstUser);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	void shouldFindUsersByFirstnameWithLeadingPageableParameter() {

		flushTestUsers();

		List<User> users = repository.findUsersByFirstnamePaginated(PageRequest.of(0, 2), firstUser.getFirstname());

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-629
	void shouldfindUsersBySpELExpressionParametersWithSpelTemplateExpression() {

		flushTestUsers();
		List<User> users = repository
				.findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression("Joachim", "Arrasz");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-606
	void findByEmptyCollectionOfStrings() {

		flushTestUsers();

		List<User> users = repository.findByAttributesIn(new HashSet<>());
		assertThat(users).isEmpty();
	}

	@Test // DATAJPA-606
	void findByEmptyCollectionOfIntegers() {

		flushTestUsers();

		List<User> users = repository.findByAgeIn(Collections.emptyList());
		assertThat(users).isEmpty();
	}

	@Test // GH-2013
	void findByCollectionWithPageable() {

		flushTestUsers();

		Page<User> userPage = repository.findByAgeIn(List.of(28, 35), (Pageable) PageRequest.of(0, 2));

		assertThat(userPage).hasSize(2);
		assertThat(userPage.getTotalElements()).isEqualTo(2);
		assertThat(userPage.getTotalPages()).isOne();
		assertThat(userPage.getContent()).containsExactlyInAnyOrder(firstUser, secondUser);
	}

	@Test // GH-2013
	void findByCollectionWithPageRequest() {

		flushTestUsers();

		Page<User> userPage = repository.findByAgeIn(List.of(28, 35), (PageRequest) PageRequest.of(0, 2));

		assertThat(userPage).hasSize(2);
		assertThat(userPage.getTotalElements()).isEqualTo(2);
		assertThat(userPage.getTotalPages()).isOne();
		assertThat(userPage.getContent()).containsExactlyInAnyOrder(firstUser, secondUser);
	}

	@Test // DATAJPA-606
	void findByEmptyArrayOfIntegers() {

		flushTestUsers();

		List<User> users = repository.queryByAgeIn(new Integer[0]);
		assertThat(users).isEmpty();
	}

	@Test // DATAJPA-606
	void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		flushTestUsers();

		List<User> users = repository.queryByAgeInOrFirstname(new Integer[0], secondUser.getFirstname());
		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-677
	void shouldSupportJava8StreamsForRepositoryFinderMethods() {

		flushTestUsers();

		try (Stream<User> stream = repository.findAllByCustomQueryAndStream()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test // DATAJPA-677
	void shouldSupportJava8StreamsForRepositoryDerivedFinderMethods() {

		flushTestUsers();

		try (Stream<User> stream = repository.readAllByFirstnameNotNull()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test // DATAJPA-677
	void supportsJava8StreamForPageableMethod() {

		flushTestUsers();

		try (Stream<User> stream = repository.streamAllPaged(PageRequest.of(0, 2))) {
			assertThat(stream).hasSize(2);
		}
	}

	@Test // DATAJPA-218
	void findAllByExample() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);
		prototype.setCreatedAt(null);

		List<User> users = repository.findAll(of(prototype));

		assertThat(users).hasSize(1);
		assertThat(users.get(0)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithEmptyProbe() {

		flushTestUsers();

		User prototype = new User();
		prototype.setCreatedAt(null);

		List<User> users = repository
				.findAll(of(prototype, ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "active")));

		assertThat(users).hasSize(4);
	}

	@Test // DATAJPA-218
	void findAllByNullExample() {
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> repository.findAll((Example<User>) null));
	}

	@Test // DATAJPA-218
	void findAllByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithAssociation() {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.saveAll(asList(firstUser, thirdUser));

		User manager = new User();
		manager.setLastname("Arrasz");
		manager.setAge(secondUser.getAge());
		manager.setCreatedAt(null);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setManager(manager);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("age"));
		List<User> users = repository.findAll(example);

		assertThat(users).hasSize(1);
		assertThat(users.get(0)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithEmbedded() {

		flushTestUsers();

		firstUser.setAddress(new Address("germany", "dresden", "", ""));
		repository.save(firstUser);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setAddress(new Address("germany", null, null, null));

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("age"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithStartingStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("Ol");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.STARTING).withIgnorePaths("age", "createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithEndingStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("ver");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.ENDING).withIgnorePaths("age", "createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithRegexStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("^Oliver$");

		Example<User> example = Example.of(prototype, matching().withStringMatcher(StringMatcher.REGEX));
		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class).isThrownBy(() -> repository.findAll(example));
	}

	@Test // DATAJPA-218
	void findAllByExampleWithIgnoreCase() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiVer");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithStringMatcherAndIgnoreCase() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiV");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.STARTING).withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithIncludeNull() {

		flushTestUsers();

		firstUser.setAddress(new Address("andor", "caemlyn", "", ""));

		User fifthUser = new User();
		fifthUser.setEmailAddress("foo@bar.com");
		fifthUser.setActive(firstUser.isActive());
		fifthUser.setAge(firstUser.getAge());
		fifthUser.setFirstname(firstUser.getFirstname());
		fifthUser.setLastname(firstUser.getLastname());

		repository.saveAll(asList(firstUser, fifthUser));

		User prototype = new User();
		prototype.setFirstname(firstUser.getFirstname());

		Example<User> example = Example.of(prototype, matching().withIncludeNullValues().withIgnorePaths("id", "binaryData",
				"lastname", "emailAddress", "age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(fifthUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithPropertySpecifier() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withMatcher("firstname", new GenericPropertyMatcher().startsWith()));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithSort() {

		flushTestUsers();

		User user1 = new User("Oliver", "Srping", "o@s.de");
		user1.setAge(30);

		repository.save(user1);

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		List<User> users = repository.findAll(example, Sort.by(DESC, "age"));

		assertThat(users).hasSize(2).containsExactly(user1, firstUser);
	}

	@Test // DATAJPA-218
	void findAllByExampleWithPageable() {

		flushTestUsers();

		for (int i = 0; i < 99; i++) {
			User user1 = new User("Oliver-" + i, "Srping", "o" + i + "@s.de");
			user1.setAge(30 + i);

			repository.save(user1);
		}

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		Page<User> users = repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age")));

		assertThat(users.getSize()).isEqualTo(10);
		assertThat(users.hasNext()).isTrue();
		assertThat(users.getTotalElements()).isEqualTo(100L);
	}

	@Test // DATAJPA-218
	void findAllByExampleShouldNotAllowCycles() {

		flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		user1.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age"))));
	}

	@Test // DATAJPA-218
	void findAllByExampleShouldNotAllowCyclesOverSeveralInstances() {

		flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		User user2 = new User();
		user2.setFirstname("user2");

		user1.setManager(user2);
		user2.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class)
				.isThrownBy(() -> repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age"))));
	}

	@Test // DATAJPA-218
	void findOneByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));

		assertThat(repository.findOne(example)).contains(firstUser);
	}

	@Test // GH-2294
	void findByFluentExampleWithSorting() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).all());

		assertThat(users).containsExactly(thirdUser, firstUser, fourthUser);
	}

	@Test // GH-2294
	void findByFluentExampleFirstValue() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		User firstUser = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).firstValue());

		assertThat(firstUser).isEqualTo(thirdUser);
	}

	@Test // GH-2294
	void findByFluentExampleOneValue() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class).isThrownBy(() -> repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).oneValue()));
	}

	@Test // GH-2294
	void findByFluentExampleStream() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		Stream<User> userStream = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).stream());

		assertThat(userStream).containsExactly(thirdUser, firstUser, fourthUser);
	}

	@Test // GH-2294
	void findByFluentExamplePage() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		Example<User> userProbe = of(prototype, matching().withIgnorePaths("age", "createdAt", "active")
				.withMatcher("firstname", GenericPropertyMatcher::contains));

		Page<User> page0 = repository.findBy(userProbe, //
				q -> q.sortBy(Sort.by("firstname")).page(PageRequest.of(0, 2)));

		Page<User> page1 = repository.findBy(userProbe, //
				q -> q.sortBy(Sort.by("firstname")).page(PageRequest.of(1, 2)));

		assertThat(page0.getContent()).containsExactly(thirdUser, firstUser);
		assertThat(page1.getContent()).containsExactly(fourthUser);
	}

	@Test // GH-2294
	void findByFluentExampleWithInterfaceBasedProjection() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<UserProjectionInterfaceBased> users = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.as(UserProjectionInterfaceBased.class).all());

		assertThat(users).extracting(UserProjectionInterfaceBased::getFirstname)
				.containsExactlyInAnyOrder(firstUser.getFirstname(), thirdUser.getFirstname(), fourthUser.getFirstname());
	}

	@Test // GH-2294
	void findByFluentExampleWithSimplePropertyPathsDoesntLoadUnrequestedPaths() {

		flushTestUsers();
		// make sure we don't get preinitialized entities back:
		em.clear();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.project("firstname").all());

		// remove the entities, so lazy loading throws an exception
		em.clear();

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
				thirdUser.getFirstname(), fourthUser.getFirstname());

		assertThatExceptionOfType(LazyInitializationException.class) //
				.isThrownBy( //
						() -> users.forEach(u -> u.getRoles().size()) // forces loading of roles
				);
	}

	@Test // GH-2294
	void findByFluentExampleWithCollectionPropertyPathsDoesntLoadUnrequestedPaths() {

		flushTestUsers();
		// make sure we don't get preinitialized entities back:
		em.clear();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.project("firstname", "roles").all());

		// remove the entities, so lazy loading throws an exception
		em.clear();

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
				thirdUser.getFirstname(), fourthUser.getFirstname());

		assertThat(users).allMatch(u -> u.getRoles().isEmpty());
	}

	@Test // GH-2294
	void findByFluentExampleWithComplexPropertyPathsDoesntLoadUnrequestedPaths() {

		flushTestUsers();
		// make sure we don't get preinitialized entities back:
		em.clear();

		User prototype = new User();
		prototype.setFirstname("v");

		List<User> users = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.project("roles.name").all());

		// remove the entities, so lazy loading throws an exception
		em.clear();

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
				thirdUser.getFirstname(), fourthUser.getFirstname());

		assertThat(users).allMatch(u -> u.getRoles().isEmpty());
	}

	@Test // GH-2294
	void findByFluentExampleWithSortedInterfaceBasedProjection() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		List<UserProjectionInterfaceBased> users = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.as(UserProjectionInterfaceBased.class).sortBy(Sort.by("firstname")).all());

		assertThat(users).extracting(UserProjectionInterfaceBased::getFirstname)
				.containsExactlyInAnyOrder(thirdUser.getFirstname(), firstUser.getFirstname(), fourthUser.getFirstname());
	}

	@Test // GH-2294
	void fluentExamplesWithClassBasedDtosNotYetSupported() {

		@Data
		class UserDto {
			String firstname;
		}

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {

			User prototype = new User();
			prototype.setFirstname("v");

			repository.findBy(
					of(prototype,
							matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
									GenericPropertyMatcher::contains)), //
					q -> q.as(UserDto.class).sortBy(Sort.by("firstname")).all());
		});
	}

	@Test // GH-2294
	void countByFluentExample() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		long numOfUsers = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).count());

		assertThat(numOfUsers).isEqualTo(3);
	}

	@Test // GH-2294
	void existsByFluentExample() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("v");

		boolean exists = repository.findBy(
				of(prototype,
						matching().withIgnorePaths("age", "createdAt", "active").withMatcher("firstname",
								GenericPropertyMatcher::contains)), //
				q -> q.sortBy(Sort.by("firstname")).exists());

		assertThat(exists).isTrue();
	}

	@Test // GH-2274
	void findByFluentSpecificationWithSorting() {

		flushTestUsers();

		List<User> users = repository.findBy(userHasFirstnameLike("v"), q -> q.sortBy(Sort.by("firstname")).all());

		assertThat(users).containsExactly(thirdUser, firstUser, fourthUser);
	}

	@Test // GH-2274
	void findByFluentSpecificationFirstValue() {

		flushTestUsers();

		User firstUser = repository.findBy(userHasFirstnameLike("v"), q -> q.sortBy(Sort.by("firstname")).firstValue());

		assertThat(firstUser).isEqualTo(thirdUser);
	}

	@Test // GH-2274
	void findByFluentSpecificationOneValue() {

		flushTestUsers();

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
				.isThrownBy(() -> repository.findBy(userHasFirstnameLike("v"), q -> q.sortBy(Sort.by("firstname")).oneValue()));
	}

	@Test // GH-2274
	void findByFluentSpecificationStream() {

		flushTestUsers();

		Stream<User> userStream = repository.findBy(userHasFirstnameLike("v"),
				q -> q.sortBy(Sort.by("firstname")).stream());

		assertThat(userStream).containsExactly(thirdUser, firstUser, fourthUser);
	}

	@Test // GH-2274
	void findByFluentSpecificationPage() {

		flushTestUsers();

		Page<User> page0 = repository.findBy(userHasFirstnameLike("v"),
				q -> q.sortBy(Sort.by("firstname")).page(PageRequest.of(0, 2)));

		Page<User> page1 = repository.findBy(userHasFirstnameLike("v"),
				q -> q.sortBy(Sort.by("firstname")).page(PageRequest.of(1, 2)));

		assertThat(page0.getContent()).containsExactly(thirdUser, firstUser);
		assertThat(page1.getContent()).containsExactly(fourthUser);
	}

	@Test // GH-2274
	void findByFluentSpecificationWithInterfaceBasedProjection() {

		flushTestUsers();

		List<UserProjectionInterfaceBased> users = repository.findBy(userHasFirstnameLike("v"),
				q -> q.as(UserProjectionInterfaceBased.class).all());

		assertThat(users).extracting(UserProjectionInterfaceBased::getFirstname)
				.containsExactlyInAnyOrder(firstUser.getFirstname(), thirdUser.getFirstname(), fourthUser.getFirstname());
	}

	@Test // GH-2274
	void findByFluentSpecificationWithSimplePropertyPathsDoesntLoadUnrequestedPaths() {

		flushTestUsers();
		// make sure we don't get preinitialized entities back:
		em.clear();

		List<User> users = repository.findBy(userHasFirstnameLike("v"), q -> q.project("firstname").all());

		// remove the entities, so lazy loading throws an exception
		em.clear();

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
				thirdUser.getFirstname(), fourthUser.getFirstname());

		assertThatExceptionOfType(LazyInitializationException.class) //
				.isThrownBy( //
						() -> users.forEach(u -> u.getRoles().size()) // forces loading of roles
				);
	}

	@Test // GH-2274
	void findByFluentSpecificationWithCollectionPropertyPathsDoesntLoadUnrequestedPaths() {

		flushTestUsers();
		// make sure we don't get preinitialized entities back:
		em.clear();

		List<User> users = repository.findBy(userHasFirstnameLike("v"), q -> q.project("firstname", "roles").all());

		// remove the entities, so lazy loading throws an exception
		em.clear();

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
				thirdUser.getFirstname(), fourthUser.getFirstname());

		assertThat(users).allMatch(u -> u.getRoles().isEmpty());
	}

	@Test // GH-2274
	void findByFluentSpecificationWithComplexPropertyPathsDoesntLoadUnrequestedPaths() {

		flushTestUsers();
		// make sure we don't get preinitialized entities back:
		em.clear();

		List<User> users = repository.findBy(userHasFirstnameLike("v"), q -> q.project("roles.name").all());

		// remove the entities, so lazy loading throws an exception
		em.clear();

		assertThat(users).extracting(User::getFirstname).containsExactlyInAnyOrder(firstUser.getFirstname(),
				thirdUser.getFirstname(), fourthUser.getFirstname());

		assertThat(users).allMatch(u -> u.getRoles().isEmpty());
	}

	@Test // GH-2274
	void findByFluentSpecificationWithSortedInterfaceBasedProjection() {

		flushTestUsers();

		List<UserProjectionInterfaceBased> users = repository.findBy(userHasFirstnameLike("v"),
				q -> q.as(UserProjectionInterfaceBased.class).sortBy(Sort.by("firstname")).all());

		assertThat(users).extracting(UserProjectionInterfaceBased::getFirstname)
				.containsExactlyInAnyOrder(thirdUser.getFirstname(), firstUser.getFirstname(), fourthUser.getFirstname());
	}

	@Test // GH-2274
	void fluentSpecificationWithClassBasedDtosNotYetSupported() {

		@Data
		class UserDto {
			String firstname;
		}

		assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> {
			repository.findBy(userHasFirstnameLike("v"), q -> q.as(UserDto.class).sortBy(Sort.by("firstname")).all());
		});
	}

	@Test // GH-2274
	void countByFluentSpecification() {

		flushTestUsers();

		long numOfUsers = repository.findBy(userHasFirstnameLike("v"), q -> q.sortBy(Sort.by("firstname")).count());

		assertThat(numOfUsers).isEqualTo(3);
	}

	@Test // GH-2274
	void existsByFluentSpecification() {

		flushTestUsers();

		boolean exists = repository.findBy(userHasFirstnameLike("v"), q -> q.sortBy(Sort.by("firstname")).exists());

		assertThat(exists).isTrue();
	}

	@Test // DATAJPA-218
	void countByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		long count = repository.count(example);

		assertThat(count).isOne();
	}

	@Test // DATAJPA-218
	void existsByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		boolean exists = repository.exists(example);

		assertThat(exists).isTrue();
	}

	@Test // GH-2368
	void existsByExampleNegative() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(4711); // there is none with that age

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		boolean exists = repository.exists(example);

		assertThat(exists).isFalse();
	}

	@Test // DATAJPA-905
	void executesPagedSpecificationSettingAnOrder() {

		flushTestUsers();

		Page<User> result = repository.findAll(userHasLastnameLikeWithSort("e"), PageRequest.of(0, 1));

		assertThat(result.getTotalElements()).isEqualTo(2L);
		assertThat(result.getNumberOfElements()).isOne();
		assertThat(result.getContent().get(0)).isEqualTo(thirdUser);
	}

	@Test // DATAJPA-1172
	void exceptionsDuringParameterSettingGetThrown() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class) //
				.isThrownBy(() -> repository.findByStringAge("twelve")) //
				.matches(e -> !e.getMessage().contains("Named parameter [age] not set"));
	}

	@Test // DATAJPA-1172
	void queryProvidesCorrectNumberOfParametersForNativeQuery() {

		Query query = em.createNativeQuery("select 1 from User where firstname=? and lastname=?");
		assertThat(query.getParameters()).hasSize(2);
	}

	@Test // DATAJPA-1185
	void dynamicProjectionReturningStream() {

		flushTestUsers();

		assertThat(repository.findAsStreamByFirstnameLike("%O%", User.class)).hasSize(1);
	}

	@Test // DATAJPA-1185
	void dynamicProjectionReturningList() {

		flushTestUsers();

		List<User> users = repository.findAsListByFirstnameLike("%O%", User.class);

		assertThat(users).hasSize(1);
	}

	@Test // DATAJPA-1179
	void duplicateSpelsWorkAsIntended() {

		flushTestUsers();

		List<User> users = repository.findUsersByDuplicateSpel("Oliver");

		assertThat(users).hasSize(1);
	}

	@Test // DATAJPA-980
	void supportsProjectionsWithNativeQueries() {

		flushTestUsers();

		User user = repository.findAll().get(0);

		NameOnly result = repository.findByNativeQuery(user.getId());

		assertThat(result.getFirstname()).isEqualTo(user.getFirstname());
		assertThat(result.getLastname()).isEqualTo(user.getLastname());
	}

	@Test // DATAJPA-1248
	void supportsProjectionsWithNativeQueriesAndCamelCaseProperty() {

		flushTestUsers();
		User user = repository.findAll().get(0);

		UserRepository.EmailOnly result = repository.findEmailOnlyByNativeQuery(user.getId());

		String emailAddress = result.getEmailAddress();

		assertThat(emailAddress) //
				.isEqualTo(user.getEmailAddress()) //
				.as("ensuring email is actually not null") //
				.isNotNull();
	}

	@Test // DATAJPA-1235
	void handlesColonsFollowedByIntegerInStringLiteral() {

		String firstName = "aFirstName";

		User expected = new User(firstName, "000:1", "something@something");
		User notExpected = new User(firstName, "000\\:1", "something@something.else");

		repository.save(expected);
		repository.save(notExpected);

		assertThat(repository.findAll()).hasSize(2);

		List<User> users = repository.queryWithIndexedParameterAndColonFollowedByIntegerInString(firstName);

		assertThat(users).extracting(User::getId).containsExactly(expected.getId());
	}

	@Test // DATAJPA-1233
	void handlesCountQueriesWithLessParametersSingleParam() {
		repository.findAllOrderedBySpecialNameSingleParam("Oliver", PageRequest.of(2, 3));
	}

	@Test // DATAJPA-1233
	void handlesCountQueriesWithLessParametersMoreThanOne() {
		repository.findAllOrderedBySpecialNameMultipleParams("Oliver", "x", PageRequest.of(2, 3));
	}

	@Test // DATAJPA-1233
	void handlesCountQueriesWithLessParametersMoreThanOneIndexed() {
		repository.findAllOrderedBySpecialNameMultipleParamsIndexed("x", "Oliver", PageRequest.of(2, 3));
	}

	// DATAJPA-928
	@Test
	void executeNativeQueryWithPage() {

		flushTestUsers();

		Page<User> firstPage = repository.findByNativeNamedQueryWithPageable(PageRequest.of(0, 3));
		Page<User> secondPage = repository.findByNativeNamedQueryWithPageable(PageRequest.of(1, 3));

		SoftAssertions softly = new SoftAssertions();

		assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		assertThat(firstPage.getContent()) //
				.extracting(User::getFirstname) //
				.containsExactly("Dave", "Joachim", "kevin");

		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		assertThat(secondPage.getNumberOfElements()).isOne();
		assertThat(secondPage.getContent()) //
				.extracting(User::getFirstname) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	// DATAJPA-928
	@Test
	void executeNativeQueryWithPageWorkaround() {

		flushTestUsers();

		Page<String> firstPage = repository.findByNativeQueryWithPageable(PageRequest.of(0, 3));
		Page<String> secondPage = repository.findByNativeQueryWithPageable(PageRequest.of(1, 3));

		SoftAssertions softly = new SoftAssertions();

		assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		assertThat(firstPage.getContent()) //
				.containsExactly("Dave", "Joachim", "kevin");

		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		assertThat(secondPage.getNumberOfElements()).isOne();
		assertThat(secondPage.getContent()) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	@Test // DATAJPA-1273
	void bindsNativeQueryResultsToProjectionByName() {

		flushTestUsers();

		List<NameOnly> result = repository.findByNamedQueryWithAliasInInvertedOrder();

		assertThat(result).element(0).satisfies(it -> {
			assertThat(it.getFirstname()).isEqualTo("Joachim");
			assertThat(it.getLastname()).isEqualTo("Arrasz");
		});
	}

	@Test // DATAJPA-1301
	void returnsNullValueInMap() {

		firstUser.setLastname(null);
		flushTestUsers();

		Map<String, Object> map = repository.findMapWithNullValues();

		SoftAssertions softly = new SoftAssertions();

		softly.assertThat(map.keySet()).containsExactlyInAnyOrder("firstname", "lastname");

		softly.assertThat(map.containsKey("firstname")).isTrue();
		softly.assertThat(map.containsKey("lastname")).isTrue();

		softly.assertThat(map.get("firstname")).isEqualTo("Oliver");
		softly.assertThat(map.get("lastname")).isNull();

		softly.assertThat(map.get("non-existent")).isNull();

		softly.assertThat(map.get(new Object())).isNull();

		softly.assertAll();
	}

	@Test // DATAJPA-1307
	void testFindByEmailAddressJdbcStyleParameter() {

		flushTestUsers();

		assertThat(repository.findByEmailNativeAddressJdbcStyleParameter("gierke@synyx.de")).isEqualTo(firstUser);
	}

	@Test // DATAJPA-1535
	void savingUserThrowsAnException() {
		// if this test fails this means deleteNewInstanceSucceedsByDoingNothing() might actually save the user without the
		// test failing, which would be a bad thing.
		assertThatThrownBy(() -> repository.save(new User())).isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test // DATAJPA-1535
	void deleteNewInstanceSucceedsByDoingNothing() {
		repository.delete(new User());
	}

	@Test // DATAJPA-1303
	void findByElementCollectionInAttributeIgnoreCase() {

		firstUser.getAttributes().add("cOOl");
		secondUser.getAttributes().add("hIp");
		thirdUser.getAttributes().add("roCKsTar");

		flushTestUsers();

		List<User> result = repository.findByAttributesIgnoreCaseIn(new HashSet<>(asList("cOOl", "hIP")));

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-1303
	void findByElementCollectionNotInAttributeIgnoreCase() {

		firstUser.getAttributes().add("cOOl");
		secondUser.getAttributes().add("hIp");
		thirdUser.getAttributes().add("rOckStAr");

		flushTestUsers();

		List<User> result = repository.findByAttributesIgnoreCaseNotIn(asList("CooL", "HIp"));

		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-1303
	void findByElementVarargInAttributeIgnoreCase() {

		firstUser.getAttributes().add("cOOl");
		secondUser.getAttributes().add("hIp");
		thirdUser.getAttributes().add("rOckStAr");

		flushTestUsers();

		Page<User> result = repository.findByAttributesIgnoreCaseIn(PageRequest.of(0, 20), "CooL", "HIp");

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-1303
	void findByElementCollectionInAttributeIgnoreCaseWithNulls() {

		firstUser.getAttributes().add("cOOl");
		secondUser.getAttributes().add("hIp");
		thirdUser.getAttributes().add("roCKsTar");

		flushTestUsers();

		List<User> result = repository.findByAttributesIgnoreCaseIn(asList("cOOl", null));

		assertThat(result).containsOnly(firstUser);
	}

	@Test // #2363
	void readsDtoProjections() {

		flushTestUsers();

		assertThat(repository.findAllDtoProjectedBy()).hasSize(4);
	}

	@Test // GH-2408, GH-2363
	void readsDerivedInterfaceProjections() {

		flushTestUsers();

		assertThat(repository.findAllInterfaceProjectedBy()).hasSize(4);
	}

	@Test // GH-2388
	void existsWithSpec() {

		flushTestUsers();

		Specification<User> minorSpec = userHasAgeLess(18);
		Specification<User> hundredYearsOld = userHasAgeLess(100);

		assertThat(repository.exists(minorSpec)).isFalse();
		assertThat(repository.exists(hundredYearsOld)).isTrue();
	}

	@Test // GH-2555
	void modifyingUpdateNativeQueryWorksWithJSQLParser() {

		flushTestUsers();

		Optional<User> byIdUser = repository.findById(firstUser.getId());
		assertThat(byIdUser).isPresent().map(User::isActive).get().isEqualTo(true);

		repository.setActiveToFalseWithModifyingNative(byIdUser.get().getId());

		Optional<User> afterUpdate = repository.findById(firstUser.getId());
		assertThat(afterUpdate).isPresent().map(User::isActive).get().isEqualTo(false);
	}

	@Test // GH-1262
	void deleteWithSpec() {

		flushTestUsers();

		Specification<User> usersWithEInTheirName = userHasFirstnameLike("e");

		long initialCount = repository.count();
		assertThat(repository.delete(usersWithEInTheirName)).isEqualTo(3L);
		long finalCount = repository.count();
		assertThat(initialCount - finalCount).isEqualTo(3L);
	}

	@Test // GH-2045, GH-425
	public void correctlyBuildSortClauseWhenSortingByFunctionAliasAndFunctionContainsPositionalParameters() {
		repository.findAllAndSortByFunctionResultPositionalParameter("prefix", "suffix", Sort.by("idWithPrefixAndSuffix"));
	}

	@Test // GH-2045, GH-425
	public void correctlyBuildSortClauseWhenSortingByFunctionAliasAndFunctionContainsNamedParameters() {
		repository.findAllAndSortByFunctionResultNamedParameter("prefix", "suffix", Sort.by("idWithPrefixAndSuffix"));
	}

	@Test // GH-2578
	void simpleNativeExceptTest() {

		flushTestUsers();

		List<String> foundIds = repository.findWithSimpleExceptNative();

		assertThat(foundIds) //
				.isNotEmpty() //
				.contains("Oliver", "kevin");
	}

	@Test // GH-2578
	void simpleNativeUnionTest() {

		flushTestUsers();

		List<String> foundIds = repository.findWithSimpleUnionNative();

		assertThat(foundIds) //
				.isNotEmpty() //
				.containsExactlyInAnyOrder("Dave", "Joachim", "Oliver", "kevin");
	}

	@Test // GH-2578
	void complexNativeExceptTest() {

		flushTestUsers();

		List<String> foundIds = repository.findWithComplexExceptNative();

		assertThat(foundIds).containsExactly("Oliver", "kevin");
	}

	@Test // GH-2578
	void simpleValuesStatementNative() {

		flushTestUsers();

		List<Integer> foundIds = repository.valuesStatementNative();

		assertThat(foundIds).containsExactly(1);
	}

	@Test // GH-2578
	void withStatementNative() {

		flushTestUsers();

		List<User> foundData = repository.withNativeStatement();

		assertThat(foundData) //
				.map(User::getFirstname) //
				.containsExactly("Joachim", "Dave", "kevin");
	}

	@Test // GH-2578
	void complexWithNativeStatement() {

		flushTestUsers();

		List<String> foundData = repository.complexWithNativeStatement();

		assertThat(foundData).containsExactly("joachim", "dave", "kevin");
	}

	@Test // GH-2607
	void containsWithCollection() {

		firstUser.getAttributes().add("cool");
		firstUser.getAttributes().add("hip");

		secondUser.getAttributes().add("hip");

		thirdUser.getAttributes().add("rockstar");
		thirdUser.getAttributes().add("%hip%");

		flushTestUsers();

		List<User> result = repository.findByAttributesContains("hip");

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // GH-2593
	void insertStatementModifyingQueryWorks() {
		flushTestUsers();
		repository.insertNewUserWithNativeQuery();

		List<User> all = repository.findAll();
		assertThat(all) //
				.hasSize(5) //
				.map(User::getLastname) //
				.contains("Gierke", "Arrasz", "Matthews", "raymond", "K");
	}

	@Test // GH-2593
	void insertStatementModifyingQueryWithParamsWorks() {
		flushTestUsers();
		String testLastName = "TestLastName";
		repository.insertNewUserWithParamNativeQuery(testLastName);

		List<User> all = repository.findAll();
		assertThat(all) //
				.hasSize(5) //
				.map(User::getLastname) //
				.contains("Gierke", "Arrasz", "Matthews", "raymond", testLastName);
	}

	@Test // GH-2641
	void mergeWithNativeStatement() {

		flushTestUsers();

		assertThat(repository.findById(firstUser.getId())) //
				.map(User::getAge).contains(28);

		// when
		repository.mergeNativeStatement();

		// then
		assertThat(repository.findById(firstUser.getId())) //
				.map(User::getAge).contains(30);
	}

	private Page<User> executeSpecWithSort(Sort sort) {

		flushTestUsers();

		Specification<User> spec1 = userHasFirstname("Oliver").or(userHasLastname("Matthews"));

		Page<User> result1 = repository.findAll(spec1, PageRequest.of(0, 1, sort));
		assertThat(result1.getTotalElements()).isEqualTo(2L);

		Specification<User> spec2 = Specification.anyOf( //
				userHasFirstname("Oliver"), //
				userHasLastname("Matthews"));

		Page<User> result2 = repository.findAll(spec2, PageRequest.of(0, 1, sort));
		assertThat(result2.getTotalElements()).isEqualTo(2L);

		assertThat(result1).containsExactlyElementsOf(result2);

		return result2;
	}

	private interface UserProjectionInterfaceBased {
		String getFirstname();
	}
}
