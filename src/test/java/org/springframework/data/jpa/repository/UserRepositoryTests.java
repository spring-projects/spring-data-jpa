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
package org.springframework.data.jpa.repository;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.jpa.domain.Specifications.*;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Base integration test class for {@code UserRepository}. Loads a basic (non-namespace) Spring configuration file as
 * well as Hibernate configuration to execute tests.
 * <p>
 * To test further persistence providers subclass this class and provide a custom provider configuration.
 * 
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
public class UserRepositoryTests {

	@PersistenceContext
	EntityManager em;

	// CUT
	@Autowired
	UserRepository repository;

	// Test fixture
	User firstUser, secondUser, thirdUser;
	Integer id;

	@Before
	public void setUp() throws Exception {

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);
		Thread.sleep(10);
		thirdUser = new User("Dave", "Matthews", "no@email.com");
		thirdUser.setAge(43);
	}

	@Test
	public void testCreation() {

		Query countQuery = em.createQuery("select count(u) from User u");
		Long before = (Long) countQuery.getSingleResult();

		flushTestUsers();

		assertThat((Long) countQuery.getSingleResult(), is(before + 3));
	}

	@Test
	public void testRead() throws Exception {

		flushTestUsers();

		User foundPerson = repository.findOne(id);
		assertThat(firstUser.getFirstname(), is(foundPerson.getFirstname()));
	}

	@Test
	public void findsAllByGivenIds() {

		flushTestUsers();

		Iterable<User> result = repository.findAll(Arrays.asList(firstUser.getId(), secondUser.getId()));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	@Test
	public void testReadByIdReturnsNullForNotFoundEntities() {

		flushTestUsers();

		assertThat(repository.findOne(id * 27), is(nullValue()));
	}

	@Test
	public void savesCollectionCorrectly() throws Exception {

		List<User> result = repository.save(Arrays.asList(firstUser, secondUser, thirdUser));
		assertThat(result, is(notNullValue()));
		assertThat(result.size(), is(3));
		assertThat(result, hasItems(firstUser, secondUser, thirdUser));
	}

	@Test
	public void savingNullCollectionIsNoOp() throws Exception {

		List<User> result = repository.save((Collection<User>) null);
		assertThat(result, is(notNullValue()));
		assertThat(result.isEmpty(), is(true));
	}

	@Test
	public void savingEmptyCollectionIsNoOp() throws Exception {

		List<User> result = repository.save(new ArrayList<User>());
		assertThat(result, is(notNullValue()));
		assertThat(result.isEmpty(), is(true));
	}

	@Test
	public void testUpdate() {

		flushTestUsers();

		User foundPerson = repository.findOne(id);
		foundPerson.setLastname("Schlicht");

		User updatedPerson = repository.findOne(id);
		assertThat(updatedPerson.getFirstname(), is(foundPerson.getFirstname()));
	}

	@Test
	public void existReturnsWhetherAnEntityCanBeLoaded() throws Exception {

		flushTestUsers();
		assertThat(repository.exists(id), is(true));
		assertThat(repository.exists(id * 27), is(false));
	}

	@Test
	public void deletesAUserById() {

		flushTestUsers();

		repository.delete(firstUser.getId());
		assertThat(repository.exists(id), is(false));
		assertThat(repository.findOne(id), is(nullValue()));
	}

	@Test
	public void testDelete() {

		flushTestUsers();

		repository.delete(firstUser);
		assertThat(repository.exists(id), is(false));
		assertThat(repository.findOne(id), is(nullValue()));
	}

	@Test
	public void returnsAllSortedCorrectly() throws Exception {

		flushTestUsers();
		List<User> result = repository.findAll(new Sort(ASC, "lastname"));
		assertThat(result, is(notNullValue()));
		assertThat(result.size(), is(3));
		assertThat(result.get(0), is(secondUser));
		assertThat(result.get(1), is(firstUser));
		assertThat(result.get(2), is(thirdUser));
	}

	@Test
	public void deleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.delete(Arrays.asList(firstUser, secondUser));
		assertThat(repository.exists(firstUser.getId()), is(false));
		assertThat(repository.exists(secondUser.getId()), is(false));
		assertThat(repository.count(), is(before - 2));
	}

	@Test
	public void batchDeleteColletionOfEntities() {

		flushTestUsers();

		long before = repository.count();

		repository.deleteInBatch(Arrays.asList(firstUser, secondUser));
		assertThat(repository.exists(firstUser.getId()), is(false));
		assertThat(repository.exists(secondUser.getId()), is(false));
		assertThat(repository.count(), is(before - 2));
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
		assertThat(repository.findByLastname("newLastname").size(), is(Long.valueOf(expected).intValue()));
	}

	@Test
	public void testFinderInvocationWithNullParameter() {

		flushTestUsers();

		repository.findByLastname(null);
	}

	@Test
	public void testFindByLastname() throws Exception {

		flushTestUsers();

		List<User> byName = repository.findByLastname("Gierke");

		assertThat(byName.size(), is(1));
		assertThat(byName.get(0), is(firstUser));
	}

	/**
	 * Tests, that searching by the email address of the reference user returns exactly that instance.
	 * 
	 * @throws Exception
	 */
	@Test
	public void testFindByEmailAddress() throws Exception {

		flushTestUsers();

		User byName = repository.findByEmailAddress("gierke@synyx.de");

		assertThat(byName, is(notNullValue()));
		assertThat(byName, is(firstUser));
	}

	/**
	 * Tests reading all users.
	 */
	@Test
	public void testReadAll() {

		flushTestUsers();

		assertThat(repository.count(), is(3L));
		assertThat(repository.findAll(), hasItems(firstUser, secondUser, thirdUser));
	}

	/**
	 * Tests that all users get deleted by triggering {@link UserRepository#deleteAll()}.
	 * 
	 * @throws Exception
	 */
	@Test
	public void deleteAll() throws Exception {

		flushTestUsers();

		repository.deleteAll();

		assertThat(repository.count(), is(0L));
	}

	/**
	 * @see DATAJPA-137
	 */
	@Test
	public void deleteAllInBatch() {

		flushTestUsers();

		repository.deleteAllInBatch();

		assertThat(repository.count(), is(0L));
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
		User firstReferenceUser = repository.findOne(firstUser.getId());
		assertThat(firstReferenceUser, is(firstUser));

		// Fetch colleagues and assert link
		Set<User> colleagues = firstReferenceUser.getColleagues();
		assertThat(colleagues.size(), is(1));
		assertThat(colleagues.contains(secondUser), is(true));
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

		User reference = repository.findOne(firstUser.getId());
		Set<User> colleagues = reference.getColleagues();

		assertThat(colleagues, is(notNullValue()));
		assertThat(colleagues.size(), is(2));
	}

	@Test
	public void testCountsCorrectly() {

		long count = repository.count();

		User user = new User();
		user.setEmailAddress("gierke@synyx.de");
		repository.save(user);

		assertThat(repository.count() == count + 1, is(true));
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

		assertThat(repository.findByAnnotatedQuery("gierke@synyx.de"), is(nullValue()));
	}

	@Test
	public void testExecutionOfProjectingMethod() {

		flushTestUsers();
		assertThat(repository.countWithFirstname("Oliver").longValue(), is(1L));
	}

	@Test
	public void executesSpecificationCorrectly() {

		flushTestUsers();
		assertThat(repository.findAll(where(userHasFirstname("Oliver"))).size(), is(1));
	}

	@Test
	public void executesSingleEntitySpecificationCorrectly() throws Exception {

		flushTestUsers();
		assertThat(repository.findOne(userHasFirstname("Oliver")), is(firstUser));
	}

	@Test
	public void returnsNullIfNoEntityFoundForSingleEntitySpecification() throws Exception {

		flushTestUsers();
		assertThat(repository.findOne(userHasLastname("Beauford")), is(nullValue()));
	}

	@Test(expected = IncorrectResultSizeDataAccessException.class)
	public void throwsExceptionForUnderSpecifiedSingleEntitySpecification() {

		flushTestUsers();
		repository.findOne(userHasFirstnameLike("e"));
	}

	@Test
	public void executesCombinedSpecificationsCorrectly() {

		flushTestUsers();
		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Arrasz"));
		assertThat(repository.findAll(spec), hasSize(2));
	}

	/**
	 * @see DATAJPA-253
	 */
	@Test
	public void executesNegatingSpecificationCorrectly() {

		flushTestUsers();
		Specification<User> spec = not(userHasFirstname("Oliver")).and(userHasLastname("Arrasz"));
		List<User> result = repository.findAll(spec);

		assertThat(result, hasSize(1));
		assertThat(result, hasItem(secondUser));
	}

	@Test
	public void executesCombinedSpecificationsWithPageableCorrectly() {

		flushTestUsers();
		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Arrasz"));

		Page<User> users = repository.findAll(spec, new PageRequest(0, 1));
		assertThat(users.getSize(), is(1));
		assertThat(users.hasPreviousPage(), is(false));
		assertThat(users.getTotalElements(), is(2L));
	}

	@Test
	public void executesMethodWithAnnotatedNamedParametersCorrectly() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertTrue(repository.findByLastnameOrFirstname("Oliver", "Arrasz").containsAll(
				Arrays.asList(firstUser, secondUser)));
	}

	@Test
	public void executesMethodWithNamedParametersCorrectlyOnMethodsWithQueryCreation() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		List<User> result = repository.findByFirstnameOrLastname("Oliver", "Arrasz");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	@Test
	public void executesLikeAndOrderByCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameLikeOrderByFirstnameDesc("%r%");
		assertThat(result.size(), is(2));
		assertEquals(firstUser, result.get(0));
		assertEquals(secondUser, result.get(1));
	}

	@Test
	public void executesNotLikeCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameNotLike("%er%");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	@Test
	public void executesSimpleNotCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameNot("Gierke");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	@Test
	public void returnsSameListIfNoSpecGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll(), repository.findAll((Specification<User>) null));
	}

	@Test
	public void returnsSameListIfNoSortIsGiven() throws Exception {

		flushTestUsers();
		assertSameElements(repository.findAll((Sort) null), repository.findAll());
	}

	@Test
	public void returnsSamePageIfNoSpecGiven() throws Exception {

		Pageable pageable = new PageRequest(0, 1);

		flushTestUsers();
		assertThat(repository.findAll(null, pageable), is(repository.findAll(pageable)));
	}

	@Test
	public void returnsAllAsPageIfNoPageableIsGiven() throws Exception {

		flushTestUsers();
		assertThat(repository.findAll((Pageable) null), is((Page<User>) new PageImpl<User>(repository.findAll())));
	}

	@Test
	public void removeDetachedObject() throws Exception {

		flushTestUsers();

		em.detach(firstUser);
		repository.delete(firstUser);

		assertThat(repository.count(), is(2L));
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executesPagedSpecificationsCorrectly() throws Exception {

		Page<User> result = executeSpecWithSort(null);
		assertThat(result.getContent(), anyOf(hasItem(firstUser), hasItem(thirdUser)));
		assertThat(result.getContent(), not(hasItem(secondUser)));
	}

	@Test
	public void executesPagedSpecificationsWithSortCorrectly() throws Exception {

		Page<User> result = executeSpecWithSort(new Sort(Direction.ASC, "lastname"));

		assertThat(result.getContent(), hasItem(firstUser));
		assertThat(result.getContent(), not(hasItem(secondUser)));
		assertThat(result.getContent(), not(hasItem(thirdUser)));
	}

	@Test
	public void executesPagedSpecificationWithSortCorrectly2() throws Exception {

		Page<User> result = executeSpecWithSort(new Sort(Direction.DESC, "lastname"));

		assertThat(result.getContent(), hasItem(thirdUser));
		assertThat(result.getContent(), not(hasItem(secondUser)));
		assertThat(result.getContent(), not(hasItem(firstUser)));
	}

	@Test
	public void executesQueryMethodWithDeepTraversalCorrectly() throws Exception {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.save(Arrays.asList(firstUser, thirdUser));

		List<User> result = repository.findByManagerLastname("Arrasz");

		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));

		result = repository.findByManagerLastname("Gierke");
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(thirdUser));
	}

	@Test
	public void executesFindByColleaguesLastnameCorrectly() throws Exception {

		flushTestUsers();

		firstUser.addColleague(secondUser);
		thirdUser.addColleague(firstUser);
		repository.save(Arrays.asList(firstUser, thirdUser));

		List<User> result = repository.findByColleaguesLastname(secondUser.getLastname());

		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));

		result = repository.findByColleaguesLastname("Gierke");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(thirdUser, secondUser));
	}

	@Test
	public void executesFindByNotNullLastnameCorrectly() throws Exception {

		flushTestUsers();
		List<User> result = repository.findByLastnameNotNull();

		assertThat(result.size(), is(3));
		assertThat(result, hasItems(firstUser, secondUser, thirdUser));
	}

	@Test
	public void executesFindByNullLastnameCorrectly() throws Exception {

		flushTestUsers();
		User forthUser = repository.save(new User("Foo", null, "email@address.com"));

		List<User> result = repository.findByLastnameNull();

		assertThat(result.size(), is(1));
		assertThat(result, hasItems(forthUser));
	}

	@Test
	public void findsSortedByLastname() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByEmailAddressLike("%@%", new Sort(Direction.ASC, "lastname"));

		assertThat(result.size(), is(3));
		assertThat(result.get(0), is(secondUser));
		assertThat(result.get(1), is(firstUser));
		assertThat(result.get(2), is(thirdUser));
	}

	@Test
	public void findsUsersBySpringDataNamedQuery() {

		flushTestUsers();

		List<User> result = repository.findBySpringDataNamedQuery("Gierke");
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));
	}

	/**
	 * @see DATADOC-86
	 */
	@Test
	public void readsPageWithGroupByClauseCorrectly() {

		flushTestUsers();

		Page<String> result = repository.findByLastnameGrouped(new PageRequest(0, 10));
		assertThat(result.getTotalPages(), is(1));
	}

	@Test
	public void executesLessThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByAgeLessThanEqual(35);
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	@Test
	public void executesGreaterThatOrEqualQueriesCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByAgeGreaterThanEqual(35);
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	/**
	 * @see DATAJPA-117
	 */
	@Test
	public void executesNativeQueryCorrectly() {

		flushTestUsers();

		List<User> result = repository.findNativeByLastname("Matthews");

		assertThat(result, hasItem(thirdUser));
		assertThat(result.size(), is(1));
	}

	/**
	 * @see DATAJPA-132
	 */
	@Test
	public void executesFinderWithTrueKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		List<User> result = repository.findByActiveTrue();
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	/**
	 * @see DATAJPA-132
	 */
	@Test
	public void executesFinderWithFalseKeywordCorrectly() {

		flushTestUsers();
		firstUser.setActive(false);
		repository.save(firstUser);

		List<User> result = repository.findByActiveFalse();
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));
	}

	/**
	 * Ignored until the query declaration is supported by OpenJPA.
	 */
	@Test
	@Ignore
	public void executesAnnotatedCollectionMethodCorrectly() {

		flushTestUsers();
		firstUser.addColleague(thirdUser);
		repository.save(firstUser);

		List<User> result = null; // repository.findColleaguesFor(firstUser);
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(thirdUser));
	}

	/**
	 * @see DATAJPA-188
	 */
	@Test
	public void executesFinderWithAfterKeywordCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByCreatedAtAfter(secondUser.getCreatedAt());
		assertThat(result.size(), is(1));
		assertThat(result, hasItems(thirdUser));
	}

	/**
	 * @see DATAJPA-188
	 */
	@Test
	public void executesFinderWithBeforeKeywordCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByCreatedAtBefore(thirdUser.getCreatedAt());
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	/**
	 * @see DATAJPA-180
	 */
	@Test
	public void executesFinderWithStartingWithCorrectly() {

		flushTestUsers();
		List<User> result = repository.findByFirstnameStartingWith("Oli");
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));
	}

	/**
	 * @see DATAJPA-180
	 */
	@Test
	public void executesFinderWithEndingWithCorrectly() {

		flushTestUsers();
		List<User> result = repository.findByFirstnameEndingWith("er");
		assertThat(result.size(), is(1));
		assertThat(result, hasItem(firstUser));
	}

	/**
	 * @see DATAJPA-180
	 */
	@Test
	public void executesFinderWithContainingCorrectly() {

		flushTestUsers();
		List<User> result = repository.findByFirstnameContaining("a");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(secondUser, thirdUser));
	}

	/**
	 * @see DATAJPA-201
	 */
	@Test
	public void allowsExecutingPageableMethodWithNullPageable() {

		flushTestUsers();

		List<User> users = repository.findByFirstname("Oliver", null);
		assertThat(users.size(), is(1));
		assertThat(users, hasItem(firstUser));

		Page<User> page = repository.findByFirstnameIn(null, "Oliver");
		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page.getContent(), hasItem(firstUser));

		page = repository.findAll((Pageable) null);
		assertThat(page.getNumberOfElements(), is(3));
		assertThat(page.getContent(), hasItems(firstUser, secondUser, thirdUser));
	}

	/**
	 * @see DATAJPA-207
	 */
	@Test
	public void executesNativeQueryForNonEntitiesCorrectly() {

		flushTestUsers();

		List<Integer> result = repository.findOnesByNativeQuery();

		assertThat(result.size(), is(3));
		assertThat(result, hasItem(1));
	}

	/**
	 * @see DATAJPA-232
	 */
	@Test
	public void handlesIterableOfIdsCorrectly() {

		flushTestUsers();

		Set<Integer> set = new HashSet<Integer>();
		set.add(firstUser.getId());
		set.add(secondUser.getId());

		Iterable<User> result = repository.findAll(set);

		assertThat(result, is(Matchers.<User> iterableWithSize(2)));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	protected void flushTestUsers() {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);

		repository.flush();

		id = firstUser.getId();

		assertThat(id, is(notNullValue()));
		assertThat(secondUser.getId(), is(notNullValue()));
		assertThat(thirdUser.getId(), is(notNullValue()));

		assertThat(repository.exists(id), is(true));
		assertThat(repository.exists(secondUser.getId()), is(true));
		assertThat(repository.exists(thirdUser.getId()), is(true));
	}

	private static <T> void assertSameElements(Collection<T> first, Collection<T> second) {

		for (T element : first) {
			assertThat(element, isIn(second));
		}

		for (T element : second) {
			assertThat(element, isIn(first));
		}
	}

	private void assertDeleteCallDoesNotDeleteAnything(List<User> collection) {

		flushTestUsers();
		long count = repository.count();

		repository.delete(collection);
		assertThat(repository.count(), is(count));
	}

	@Test
	public void ordersByReferencedEntityCorrectly() {

		flushTestUsers();
		firstUser.setManager(thirdUser);
		repository.save(firstUser);

		Page<User> all = repository.findAll(new PageRequest(0, 10, new Sort("manager.id")));

		assertThat(all.getContent().isEmpty(), is(false));
	}

	/**
	 * @see DATAJPA-252
	 */
	@Test
	public void bindsSortingToOuterJoinCorrectly() {

		flushTestUsers();

		// Managers not set, make sure adding the sort does not rule out those Users
		Page<User> result = repository.findAllPaged(new PageRequest(0, 10, new Sort("manager.lastname")));
		assertThat(result.getContent(), hasSize((int) repository.count()));
	}

	/**
	 * @see DATAJPA-277
	 */
	@Test
	public void doesNotDropNullValuesOnPagedSpecificationExecution() {

		flushTestUsers();

		Page<User> page = repository.findAll(new Specification<User>() {
			public Predicate toPredicate(Root<User> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				return cb.equal(root.get("lastname"), "Gierke");
			}
		}, new PageRequest(0, 20, new Sort("manager.lastname")));

		assertThat(page.getNumberOfElements(), is(1));
		assertThat(page, hasItem(firstUser));
	}

	/**
	 * @see DATAJPA-292
	 */
	@Test
	public void executesManualQueryWithPositionLikeExpressionCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByFirstnameLike("Da");

		assertThat(result, hasSize(1));
		assertThat(result, hasItem(thirdUser));
	}

	/**
	 * @see DATAJPA-292
	 */
	@Test
	public void executesManualQueryWithNamedLikeExpressionCorrectly() {

		flushTestUsers();

		List<User> result = repository.findByFirstnameLikeNamed("Da");

		assertThat(result, hasSize(1));
		assertThat(result, hasItem(thirdUser));
	}

	/**
	 * @see DATAJPA-332
	 */
	@Test
	public void findAllReturnsEmptyIterableIfNoIdsGiven() {

		assertThat(repository.findAll(Collections.<Integer> emptySet()), is(emptyIterable()));
		assertThat(repository.findAll((Iterable<Integer>) null), is(emptyIterable()));
	}

	private Page<User> executeSpecWithSort(Sort sort) {

		flushTestUsers();

		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Matthews"));

		Page<User> result = repository.findAll(spec, new PageRequest(0, 1, sort));
		assertThat(result.getTotalElements(), is(2L));
		return result;
	}
}
