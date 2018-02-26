/*
 * Copyright 2008-2018 the original author or authors.
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

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.Example.*;
import static org.springframework.data.domain.ExampleMatcher.*;
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.jpa.domain.Specification.*;
import static org.springframework.data.jpa.domain.Specification.not;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.assertj.core.api.SoftAssertions;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.domain.ExampleMatcher.GenericPropertyMatcher;
import org.springframework.data.domain.ExampleMatcher.StringMatcher;
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
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.google.common.base.Optional;

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
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryTests {

	@PersistenceContext EntityManager em;

	// CUT
	@Autowired UserRepository repository;

	// Test fixture
	User firstUser, secondUser, thirdUser, fourthUser;
	Integer id;
	Role adminRole;

	@Before
	public void setUp() throws Exception {

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
	public void testCreation() {

		Query countQuery = em.createQuery("select count(u) from User u");
		Long before = (Long) countQuery.getSingleResult();

		flushTestUsers();

		assertThat((Long) countQuery.getSingleResult()).isEqualTo(before + 4L);
	}

	@Test
	public void testRead() throws Exception {

		flushTestUsers();

		assertThat(repository.findById(id)).map(User::getFirstname).contains(firstUser.getFirstname());
	}

	@Test
	public void findsAllByGivenIds() {

		flushTestUsers();

		assertThat(repository.findAllById(Arrays.asList(firstUser.getId(), secondUser.getId()))).contains(firstUser,
				secondUser);
	}

	@Test
	public void testReadByIdReturnsNullForNotFoundEntities() {

		flushTestUsers();

		assertThat(repository.findById(id * 27)).isNotPresent();
	}

	@Test
	public void savesCollectionCorrectly() throws Exception {

		assertThat(repository.saveAll(Arrays.asList(firstUser, secondUser, thirdUser))).hasSize(3).contains(firstUser,
				secondUser, thirdUser);
	}

	@Test
	public void savingEmptyCollectionIsNoOp() throws Exception {
		assertThat(repository.saveAll(new ArrayList<>())).isEmpty();
	}

	@Test
	public void testUpdate() {

		flushTestUsers();

		User foundPerson = repository.findById(id).get();
		foundPerson.setLastname("Schlicht");

		assertThat(repository.findById(id)).map(User::getFirstname).contains(foundPerson.getFirstname());
	}

	@Test
	public void existReturnsWhetherAnEntityCanBeLoaded() throws Exception {

		flushTestUsers();
		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(id * 27)).isFalse();
	}

	@Test
	public void deletesAUserById() {

		flushTestUsers();

		repository.deleteById(firstUser.getId());

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.findById(id)).isNotPresent();
	}

	@Test
	public void testDelete() {

		flushTestUsers();

		repository.delete(firstUser);

		assertThat(repository.existsById(id)).isFalse();
		assertThat(repository.findById(id)).isNotPresent();
	}

	@Test
	public void returnsAllSortedCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findAll(Sort.by(ASC, "lastname"))).hasSize(4).containsExactly(secondUser, firstUser,
				thirdUser, fourthUser);
	}

	@Test // DATAJPA-296
	public void returnsAllIgnoreCaseSortedCorrectly() throws Exception {

		flushTestUsers();

		Order order = new Order(ASC, "firstname").ignoreCase();

		assertThat(repository.findAll(Sort.by(order))) //
				.hasSize(4)//
				.containsExactly(thirdUser, secondUser, fourthUser, firstUser);
	}

	@Test
	public void deleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteAll(Arrays.asList(firstUser, secondUser));

		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void batchDeleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteInBatch(Arrays.asList(firstUser, secondUser));

		assertThat(repository.existsById(firstUser.getId())).isFalse();
		assertThat(repository.existsById(secondUser.getId())).isFalse();
		assertThat(repository.count()).isEqualTo(before - 2);
	}

	@Test
	public void deleteEmptyCollectionDoesNotDeleteAnything() {

		assertDeleteCallDoesNotDeleteAnything(new ArrayList<User>());
	}

	@Test
	public void executesManipulatingQuery() throws Exception {

		flushTestUsers();
		repository.renameAllUsersTo("newLastname");

		long expected = repository.count();
		assertThat(repository.findByLastname("newLastname").size()).isEqualTo(Long.valueOf(expected).intValue());
	}

	@Test
	public void testFinderInvocationWithNullParameter() {

		flushTestUsers();

		repository.findByLastname((String) null);
	}

	@Test
	public void testFindByLastname() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastname("Gierke")).containsOnly(firstUser);
	}

	/**
	 * Tests, that searching by the email address of the reference user returns exactly that instance.
	 */
	@Test
	public void testFindByEmailAddress() throws Exception {

		flushTestUsers();

		assertThat(repository.findByEmailAddress("gierke@synyx.de")).isEqualTo(firstUser);
	}

	/**
	 * Tests reading all users.
	 */
	@Test
	public void testReadAll() {

		flushTestUsers();

		assertThat(repository.count()).isEqualTo(4L);
		assertThat(repository.findAll()).contains(firstUser, secondUser, thirdUser, fourthUser);
	}

	/**
	 * Tests that all users get deleted by triggering {@link UserRepository#deleteAll()}.
	 */
	@Test
	public void deleteAll() throws Exception {

		flushTestUsers();

		repository.deleteAll();

		assertThat(repository.count()).isZero();
	}

	@Test // DATAJPA-137
	public void deleteAllInBatch() {

		flushTestUsers();

		repository.deleteAllInBatch();

		assertThat(repository.count()).isZero();
	}

	/**
	 * Tests cascading persistence.
	 */
	@Test
	public void testCascadesPersisting() {

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
	@Test(expected = DataAccessException.class)
	public void testPreventsCascadingRolePersisting() {

		firstUser.addRole(new Role("USER"));

		flushTestUsers();
	}

	/**
	 * Tests cascading on {@literal merge} operation.
	 */
	@Test
	public void testMergingCascadesCollegueas() {

		firstUser.addColleague(secondUser);
		flushTestUsers();

		firstUser.addColleague(new User("Florian", "Hopf", "hopf@synyx.de"));
		firstUser = repository.save(firstUser);

		User reference = repository.findById(firstUser.getId()).get();
		Set<User> colleagues = reference.getColleagues();

		assertThat(colleagues).hasSize(2);
	}

	@Test
	public void testCountsCorrectly() {

		long count = repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		repository.save(user);

		assertThat(repository.count()).isEqualTo(count + 1);
	}

	@Test
	public void testInvocationOfCustomImplementation() {

		repository.someCustomMethod(new User());
	}

	@Test
	public void testOverwritingFinder() {

		repository.findByOverrridingMethod();
	}

	@Test
	public void testUsesQueryAnnotation() {

		assertThat(repository.findByAnnotatedQuery("gierke@synyx.de")).isNull();
	}

	@Test
	public void testExecutionOfProjectingMethod() {

		flushTestUsers();
		assertThat(repository.countWithFirstname("Oliver")).isEqualTo(1L);
	}

	@Test
	public void executesSpecificationCorrectly() {

		flushTestUsers();
		assertThat(repository.findAll(where(userHasFirstname("Oliver")))).hasSize(1);
	}

	@Test
	public void executesSingleEntitySpecificationCorrectly() throws Exception {

		flushTestUsers();
		assertThat(repository.findOne(userHasFirstname("Oliver"))).contains(firstUser);
	}

	@Test
	public void returnsNullIfNoEntityFoundForSingleEntitySpecification() throws Exception {

		flushTestUsers();
		assertThat(repository.findOne(userHasLastname("Beauford"))).isNotPresent();
	}

	@Test(expected = IncorrectResultSizeDataAccessException.class)
	public void throwsExceptionForUnderSpecifiedSingleEntitySpecification() {

		flushTestUsers();
		repository.findOne(userHasFirstnameLike("e"));
	}

	@Test
	public void executesCombinedSpecificationsCorrectly() {

		flushTestUsers();
		Specification<User> spec = userHasFirstname("Oliver").or(userHasLastname("Arrasz"));
		assertThat(repository.findAll(spec)).hasSize(2);
	}

	@Test // DATAJPA-253
	public void executesNegatingSpecificationCorrectly() {

		flushTestUsers();
		Specification<User> spec = not(userHasFirstname("Oliver")).and(userHasLastname("Arrasz"));

		assertThat(repository.findAll(spec)).containsOnly(secondUser);
	}

	@Test
	public void executesCombinedSpecificationsWithPageableCorrectly() {

		flushTestUsers();
		Specification<User> spec = userHasFirstname("Oliver").or(userHasLastname("Arrasz"));

		Page<User> users = repository.findAll(spec, PageRequest.of(0, 1));
		assertThat(users.getSize()).isEqualTo(1);
		assertThat(users.hasPrevious()).isFalse();
		assertThat(users.getTotalElements()).isEqualTo(2L);
	}

	@Test
	public void executesMethodWithAnnotatedNamedParametersCorrectly() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertThat(repository.findByLastnameOrFirstname("Oliver", "Arrasz")).contains(firstUser, secondUser);
	}

	@Test
	public void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertThat(repository.findByFirstnameOrLastname("Oliver", "Arrasz")).containsOnly(firstUser, secondUser);
	}

	@Test
	public void executesLikeAndOrderByCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameLikeOrderByFirstnameDesc("%r%")).hasSize(3).containsExactly(fourthUser,
				firstUser, secondUser);
	}

	@Test
	public void executesNotLikeCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameNotLike("%er%")).containsOnly(secondUser, thirdUser, fourthUser);
	}

	@Test
	public void executesSimpleNotCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameNot("Gierke")).containsOnly(secondUser, thirdUser, fourthUser);
	}

	@Test
	public void returnsSameListIfNoSpecGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll(), repository.findAll((Specification<User>) null));
	}

	@Test
	public void returnsSameListIfNoSortIsGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll(Sort.unsorted()), repository.findAll());
	}

	@Test
	public void returnsSamePageIfNoSpecGiven() throws Exception {

		Pageable pageable = PageRequest.of(0, 1);

		flushTestUsers();
		assertThat(repository.findAll((Specification<User>) null, pageable)).isEqualTo(repository.findAll(pageable));
	}

	@Test
	public void returnsAllAsPageIfNoPageableIsGiven() throws Exception {

		flushTestUsers();
		assertThat(repository.findAll(Pageable.unpaged())).isEqualTo(new PageImpl<>(repository.findAll()));
	}

	@Test
	public void removeDetachedObject() throws Exception {

		flushTestUsers();

		em.detach(firstUser);
		repository.delete(firstUser);

		assertThat(repository.count()).isEqualTo(3L);
	}

	@Test
	public void executesPagedSpecificationsCorrectly() throws Exception {

		Page<User> result = executeSpecWithSort(Sort.unsorted());
		assertThat(result.getContent()).isSubsetOf(firstUser, thirdUser);
	}

	@Test
	public void executesPagedSpecificationsWithSortCorrectly() throws Exception {

		Page<User> result = executeSpecWithSort(Sort.by(Direction.ASC, "lastname"));

		assertThat(result.getContent()).contains(firstUser).doesNotContain(secondUser, thirdUser);
	}

	@Test
	public void executesPagedSpecificationWithSortCorrectly2() throws Exception {

		Page<User> result = executeSpecWithSort(Sort.by(Direction.DESC, "lastname"));

		assertThat(result.getContent()).contains(thirdUser).doesNotContain(secondUser, firstUser);
	}

	@Test
	public void executesQueryMethodWithDeepTraversalCorrectly() throws Exception {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.saveAll(Arrays.asList(firstUser, thirdUser));

		assertThat(repository.findByManagerLastname("Arrasz")).containsOnly(firstUser);
		assertThat(repository.findByManagerLastname("Gierke")).containsOnly(thirdUser);
	}

	@Test
	public void executesFindByColleaguesLastnameCorrectly() throws Exception {

		flushTestUsers();

		firstUser.addColleague(secondUser);
		thirdUser.addColleague(firstUser);
		repository.saveAll(Arrays.asList(firstUser, thirdUser));

		assertThat(repository.findByColleaguesLastname(secondUser.getLastname())).containsOnly(firstUser);

		assertThat(repository.findByColleaguesLastname("Gierke")).containsOnly(thirdUser, secondUser);
	}

	@Test
	public void executesFindByNotNullLastnameCorrectly() throws Exception {

		flushTestUsers();

		assertThat(repository.findByLastnameNotNull()).containsOnly(firstUser, secondUser, thirdUser, fourthUser);
	}

	@Test
	public void executesFindByNullLastnameCorrectly() throws Exception {

		flushTestUsers();
		User forthUser = repository.save(new User("Foo", null, "email@address.com"));

		assertThat(repository.findByLastnameNull()).containsOnly(forthUser);
	}

	@Test
	public void findsSortedByLastname() throws Exception {

		flushTestUsers();

		assertThat(repository.findByEmailAddressLike("%@%", Sort.by(Direction.ASC, "lastname"))).containsExactly(secondUser,
				firstUser, thirdUser, fourthUser);
	}

	@Test
	public void findsUsersBySpringDataNamedQuery() {

		flushTestUsers();

		assertThat(repository.findBySpringDataNamedQuery("Gierke")).containsOnly(firstUser);
	}

	@Test // DATADOC-86
	public void readsPageWithGroupByClauseCorrectly() {

		flushTestUsers();

		Page<String> result = repository.findByLastnameGrouped(PageRequest.of(0, 10));
		assertThat(result.getTotalPages()).isEqualTo(1);
	}

	@Test
	public void executesLessThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		assertThat(repository.findByAgeLessThanEqual(35)).containsOnly(firstUser, secondUser, fourthUser);
	}

	@Test
	public void executesGreaterThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		assertThat(repository.findByAgeGreaterThanEqual(35)).containsOnly(secondUser, thirdUser);
	}

	@Test // DATAJPA-117
	public void executesNativeQueryCorrectly() {

		flushTestUsers();

		assertThat(repository.findNativeByLastname("Matthews")).containsOnly(thirdUser);
	}

	@Test // DATAJPA-132
	public void executesFinderWithTrueKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		assertThat(repository.findByActiveTrue()).containsOnly(secondUser, thirdUser, fourthUser);
	}

	@Test // DATAJPA-132
	public void executesFinderWithFalseKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		assertThat(repository.findByActiveFalse()).containsOnly(firstUser);
	}

	/**
	 * Ignored until the query declaration is supported by OpenJPA.
	 */
	@Test
	public void executesAnnotatedCollectionMethodCorrectly() {

		flushTestUsers();
		firstUser.addColleague(thirdUser);
		repository.save(firstUser);

		List<User> result = repository.findColleaguesFor(firstUser);
		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-188
	public void executesFinderWithAfterKeywordCorrectly() {

		flushTestUsers();

		assertThat(repository.findByCreatedAtAfter(secondUser.getCreatedAt())).containsOnly(thirdUser, fourthUser);
	}

	@Test // DATAJPA-188
	public void executesFinderWithBeforeKeywordCorrectly() {

		flushTestUsers();

		assertThat(repository.findByCreatedAtBefore(thirdUser.getCreatedAt())).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-180
	public void executesFinderWithStartingWithCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameStartingWith("Oli")).containsOnly(firstUser);
	}

	@Test // DATAJPA-180
	public void executesFinderWithEndingWithCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameEndingWith("er")).containsOnly(firstUser);
	}

	@Test // DATAJPA-180
	public void executesFinderWithContainingCorrectly() {

		flushTestUsers();

		assertThat(repository.findByFirstnameContaining("a")).containsOnly(secondUser, thirdUser);
	}

	@Test // DATAJPA-201
	public void allowsExecutingPageableMethodWithUnpagedArgument() {

		flushTestUsers();

		assertThat(repository.findByFirstname("Oliver", null)).containsOnly(firstUser);

		Page<User> page = repository.findByFirstnameIn(Pageable.unpaged(), "Oliver");
		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page.getContent()).contains(firstUser);

		page = repository.findAll(Pageable.unpaged());
		assertThat(page.getNumberOfElements()).isEqualTo(4);
		assertThat(page.getContent()).contains(firstUser, secondUser, thirdUser, fourthUser);
	}

	@Test // DATAJPA-207
	public void executesNativeQueryForNonEntitiesCorrectly() {

		flushTestUsers();

		List<Integer> result = repository.findOnesByNativeQuery();

		assertThat(result.size()).isEqualTo(4);
		assertThat(result).contains(1);
	}

	@Test // DATAJPA-232
	public void handlesIterableOfIdsCorrectly() {

		flushTestUsers();

		Set<Integer> set = new HashSet<>();
		set.add(firstUser.getId());
		set.add(secondUser.getId());

		assertThat(repository.findAllById(set)).containsOnly(firstUser, secondUser);
	}

	protected void flushTestUsers() {

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
	public void ordersByReferencedEntityCorrectly() {

		flushTestUsers();
		firstUser.setManager(thirdUser);
		repository.save(firstUser);

		Page<User> all = repository.findAll(PageRequest.of(0, 10, Sort.by("manager.id")));

		assertThat(all.getContent().isEmpty()).isFalse();
	}

	@Test // DATAJPA-252
	public void bindsSortingToOuterJoinCorrectly() {

		flushTestUsers();

		// Managers not set, make sure adding the sort does not rule out those Users
		Page<User> result = repository.findAllPaged(PageRequest.of(0, 10, Sort.by("manager.lastname")));
		assertThat(result.getContent()).hasSize((int) repository.count());
	}

	@Test // DATAJPA-277
	public void doesNotDropNullValuesOnPagedSpecificationExecution() {

		flushTestUsers();

		Page<User> page = repository.findAll(new Specification<User>() {
			@Override
			public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("lastname"), "Gierke");
			}
		}, PageRequest.of(0, 20, Sort.by("manager.lastname")));

		assertThat(page.getNumberOfElements()).isEqualTo(1);
		assertThat(page).containsOnly(firstUser);
	}

	@Test // DATAJPA-346
	public void shouldGenerateLeftOuterJoinInfindAllWithPaginationAndSortOnNestedPropertyPath() {

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
	public void executesManualQueryWithPositionLikeExpressionCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByFirstnameLike("Da");

		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-292
	public void executesManualQueryWithNamedLikeExpressionCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByFirstnameLikeNamed("Da");

		assertThat(result).containsOnly(thirdUser);
	}

	@Test // DATAJPA-231
	public void executesDerivedCountQueryToLong() {

		flushTestUsers();

		assertThat(repository.countByLastname("Matthews")).isEqualTo(1L);
	}

	@Test // DATAJPA-231
	public void executesDerivedCountQueryToInt() {

		flushTestUsers();

		assertThat(repository.countUsersByFirstname("Dave")).isEqualTo(1);
	}

	@Test // DATAJPA-231
	public void executesDerivedExistsQuery() {

		flushTestUsers();

		assertThat(repository.existsByLastname("Matthews")).isEqualTo(true);
		assertThat(repository.existsByLastname("Hans Peter")).isEqualTo(false);
	}

	@Test // DATAJPA-332, DATAJPA-1168
	public void findAllReturnsEmptyIterableIfNoIdsGiven() {

		assertThat(repository.findAllById(Collections.<Integer> emptySet())).isEmpty();
	}

	@Test // DATAJPA-391
	public void executesManuallyDefinedQueryWithFieldProjection() {

		flushTestUsers();
		List<String> lastname = repository.findFirstnamesByLastname("Matthews");

		assertThat(lastname).containsOnly("Dave");
	}

	@Test // DATAJPA-83
	public void looksUpEntityReference() {

		flushTestUsers();

		User result = repository.getOne(firstUser.getId());
		assertThat(result).isEqualTo(firstUser);
	}

	@Test // DATAJPA-415
	public void invokesQueryWithVarargsParametersCorrectly() {

		flushTestUsers();

		Collection<User> result = repository.findByIdIn(firstUser.getId(), secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-415
	public void shouldSupportModifyingQueryWithVarArgs() {

		flushTestUsers();

		repository.updateUserActiveState(false, firstUser.getId(), secondUser.getId(), thirdUser.getId(),
				fourthUser.getId());

		long expectedCount = repository.count();
		assertThat(repository.findByActiveFalse().size()).isEqualTo((int) expectedCount);
		assertThat(repository.findByActiveTrue().size()).isEqualTo(0);
	}

	@Test // DATAJPA-405
	public void executesFinderWithOrderClauseOnly() {

		flushTestUsers();

		assertThat(repository.findAllByOrderByLastnameAsc()).containsOnly(secondUser, firstUser, thirdUser, fourthUser);
	}

	@Test // DATAJPA-427
	public void sortByAssociationPropertyShouldUseLeftOuterJoin() {

		secondUser.getColleagues().add(firstUser);
		fourthUser.getColleagues().add(thirdUser);
		flushTestUsers();

		List<User> result = repository.findAll(Sort.by(Sort.Direction.ASC, "colleagues.id"));

		assertThat(result).hasSize(4);
	}

	@Test // DATAJPA-427
	public void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {

		secondUser.getColleagues().add(firstUser);
		fourthUser.getColleagues().add(thirdUser);
		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "colleagues.id")));

		assertThat(page.getContent()).hasSize(4);
	}

	@Test // DATAJPA-427
	public void sortByEmbeddedProperty() {

		thirdUser.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));
		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(thirdUser);
	}

	@Test // DATAJPA-454
	public void findsUserByBinaryDataReference() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		List<User> result = repository.findByBinaryData(data);
		assertThat(result).containsOnly(firstUser);
		assertThat(result.get(0).getBinaryData()).isEqualTo(data);
	}

	@Test // DATAJPA-461
	public void customFindByQueryWithPositionalVarargsParameters() {

		flushTestUsers();

		Collection<User> result = repository.findByIdsCustomWithPositionalVarArgs(firstUser.getId(), secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-461
	public void customFindByQueryWithNamedVarargsParameters() {

		flushTestUsers();

		Collection<User> result = repository.findByIdsCustomWithNamedVarArgs(firstUser.getId(), secondUser.getId());

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-464
	public void saveAndFlushShouldSupportReturningSubTypesOfRepositoryEntity() {

		repository.deleteAll();
		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		SpecialUser savedUser = repository.saveAndFlush(user);

		assertThat(user.getFirstname()).isEqualTo(savedUser.getFirstname());
		assertThat(user.getEmailAddress()).isEqualTo(savedUser.getEmailAddress());
	}

	@Test // DATAJPA-218
	public void findAllByUntypedExampleShouldReturnSubTypesOfRepositoryEntity() {

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
	public void findAllByTypedUserExampleShouldReturnSubTypesOfRepositoryEntity() {

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
	public void findAllByTypedSpecialUserExampleShouldReturnSubTypesOfRepositoryEntity() {

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
	public void sortByNestedAssociationPropertyWithSortInPageable() {

		firstUser.setManager(thirdUser);
		thirdUser.setManager(fourthUser);

		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, //
				Sort.by(Sort.Direction.ASC, "manager.manager.firstname")));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-510
	public void sortByNestedAssociationPropertyWithSortOrderIgnoreCaseInPageable() {

		firstUser.setManager(thirdUser);
		thirdUser.setManager(fourthUser);

		flushTestUsers();

		Page<User> page = repository.findAll(PageRequest.of(0, 10, //
				Sort.by(new Sort.Order(Direction.ASC, "manager.manager.firstname").ignoreCase())));

		assertThat(page.getContent()).hasSize(4);
		assertThat(page.getContent().get(3)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-496
	public void findByElementCollectionAttribute() {

		firstUser.getAttributes().add("cool");
		secondUser.getAttributes().add("hip");
		thirdUser.getAttributes().add("rockstar");

		flushTestUsers();

		List<User> result = repository.findByAttributesIn(new HashSet<>(Arrays.asList("cool", "hip")));

		assertThat(result).containsOnly(firstUser, secondUser);
	}

	@Test // DATAJPA-460
	public void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		flushTestUsers();

		List<User> result = repository.deleteByLastname(firstUser.getLastname());
		assertThat(result).containsOnly(firstUser);
	}

	@Test // DATAJPA-460
	public void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		flushTestUsers();

		repository.deleteByLastname(firstUser.getLastname());
		assertThat(repository.countByLastname(firstUser.getLastname())).isEqualTo(0L);
	}

	@Test // DATAJPA-460
	public void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		flushTestUsers();

		assertThat(repository.removeByLastname(firstUser.getLastname())).isEqualTo(1L);
	}

	@Test // DATAJPA-460
	public void deleteByShouldReturnZeroInCaseNoEntityHasBeenRemovedAndReturnTypeIsNumber() {

		flushTestUsers();

		assertThat(repository.removeByLastname("bubu")).isEqualTo(0L);
	}

	@Test // DATAJPA-460
	public void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		flushTestUsers();

		assertThat(repository.deleteByLastname("dorfuaeB")).isEmpty();
	}

	/**
	 * @see <a href="https://issues.apache.org/jira/browse/OPENJPA-2484">OPENJPA-2484</a>
	 */
	@Test // DATAJPA-505
	@Ignore
	public void findBinaryDataByIdJpaQl() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		byte[] result = repository.findBinaryDataByIdNative(firstUser.getId());

		assertThat(result.length).isEqualTo(data.length);
		assertThat(result).isEqualTo(data);
	}

	@Test // DATAJPA-506
	public void findBinaryDataByIdNative() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		byte[] result = repository.findBinaryDataByIdNative(firstUser.getId());

		assertThat(result).isEqualTo(data);
		assertThat(result.length).isEqualTo(data.length);
	}

	@Test // DATAJPA-456
	public void findPaginatedExplicitQueryWithCountQueryProjection() {

		firstUser.setFirstname(null);

		flushTestUsers();

		Page<User> result = repository.findAllByFirstnameLike("", PageRequest.of(0, 10));

		assertThat(result.getContent().size()).isEqualTo(3);
	}

	@Test // DATAJPA-456
	public void findPaginatedNamedQueryWithCountQueryProjection() {

		flushTestUsers();

		Page<User> result = repository.findByNamedQueryAndCountProjection("Gierke", PageRequest.of(0, 10));

		assertThat(result.getContent().size()).isEqualTo(1);
	}

	@Test // DATAJPA-551
	public void findOldestUser() {

		flushTestUsers();

		User oldest = thirdUser;

		assertThat(repository.findFirstByOrderByAgeDesc()).isEqualTo(oldest);
		assertThat(repository.findFirst1ByOrderByAgeDesc()).isEqualTo(oldest);
	}

	@Test // DATAJPA-551
	public void findYoungestUser() {

		flushTestUsers();

		User youngest = firstUser;

		assertThat(repository.findTopByOrderByAgeAsc()).isEqualTo(youngest);
		assertThat(repository.findTop1ByOrderByAgeAsc()).isEqualTo(youngest);
	}

	@Test // DATAJPA-551
	public void find2OldestUsers() {

		flushTestUsers();

		User oldest1 = thirdUser;
		User oldest2 = secondUser;

		assertThat(repository.findFirst2ByOrderByAgeDesc()).contains(oldest1, oldest2);
		assertThat(repository.findTop2ByOrderByAgeDesc()).contains(oldest1, oldest2);
	}

	@Test // DATAJPA-551
	public void find2YoungestUsers() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;

		assertThat(repository.findFirst2UsersBy(Sort.by(ASC, "age"))).contains(youngest1, youngest2);
		assertThat(repository.findTop2UsersBy(Sort.by(ASC, "age"))).contains(youngest1, youngest2);
	}

	@Test // DATAJPA-551
	public void find3YoungestUsersPageableWithPageSize2() {

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
	public void find2YoungestUsersPageableWithPageSize3() {

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
	public void find3YoungestUsersPageableWithPageSize2Sliced() {

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
	public void find2YoungestUsersPageableWithPageSize3Sliced() {

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
	public void pageableQueryReportsTotalFromResult() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(PageRequest.of(0, 10));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = repository.findAll(PageRequest.of(1, 3));
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test // DATAJPA-912
	public void pageableQueryReportsTotalFromCount() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(PageRequest.of(0, 4));
		assertThat(firstPage.getContent()).hasSize(4);
		assertThat(firstPage.getTotalElements()).isEqualTo(4L);

		Page<User> secondPage = repository.findAll(PageRequest.of(10, 10));
		assertThat(secondPage.getContent()).hasSize(0);
		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
	}

	@Test // DATAJPA-506
	public void invokesQueryWithWrapperType() {

		flushTestUsers();

		Optional<User> result = repository.findOptionalByEmailAddress("gierke@synyx.de");

		assertThat(result.isPresent()).isEqualTo(true);
		assertThat(result.get()).isEqualTo(firstUser);
	}

	@Test // DATAJPA-564
	public void shouldFindUserByFirstnameAndLastnameWithSpelExpressionInStringBasedQuery() {

		flushTestUsers();
		List<User> users = repository.findByFirstnameAndLastnameWithSpelExpression("Oliver", "ierk");

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	public void shouldFindUserByLastnameWithSpelExpressionInStringBasedQuery() {

		flushTestUsers();
		List<User> users = repository.findByLastnameWithSpelExpression("ierk");

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	public void shouldFindBySpELExpressionWithoutArgumentsWithQuestionmark() {

		flushTestUsers();
		List<User> users = repository.findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	public void shouldFindBySpELExpressionWithoutArgumentsWithColon() {

		flushTestUsers();
		List<User> users = repository.findOliverBySpELExpressionWithoutArgumentsWithColon();

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-564
	public void shouldFindUsersByAgeForSpELExpression() {

		flushTestUsers();
		List<User> users = repository.findUsersByAgeForSpELExpressionByIndexedParameter(35);

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	public void shouldfindUsersByFirstnameForSpELExpressionWithParameterNameVariableReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpression("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	public void shouldFindCurrentUserWithCustomQueryDependingOnSecurityContext() {

		flushTestUsers();

		SampleSecurityContextHolder.getCurrent().setPrincipal(secondUser);
		List<User> users = repository.findCurrentUserWithCustomQuery();

		assertThat(users).containsOnly(secondUser);

		SampleSecurityContextHolder.getCurrent().setPrincipal(firstUser);
		users = repository.findCurrentUserWithCustomQuery();

		assertThat(users).contains(firstUser);
	}

	@Test // DATAJPA-564
	public void shouldFindByFirstnameAndCurrentUserWithCustomQuery() {

		flushTestUsers();

		SampleSecurityContextHolder.getCurrent().setPrincipal(secondUser);
		List<User> users = repository.findByFirstnameAndCurrentUserWithCustomQuery("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	public void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterNameVariableReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpressionWithParameterVariableOnly("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	public void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterIndexReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpressionWithParameterIndexOnly("Joachim");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-564
	public void shouldFindUsersInNativeQueryWithPagination() {

		flushTestUsers();

		Page<User> users = repository.findUsersInNativeQueryWithPagination(PageRequest.of(0, 2));

		assertThat(users.getContent()).hasSize(2).containsExactly(firstUser, secondUser);

		users = repository.findUsersInNativeQueryWithPagination(PageRequest.of(1, 2));

		assertThat(users.getContent()).hasSize(2).containsExactly(thirdUser, fourthUser);
	}

	@Test // DATAJPA-1140
	public void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsStringInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsString(firstUser,
				firstUser.getLastname());

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	public void shouldFindUsersByFirstnameAsStringAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository.findUsersByFirstnameAsStringAndUserLastnameAsSpELExpression(firstUser.getFirstname(),
				firstUser);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	public void shouldFindUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpressionInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository.findUsersByUserFirstnameAsSpELExpressionAndLastnameAsFakeSpELExpression(firstUser,
				firstUser.getLastname());

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	public void shouldFindUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpressionInStringBasedQuery() {

		flushTestUsers();

		List<User> users = repository
				.findUsersByFirstnameAsFakeSpELExpressionAndUserLastnameAsSpELExpression(firstUser.getFirstname(), firstUser);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-1140
	public void shouldFindUsersByFirstnameWithLeadingPageableParameter() {

		flushTestUsers();

		List<User> users = repository.findUsersByFirstnamePaginated(PageRequest.of(0, 2), firstUser.getFirstname());

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-629
	public void shouldfindUsersBySpELExpressionParametersWithSpelTemplateExpression() {

		flushTestUsers();
		List<User> users = repository
				.findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression("Joachim", "Arrasz");

		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-606
	public void findByEmptyCollectionOfStrings() throws Exception {

		flushTestUsers();

		List<User> users = repository.findByAttributesIn(new HashSet<>());
		assertThat(users).hasSize(0);
	}

	@Test // DATAJPA-606
	public void findByEmptyCollectionOfIntegers() throws Exception {

		flushTestUsers();

		List<User> users = repository.findByAgeIn(Collections.emptyList());
		assertThat(users).hasSize(0);
	}

	@Test // DATAJPA-606
	public void findByEmptyArrayOfIntegers() throws Exception {

		flushTestUsers();

		List<User> users = repository.queryByAgeIn(new Integer[0]);
		assertThat(users).hasSize(0);
	}

	@Test // DATAJPA-606
	public void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		flushTestUsers();

		List<User> users = repository.queryByAgeInOrFirstname(new Integer[0], secondUser.getFirstname());
		assertThat(users).containsOnly(secondUser);
	}

	@Test // DATAJPA-677
	public void shouldSupportJava8StreamsForRepositoryFinderMethods() {

		flushTestUsers();

		try (Stream<User> stream = repository.findAllByCustomQueryAndStream()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test // DATAJPA-677
	public void shouldSupportJava8StreamsForRepositoryDerivedFinderMethods() {

		flushTestUsers();

		try (Stream<User> stream = repository.readAllByFirstnameNotNull()) {
			assertThat(stream).hasSize(4);
		}
	}

	@Test // DATAJPA-677
	public void supportsJava8StreamForPageableMethod() {

		flushTestUsers();

		try (Stream<User> stream = repository.streamAllPaged(PageRequest.of(0, 2));) {
			assertThat(stream).hasSize(2);
		}
	}

	@Test // DATAJPA-218
	public void findAllByExample() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);
		prototype.setCreatedAt(null);

		List<User> users = repository.findAll(of(prototype));

		assertThat(users).hasSize(1);
		assertThat(users.get(0)).isEqualTo(firstUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithEmptyProbe() {

		flushTestUsers();

		User prototype = new User();
		prototype.setCreatedAt(null);

		List<User> users = repository
				.findAll(of(prototype, ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "active")));

		assertThat(users).hasSize(4);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-218
	public void findAllByNullExample() {
		repository.findAll((Example<User>) null);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithAssociation() {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.saveAll(Arrays.asList(firstUser, thirdUser));

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
	public void findAllByExampleWithEmbedded() {

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
	public void findAllByExampleWithStartingStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("Ol");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.STARTING).withIgnorePaths("age", "createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithEndingStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("ver");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.ENDING).withIgnorePaths("age", "createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-218
	public void findAllByExampleWithRegexStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("^Oliver$");

		Example<User> example = Example.of(prototype, matching().withStringMatcher(StringMatcher.REGEX));
		repository.findAll(example);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithIgnoreCase() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiVer");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithStringMatcherAndIgnoreCase() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiV");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.STARTING).withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithIncludeNull() {

		flushTestUsers();

		firstUser.setAddress(new Address("andor", "caemlyn", "", ""));

		User fifthUser = new User();
		fifthUser.setEmailAddress("foo@bar.com");
		fifthUser.setActive(firstUser.isActive());
		fifthUser.setAge(firstUser.getAge());
		fifthUser.setFirstname(firstUser.getFirstname());
		fifthUser.setLastname(firstUser.getLastname());

		repository.saveAll(Arrays.asList(firstUser, fifthUser));

		User prototype = new User();
		prototype.setFirstname(firstUser.getFirstname());

		Example<User> example = Example.of(prototype, matching().withIncludeNullValues().withIgnorePaths("id", "binaryData",
				"lastname", "emailAddress", "age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(fifthUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithPropertySpecifier() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withMatcher("firstname", new GenericPropertyMatcher().startsWith()));

		List<User> users = repository.findAll(example);

		assertThat(users).containsOnly(firstUser);
	}

	@Test // DATAJPA-218
	public void findAllByExampleWithSort() {

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
	public void findAllByExampleWithPageable() {

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
		assertThat(users.hasNext()).isEqualTo(true);
		assertThat(users.getTotalElements()).isEqualTo(100L);
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-218
	public void findAllByExampleShouldNotAllowCycles() {

		flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		user1.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age")));
	}

	@Test(expected = InvalidDataAccessApiUsageException.class) // DATAJPA-218
	public void findAllByExampleShouldNotAllowCyclesOverSeveralInstances() {

		flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		User user2 = new User();
		user2.setFirstname("user2");

		user1.setManager(user2);
		user2.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		repository.findAll(example, PageRequest.of(0, 10, Sort.by(DESC, "age")));
	}

	@Test // DATAJPA-218
	public void findOneByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));

		assertThat(repository.findOne(example)).contains(firstUser);
	}

	@Test // DATAJPA-218
	public void countByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		long count = repository.count(example);

		assertThat(count).isEqualTo(1L);
	}

	@Test // DATAJPA-218
	public void existsByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		boolean exists = repository.exists(example);

		assertThat(exists).isEqualTo(true);
	}

	@Test // DATAJPA-905
	public void executesPagedSpecificationSettingAnOrder() {

		flushTestUsers();

		Page<User> result = repository.findAll(userHasLastnameLikeWithSort("e"), PageRequest.of(0, 1));

		assertThat(result.getTotalElements()).isEqualTo(2L);
		assertThat(result.getNumberOfElements()).isEqualTo(1);
		assertThat(result.getContent().get(0)).isEqualTo(thirdUser);
	}

	@Test // DATAJPA-1172
	public void exceptionsDuringParameterSettingGetThrown() {

		assertThatExceptionOfType(InvalidDataAccessApiUsageException.class) //
				.isThrownBy(() -> repository.findByStringAge("twelve")) //
				.matches(e -> !e.getMessage().contains("Named parameter [age] not set"));
	}

	@Test // DATAJPA-1172
	public void queryProvidesCorrectNumberOfParametersForNativeQuery() {

		Query query = em.createNativeQuery("select 1 from User where firstname=? and lastname=?");
		assertThat(query.getParameters()).hasSize(2);
	}

	@Test // DATAJPA-1185
	public void dynamicProjectionReturningStream() {

		flushTestUsers();

		assertThat(repository.findAsStreamByFirstnameLike("%O%", User.class)).hasSize(1);
	}

	@Test // DATAJPA-1185
	public void dynamicProjectionReturningList() {

		flushTestUsers();

		List<User> users = repository.findAsListByFirstnameLike("%O%", User.class);

		assertThat(users).hasSize(1);
	}

	@Test // DATAJPA-1179
	public void duplicateSpelsWorkAsIntended() {

		flushTestUsers();

		List<User> users = repository.findUsersByDuplicateSpel("Oliver");

		assertThat(users).hasSize(1);
	}

	@Test // DATAJPA-980
	public void supportsProjectionsWithNativeQueries() {

		flushTestUsers();

		User user = repository.findAll().get(0);

		NameOnly result = repository.findByNativeQuery(user.getId());

		assertThat(result.getFirstname()).isEqualTo(user.getFirstname());
		assertThat(result.getLastname()).isEqualTo(user.getLastname());
	}

	@Test // DATAJPA-1248
	public void supportsProjectionsWithNativeQueriesAndCamelCaseProperty() {

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
	public void handlesColonsFollowedByIntegerInStringLiteral() {

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
	public void handlesCountQueriesWithLessParametersSingleParam() {
		repository.findAllOrderedBySpecialNameSingleParam("Oliver", PageRequest.of(2, 3));
	}

	@Test // DATAJPA-1233
	public void handlesCountQueriesWithLessParametersMoreThanOne() {
		repository.findAllOrderedBySpecialNameMultipleParams("Oliver", "x", PageRequest.of(2, 3));
	}

	@Test // DATAJPA-1233
	public void handlesCountQueriesWithLessParametersMoreThanOneIndexed() {
		repository.findAllOrderedBySpecialNameMultipleParamsIndexed("Oliver", "x", PageRequest.of(2, 3));
	}

	// DATAJPA-928
	@Test
	public void executeNativeQueryWithPage() {

		flushTestUsers();

		Page<User> firstPage = repository.findByNativeNamedQueryWithPageable(new PageRequest(0, 3));
		Page<User> secondPage = repository.findByNativeNamedQueryWithPageable(new PageRequest(1, 3));

		SoftAssertions softly = new SoftAssertions();

		assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		assertThat(firstPage.getContent()) //
				.extracting(User::getFirstname) //
				.containsExactly("Dave", "Joachim", "kevin");

		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		assertThat(secondPage.getNumberOfElements()).isEqualTo(1);
		assertThat(secondPage.getContent()) //
				.extracting(User::getFirstname) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	// DATAJPA-928
	@Test
	public void executeNativeQueryWithPageWorkaround() {

		flushTestUsers();

		Page<String> firstPage = repository.findByNativeQueryWithPageable(new PageRequest(0, 3));
		Page<String> secondPage = repository.findByNativeQueryWithPageable(new PageRequest(1, 3));

		SoftAssertions softly = new SoftAssertions();

		assertThat(firstPage.getTotalElements()).isEqualTo(4L);
		assertThat(firstPage.getNumberOfElements()).isEqualTo(3);
		assertThat(firstPage.getContent()) //
				.containsExactly("Dave", "Joachim", "kevin");

		assertThat(secondPage.getTotalElements()).isEqualTo(4L);
		assertThat(secondPage.getNumberOfElements()).isEqualTo(1);
		assertThat(secondPage.getContent()) //
				.containsExactly("Oliver");

		softly.assertAll();
	}

	@Test // DATAJPA-1273
	public void bindsNativeQueryResultsToProjectionByName() {

		flushTestUsers();

		List<NameOnly> result = repository.findByNamedQueryWithAliasInInvertedOrder();

		assertThat(result).element(0).satisfies(it -> {
			assertThat(it.getFirstname()).isEqualTo("Joachim");
			assertThat(it.getLastname()).isEqualTo("Arrasz");
		});
	}

	private Page<User> executeSpecWithSort(Sort sort) {

		flushTestUsers();

		Specification<User> spec = userHasFirstname("Oliver").or(userHasLastname("Matthews"));

		Page<User> result = repository.findAll(spec, PageRequest.of(0, 1, sort));
		assertThat(result.getTotalElements()).isEqualTo(2L);
		return result;
	}
}
