/*
 * Copyright 2008-2016 the original author or authors.
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
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Example.*;
import static org.springframework.data.domain.ExampleMatcher.*;
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.jpa.domain.Specifications.not;
import static org.springframework.data.jpa.domain.Specifications.*;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hamcrest.Matchers;
import org.junit.Assume;
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
import org.springframework.data.domain.ExampleMatcher.*;
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
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension.SampleSecurityContextHolder;
import org.springframework.data.jpa.repository.sample.UserRepository;
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
 * @author Mark Paluch
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

		assertThat((Long) countQuery.getSingleResult(), is(before + 4));
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
		assertThat(result.size(), is(4));
		assertThat(result.get(0), is(secondUser));
		assertThat(result.get(1), is(firstUser));
		assertThat(result.get(2), is(thirdUser));
		assertThat(result.get(3), is(fourthUser));
	}

	/**
	 * @see DATAJPA-296
	 * @author Kevin Raymond
	 */
	@Test
	public void returnsAllIgnoreCaseSortedCorrectly() throws Exception {

		flushTestUsers();

		Order order = new Order(ASC, "firstname").ignoreCase();
		List<User> result = repository.findAll(new Sort(order));

		assertThat(result, is(notNullValue()));
		assertThat(result.size(), is(4));
		assertThat(result.get(0), is(thirdUser));
		assertThat(result.get(1), is(secondUser));
		assertThat(result.get(2), is(fourthUser));
		assertThat(result.get(3), is(firstUser));
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

		repository.findByLastname((String) null);
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

		assertThat(repository.count(), is(4L));
		assertThat(repository.findAll(), hasItems(firstUser, secondUser, thirdUser, fourthUser));
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
		assertThat(users.hasPrevious(), is(false));
		assertThat(users.getTotalElements(), is(2L));
	}

	@Test
	public void executesMethodWithAnnotatedNamedParametersCorrectly() throws Exception {

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);

		assertTrue(
				repository.findByLastnameOrFirstname("Oliver", "Arrasz").containsAll(Arrays.asList(firstUser, secondUser)));
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
		assertThat(result.size(), is(3));
		assertEquals(fourthUser, result.get(0));
		assertEquals(firstUser, result.get(1));
		assertEquals(secondUser, result.get(2));
	}

	@Test
	public void executesNotLikeCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameNotLike("%er%");
		assertThat(result.size(), is(3));
		assertThat(result, hasItems(secondUser, thirdUser, fourthUser));
	}

	@Test
	public void executesSimpleNotCorrectly() throws Exception {

		flushTestUsers();

		List<User> result = repository.findByLastnameNot("Gierke");
		assertThat(result.size(), is(3));
		assertThat(result, hasItems(secondUser, thirdUser, fourthUser));
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
		assertThat(repository.findAll((Specification<User>) null, pageable), is(repository.findAll(pageable)));
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

		assertThat(repository.count(), is(3L));
	}

	@Test
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

		assertThat(result.size(), is(4));
		assertThat(result, hasItems(firstUser, secondUser, thirdUser, fourthUser));
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

		assertThat(result.size(), is(4));
		assertThat(result.get(0), is(secondUser));
		assertThat(result.get(1), is(firstUser));
		assertThat(result.get(2), is(thirdUser));
		assertThat(result.get(3), is(fourthUser));
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
		assertThat(result.size(), is(3));
		assertThat(result, hasItems(firstUser, secondUser, fourthUser));
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
		assertThat(result.size(), is(3));
		assertThat(result, hasItems(secondUser, thirdUser, fourthUser));
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
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(thirdUser, fourthUser));
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
		assertThat(page.getNumberOfElements(), is(4));
		assertThat(page.getContent(), hasItems(firstUser, secondUser, thirdUser, fourthUser));
	}

	/**
	 * @see DATAJPA-207
	 */
	@Test
	public void executesNativeQueryForNonEntitiesCorrectly() {

		flushTestUsers();

		List<Integer> result = repository.findOnesByNativeQuery();

		assertThat(result.size(), is(4));
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

		em.persist(adminRole);

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);
		fourthUser = repository.save(fourthUser);

		repository.flush();

		id = firstUser.getId();

		assertThat(id, is(notNullValue()));
		assertThat(secondUser.getId(), is(notNullValue()));
		assertThat(thirdUser.getId(), is(notNullValue()));
		assertThat(fourthUser.getId(), is(notNullValue()));

		assertThat(repository.exists(id), is(true));
		assertThat(repository.exists(secondUser.getId()), is(true));
		assertThat(repository.exists(thirdUser.getId()), is(true));
		assertThat(repository.exists(fourthUser.getId()), is(true));
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
	 * @see DATAJPA-346
	 */
	@Test
	public void shouldGenerateLeftOuterJoinInfindAllWithPaginationAndSortOnNestedPropertyPath() {

		firstUser.setManager(null);
		secondUser.setManager(null);
		thirdUser.setManager(firstUser); // manager Oliver
		fourthUser.setManager(secondUser); // manager Joachim

		flushTestUsers();

		Page<User> pages = repository.findAll(new PageRequest(0, 4, new Sort(Sort.Direction.ASC, "manager.firstname")));
		assertThat(pages.getSize(), is(4));
		assertThat(pages.getContent().get(0).getManager(), is(nullValue()));
		assertThat(pages.getContent().get(1).getManager(), is(nullValue()));
		assertThat(pages.getContent().get(2).getManager().getFirstname(), is("Joachim"));
		assertThat(pages.getContent().get(3).getManager().getFirstname(), is("Oliver"));
		assertThat(pages.getTotalElements(), is(4L));
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
	 * @see DATAJPA-231
	 */
	@Test
	public void executesDerivedCountQueryToLong() {

		flushTestUsers();

		assertThat(repository.countByLastname("Matthews"), is(1L));
	}

	/**
	 * @see DATAJPA-231
	 */
	@Test
	public void executesDerivedCountQueryToInt() {

		flushTestUsers();

		assertThat(repository.countUsersByFirstname("Dave"), is(1));
	}

	/**
	 * @see DATAJPA-332
	 */
	@Test
	public void findAllReturnsEmptyIterableIfNoIdsGiven() {

		assertThat(repository.findAll(Collections.<Integer> emptySet()), is(emptyIterable()));
		assertThat(repository.findAll((Iterable<Integer>) null), is(emptyIterable()));
	}

	/**
	 * @see DATAJPA-391
	 */
	@Test
	public void executesManuallyDefinedQueryWithFieldProjection() {

		flushTestUsers();
		List<String> lastname = repository.findFirstnamesByLastname("Matthews");

		assertThat(lastname, hasSize(1));
		assertThat(lastname, hasItem("Dave"));
	}

	/**
	 * @see DATAJPA-83
	 */
	@Test
	public void looksUpEntityReference() {

		flushTestUsers();

		User result = repository.getOne(firstUser.getId());
		assertThat(result, is(firstUser));
	}

	/**
	 * @see DATAJPA-415
	 */
	@Test
	public void invokesQueryWithVarargsParametersCorrectly() {

		flushTestUsers();

		Collection<User> result = repository.findByIdIn(firstUser.getId(), secondUser.getId());

		assertThat(result, hasSize(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	/**
	 * @see DATAJPA-415
	 */
	@Test
	public void shouldSupportModifyingQueryWithVarArgs() {

		flushTestUsers();

		repository.updateUserActiveState(false, firstUser.getId(), secondUser.getId(), thirdUser.getId(),
				fourthUser.getId());

		long expectedCount = repository.count();
		assertThat(repository.findByActiveFalse().size(), is((int) expectedCount));
		assertThat(repository.findByActiveTrue().size(), is(0));
	}

	/**
	 * @see DATAJPA-405
	 */
	@Test
	public void executesFinderWithOrderClauseOnly() {

		flushTestUsers();

		List<User> result = repository.findAllByOrderByLastnameAsc();

		assertThat(result, hasSize(4));
		assertThat(result, contains(secondUser, firstUser, thirdUser, fourthUser));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void sortByAssociationPropertyShouldUseLeftOuterJoin() {

		secondUser.getColleagues().add(firstUser);
		fourthUser.getColleagues().add(thirdUser);
		flushTestUsers();

		List<User> result = repository.findAll(new Sort(Sort.Direction.ASC, "colleagues.id"));

		assertThat(result, hasSize(4));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void sortByAssociationPropertyInPageableShouldUseLeftOuterJoin() {

		secondUser.getColleagues().add(firstUser);
		fourthUser.getColleagues().add(thirdUser);
		flushTestUsers();

		Page<User> page = repository.findAll(new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "colleagues.id")));

		assertThat(page.getContent(), hasSize(4));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void sortByEmbeddedProperty() {

		thirdUser.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));
		flushTestUsers();

		Page<User> page = repository.findAll(new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent(), hasSize(4));
		assertThat(page.getContent().get(3), is(thirdUser));
	}

	/**
	 * @see DATAJPA-454
	 */
	@Test
	public void findsUserByBinaryDataReference() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		List<User> result = repository.findByBinaryData(data);
		assertThat(result, hasSize(1));
		assertThat(result, hasItem(firstUser));
		assertThat(result.get(0).getBinaryData(), is(data));
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void customFindByQueryWithPositionalVarargsParameters() {

		flushTestUsers();

		Collection<User> result = repository.findByIdsCustomWithPositionalVarArgs(firstUser.getId(), secondUser.getId());

		assertThat(result, hasSize(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	/**
	 * @see DATAJPA-461
	 */
	@Test
	public void customFindByQueryWithNamedVarargsParameters() {

		flushTestUsers();

		Collection<User> result = repository.findByIdsCustomWithNamedVarArgs(firstUser.getId(), secondUser.getId());

		assertThat(result, hasSize(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	/**
	 * @see DATAJPA-464
	 */
	@Test
	public void saveAndFlushShouldSupportReturningSubTypesOfRepositoryEntity() {

		repository.deleteAll();
		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		SpecialUser savedUser = repository.saveAndFlush(user);

		assertThat(user.getFirstname(), is(savedUser.getFirstname()));
		assertThat(user.getEmailAddress(), is(savedUser.getEmailAddress()));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByUntypedExampleShouldReturnSubTypesOfRepositoryEntity() {

		flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		repository.saveAndFlush(user);

		List<User> result = repository
				.findAll(Example.of(new User(), ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "dateOfBirth")));

		assertThat(result, hasSize(5));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByTypedUserExampleShouldReturnSubTypesOfRepositoryEntity() {

		flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		repository.saveAndFlush(user);

		Example<User> example = Example.of(new User(), matching().withIgnorePaths("age", "createdAt", "dateOfBirth"));
		List<User> result = repository.findAll(example);

		assertThat(result, hasSize(5));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByTypedSpecialUserExampleShouldReturnSubTypesOfRepositoryEntity() {

		flushTestUsers();

		SpecialUser user = new SpecialUser();
		user.setFirstname("Thomas");
		user.setEmailAddress("thomas@example.org");

		repository.saveAndFlush(user);

		Example<SpecialUser> example = Example.of(new SpecialUser(),
				matching().withIgnorePaths("age", "createdAt", "dateOfBirth"));
		List<SpecialUser> result = repository.findAll(example);

		assertThat(result, hasSize(1));
	}

	/**
	 * @see DATAJPA-491
	 */
	@Test
	public void sortByNestedAssociationPropertyWithSortInPageable() {

		firstUser.setManager(thirdUser);
		thirdUser.setManager(fourthUser);

		flushTestUsers();

		Page<User> page = repository.findAll(new PageRequest(0, 10, //
				new Sort(Sort.Direction.ASC, "manager.manager.firstname")));

		assertThat(page.getContent(), hasSize(4));
		assertThat(page.getContent().get(3), is(firstUser));
	}

	/**
	 * @see DATAJPA-510
	 */
	@Test
	public void sortByNestedAssociationPropertyWithSortOrderIgnoreCaseInPageable() {

		firstUser.setManager(thirdUser);
		thirdUser.setManager(fourthUser);

		flushTestUsers();

		Page<User> page = repository.findAll(new PageRequest(0, 10, //
				new Sort(new Sort.Order(Direction.ASC, "manager.manager.firstname").ignoreCase())));

		assertThat(page.getContent(), hasSize(4));
		assertThat(page.getContent().get(3), is(firstUser));
	}

	/**
	 * @see DATAJPA-496
	 */
	@Test
	public void findByElementCollectionAttribute() {

		firstUser.getAttributes().add("cool");
		secondUser.getAttributes().add("hip");
		thirdUser.getAttributes().add("rockstar");

		flushTestUsers();

		List<User> result = repository.findByAttributesIn(new HashSet<String>(Arrays.asList("cool", "hip")));

		assertThat(result, hasSize(2));
		assertThat(result, hasItems(firstUser, secondUser));
	}

	/**
	 * @see DATAJPA-460
	 */
	@Test
	public void deleteByShouldReturnListOfDeletedElementsWhenRetunTypeIsCollectionLike() {

		flushTestUsers();

		List<User> result = repository.deleteByLastname(firstUser.getLastname());
		assertThat(result, hasItem(firstUser));
		assertThat(result, hasSize(1));
	}

	/**
	 * @see DATAJPA-460
	 */
	@Test
	public void deleteByShouldRemoveElementsMatchingDerivedQuery() {

		flushTestUsers();

		repository.deleteByLastname(firstUser.getLastname());
		assertThat(repository.countByLastname(firstUser.getLastname()), is(0L));
	}

	/**
	 * @see DATAJPA-460
	 */
	@Test
	public void deleteByShouldReturnNumberOfEntitiesRemovedIfReturnTypeIsLong() {

		flushTestUsers();

		assertThat(repository.removeByLastname(firstUser.getLastname()), is(1L));
	}

	/**
	 * @see DATAJPA-460
	 */
	@Test
	public void deleteByShouldReturnZeroInCaseNoEntityHasBeenRemovedAndReturnTypeIsNumber() {

		flushTestUsers();

		assertThat(repository.removeByLastname("bubu"), is(0L));
	}

	/**
	 * @see DATAJPA-460
	 */
	@Test
	public void deleteByShouldReturnEmptyListInCaseNoEntityHasBeenRemovedAndReturnTypeIsCollectionLike() {

		flushTestUsers();

		assertThat(repository.deleteByLastname("dorfuaeB"), empty());
	}

	/**
	 * @see DATAJPA-505
	 * @see https://issues.apache.org/jira/browse/OPENJPA-2484
	 */
	@Test
	@Ignore
	public void findBinaryDataByIdJpaQl() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		byte[] result = null; // repository.findBinaryDataByIdJpaQl(firstUser.getId());

		assertThat(result.length, is(data.length));
		assertThat(result, is(data));
	}

	/**
	 * @see DATAJPA-506
	 */
	@Test
	public void findBinaryDataByIdNative() throws Exception {

		byte[] data = "Woho!!".getBytes("UTF-8");
		firstUser.setBinaryData(data);

		flushTestUsers();

		byte[] result = repository.findBinaryDataByIdNative(firstUser.getId());
		assertThat(result.length, is(data.length));
		assertThat(result, is(data));
	}

	/**
	 * @see DATAJPA-456
	 */
	@Test
	public void findPaginatedExplicitQueryWithCountQueryProjection() {

		firstUser.setFirstname(null);

		flushTestUsers();

		Page<User> result = repository.findAllByFirstnameLike("", new PageRequest(0, 10));

		assertThat(result.getContent().size(), is(3));
	}

	/**
	 * @see DATAJPA-456
	 */
	@Test
	public void findPaginatedNamedQueryWithCountQueryProjection() {

		flushTestUsers();

		Page<User> result = repository.findByNamedQueryAndCountProjection("Gierke", new PageRequest(0, 10));

		assertThat(result.getContent().size(), is(1));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void findOldestUser() {

		flushTestUsers();

		User oldest = thirdUser;

		assertThat(repository.findFirstByOrderByAgeDesc(), is(oldest));
		assertThat(repository.findFirst1ByOrderByAgeDesc(), is(oldest));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void findYoungestUser() {

		flushTestUsers();

		User youngest = firstUser;

		assertThat(repository.findTopByOrderByAgeAsc(), is(youngest));
		assertThat(repository.findTop1ByOrderByAgeAsc(), is(youngest));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void find2OldestUsers() {

		flushTestUsers();

		User oldest1 = thirdUser;
		User oldest2 = secondUser;

		assertThat(repository.findFirst2ByOrderByAgeDesc(), hasItems(oldest1, oldest2));
		assertThat(repository.findTop2ByOrderByAgeDesc(), hasItems(oldest1, oldest2));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void find2YoungestUsers() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;

		assertThat(repository.findFirst2UsersBy(new Sort(ASC, "age")), hasItems(youngest1, youngest2));
		assertThat(repository.findTop2UsersBy(new Sort(ASC, "age")), hasItems(youngest1, youngest2));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void find3YoungestUsersPageableWithPageSize2() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Page<User> firstPage = repository.findFirst3UsersBy(new PageRequest(0, 2, ASC, "age"));
		assertThat(firstPage.getContent(), hasItems(youngest1, youngest2));

		Page<User> secondPage = repository.findFirst3UsersBy(new PageRequest(1, 2, ASC, "age"));
		assertThat(secondPage.getContent(), hasItems(youngest3));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void find2YoungestUsersPageableWithPageSize3() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Page<User> firstPage = repository.findFirst2UsersBy(new PageRequest(0, 3, ASC, "age"));
		assertThat(firstPage.getContent(), hasItems(youngest1, youngest2));

		Page<User> secondPage = repository.findFirst2UsersBy(new PageRequest(1, 3, ASC, "age"));
		assertThat(secondPage.getContent(), hasItems(youngest3));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void find3YoungestUsersPageableWithPageSize2Sliced() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Slice<User> firstPage = repository.findTop3UsersBy(new PageRequest(0, 2, ASC, "age"));
		assertThat(firstPage.getContent(), hasItems(youngest1, youngest2));

		Slice<User> secondPage = repository.findTop3UsersBy(new PageRequest(1, 2, ASC, "age"));
		assertThat(secondPage.getContent(), hasItems(youngest3));
	}

	/**
	 * @see DATAJPA-551
	 */
	@Test
	public void find2YoungestUsersPageableWithPageSize3Sliced() {

		flushTestUsers();

		User youngest1 = firstUser;
		User youngest2 = fourthUser;
		User youngest3 = secondUser;

		Slice<User> firstPage = repository.findTop2UsersBy(new PageRequest(0, 3, ASC, "age"));
		assertThat(firstPage.getContent(), hasItems(youngest1, youngest2));

		Slice<User> secondPage = repository.findTop2UsersBy(new PageRequest(1, 3, ASC, "age"));
		assertThat(secondPage.getContent(), hasItems(youngest3));
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pageableQueryReportsTotalFromResult() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(new PageRequest(0, 10));
		assertThat(firstPage.getContent(), hasSize(4));
		assertThat(firstPage.getTotalElements(), is(4L));

		Page<User> secondPage = repository.findAll(new PageRequest(1, 3));
		assertThat(secondPage.getContent(), hasSize(1));
		assertThat(secondPage.getTotalElements(), is(4L));
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pageableQueryReportsTotalFromCount() {

		flushTestUsers();

		Page<User> firstPage = repository.findAll(new PageRequest(0, 4));
		assertThat(firstPage.getContent(), hasSize(4));
		assertThat(firstPage.getTotalElements(), is(4L));

		Page<User> secondPage = repository.findAll(new PageRequest(10, 10));
		assertThat(secondPage.getContent(), hasSize(0));
		assertThat(secondPage.getTotalElements(), is(4L));
	}

	/**
	 * @see DATAJPA-506
	 */
	@Test
	public void invokesQueryWithWrapperType() {

		flushTestUsers();

		Optional<User> result = repository.findOptionalByEmailAddress("gierke@synyx.de");

		assertThat(result.isPresent(), is(true));
		assertThat(result.get(), is(firstUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindUserByFirstnameAndLastnameWithSpelExpressionInStringBasedQuery() {

		flushTestUsers();
		List<User> users = repository.findByFirstnameAndLastnameWithSpelExpression("Oliver", "ierk");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindUserByLastnameWithSpelExpressionInStringBasedQuery() {

		flushTestUsers();
		List<User> users = repository.findByLastnameWithSpelExpression("ierk");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindBySpELExpressionWithoutArgumentsWithQuestionmark() {

		flushTestUsers();
		List<User> users = repository.findOliverBySpELExpressionWithoutArgumentsWithQuestionmark();

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindBySpELExpressionWithoutArgumentsWithColon() {

		flushTestUsers();
		List<User> users = repository.findOliverBySpELExpressionWithoutArgumentsWithColon();

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindUsersByAgeForSpELExpression() {

		flushTestUsers();
		List<User> users = repository.findUsersByAgeForSpELExpressionByIndexedParameter(35);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldfindUsersByFirstnameForSpELExpressionWithParameterNameVariableReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpression("Joachim");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindCurrentUserWithCustomQueryDependingOnSecurityContext() {

		flushTestUsers();

		SampleSecurityContextHolder.getCurrent().setPrincipal(secondUser);
		List<User> users = repository.findCurrentUserWithCustomQuery();

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));

		SampleSecurityContextHolder.getCurrent().setPrincipal(firstUser);
		users = repository.findCurrentUserWithCustomQuery();

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindByFirstnameAndCurrentUserWithCustomQuery() {

		flushTestUsers();

		SampleSecurityContextHolder.getCurrent().setPrincipal(secondUser);
		List<User> users = repository.findByFirstnameAndCurrentUserWithCustomQuery("Joachim");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterNameVariableReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpressionWithParameterVariableOnly("Joachim");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldfindUsersByFirstnameForSpELExpressionOnlyWithParameterIndexReference() {

		flushTestUsers();
		List<User> users = repository.findUsersByFirstnameForSpELExpressionWithParameterIndexOnly("Joachim");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-564
	 */
	@Test
	public void shouldFindUsersInNativeQueryWithPagination() {

		flushTestUsers();

		Page<User> users = repository.findUsersInNativeQueryWithPagination(new PageRequest(0, 2));

		assertThat(users.getContent(), hasSize(2));
		assertThat(users.getContent().get(0), is(firstUser));
		assertThat(users.getContent().get(1), is(secondUser));

		users = repository.findUsersInNativeQueryWithPagination(new PageRequest(1, 2));

		assertThat(users.getContent(), hasSize(2));
		assertThat(users.getContent().get(0), is(thirdUser));
		assertThat(users.getContent().get(1), is(fourthUser));
	}

	/**
	 * @see DATAJPA-629
	 */
	@Test
	public void shouldfindUsersBySpELExpressionParametersWithSpelTemplateExpression() {

		flushTestUsers();
		List<User> users = repository
				.findUsersByFirstnameForSpELExpressionWithParameterIndexOnlyWithEntityExpression("Joachim", "Arrasz");

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-606
	 */
	@Test
	public void findByEmptyCollectionOfStrings() throws Exception {

		flushTestUsers();

		List<User> users = repository.findByAttributesIn(new HashSet<String>());
		assertThat(users, hasSize(0));
	}

	/**
	 * @see DATAJPA-606
	 */
	@Test
	public void findByEmptyCollectionOfIntegers() throws Exception {

		flushTestUsers();

		List<User> users = repository.findByAgeIn(Arrays.<Integer> asList());
		assertThat(users, hasSize(0));
	}

	/**
	 * @see DATAJPA-606
	 */
	@Test
	public void findByEmptyArrayOfIntegers() throws Exception {

		flushTestUsers();

		List<User> users = repository.queryByAgeIn(new Integer[0]);
		assertThat(users, hasSize(0));
	}

	/**
	 * @see DATAJPA-606
	 */
	@Test
	public void findByAgeWithEmptyArrayOfIntegersOrFirstName() {

		flushTestUsers();

		List<User> users = repository.queryByAgeInOrFirstname(new Integer[0], secondUser.getFirstname());
		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(secondUser));
	}

	/**
	 * @see DATAJPA-677
	 */
	@Test
	public void shouldSupportJava8StreamsForRepositoryFinderMethods() {

		flushTestUsers();

		Stream<User> stream = repository.findAllByCustomQueryAndStream();

		final List<User> users = new ArrayList<User>();

		try {

			stream.forEach(new Consumer<User>() {

				@Override
				public void accept(User user) {
					users.add(user);
				}
			});

		} finally {
			stream.close();
		}

		assertThat(users, hasSize(4));
	}

	/**
	 * @see DATAJPA-677
	 */
	@Test
	public void shouldSupportJava8StreamsForRepositoryDerivedFinderMethods() {

		flushTestUsers();

		Stream<User> stream = repository.readAllByFirstnameNotNull();

		final List<User> users = new ArrayList<User>();

		try {

			stream.forEach(new Consumer<User>() {

				@Override
				public void accept(User user) {
					users.add(user);
				}
			});

		} finally {
			stream.close();
		}

		assertThat(users, hasSize(4));
	}

	/**
	 * @see DATAJPA-677
	 */
	@Test
	public void supportsJava8StreamForPageableMethod() {

		flushTestUsers();

		Stream<User> stream = repository.streamAllPaged(new PageRequest(0, 2));

		final List<User> users = new ArrayList<User>();

		try {

			stream.forEach(new Consumer<User>() {

				@Override
				public void accept(User user) {
					users.add(user);
				}
			});

		} finally {
			stream.close();
		}

		assertThat(users, hasSize(2));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExample() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);
		prototype.setCreatedAt(null);

		List<User> users = repository.findAll(of(prototype));

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithEmptyProbe() {

		flushTestUsers();

		User prototype = new User();
		prototype.setCreatedAt(null);

		List<User> users = repository
				.findAll(of(prototype, ExampleMatcher.matching().withIgnorePaths("age", "createdAt", "active")));

		assertThat(users, hasSize(4));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByNullExample() {
		repository.findAll((Example<User>) null);
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithAssociation() {

		flushTestUsers();

		firstUser.setManager(secondUser);
		thirdUser.setManager(firstUser);
		repository.save(Arrays.asList(firstUser, thirdUser));

		User manager = new User();
		manager.setLastname("Arrasz");
		manager.setAge(secondUser.getAge());
		manager.setCreatedAt(null);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setManager(manager);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("age"));
		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithEmbedded() {

		flushTestUsers();

		firstUser.setAddress(new Address("germany", "dresden", "", ""));
		repository.save(firstUser);

		User prototype = new User();
		prototype.setCreatedAt(null);
		prototype.setAddress(new Address("germany", null, null, null));

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("age"));
		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithStartingStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("Ol");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.STARTING).withIgnorePaths("age", "createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithEndingStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("ver");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.ENDING).withIgnorePaths("age", "createdAt"));
		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByExampleWithRegexStringMatcher() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("^Oliver$");

		Example<User> example = Example.of(prototype, matching().withStringMatcher(StringMatcher.REGEX));
		repository.findAll(example);
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithIgnoreCase() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiVer");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithStringMatcherAndIgnoreCase() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLiV");

		Example<User> example = Example.of(prototype,
				matching().withStringMatcher(StringMatcher.STARTING).withIgnoreCase().withIgnorePaths("age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithIncludeNull() {

		// something is wrong with OpenJPA - I do not know what
		Assume.assumeThat(PersistenceProvider.fromEntityManager(em), not(equalTo(PersistenceProvider.OPEN_JPA)));

		flushTestUsers();

		firstUser.setAddress(new Address("andor", "caemlyn", "", ""));

		User fifthUser = new User();
		fifthUser.setEmailAddress("foo@bar.com");
		fifthUser.setActive(firstUser.isActive());
		fifthUser.setAge(firstUser.getAge());
		fifthUser.setFirstname(firstUser.getFirstname());
		fifthUser.setLastname(firstUser.getLastname());

		repository.save(Arrays.asList(firstUser, fifthUser));

		User prototype = new User();
		prototype.setFirstname(firstUser.getFirstname());

		Example<User> example = Example.of(prototype, matching().withIncludeNullValues().withIgnorePaths("id", "binaryData",
				"lastname", "emailAddress", "age", "createdAt"));

		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(fifthUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithPropertySpecifier() {

		flushTestUsers();

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withMatcher("firstname", new GenericPropertyMatcher().startsWith()));

		List<User> users = repository.findAll(example);

		assertThat(users, hasSize(1));
		assertThat(users.get(0), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findAllByExampleWithSort() {

		flushTestUsers();

		User user1 = new User("Oliver", "Srping", "o@s.de");
		user1.setAge(30);

		repository.save(user1);

		User prototype = new User();
		prototype.setFirstname("oLi");

		Example<User> example = Example.of(prototype, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		List<User> users = repository.findAll(example, new Sort(DESC, "age"));

		assertThat(users, hasSize(2));
		assertThat(users.get(0), is(user1));
		assertThat(users.get(1), is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
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

		Page<User> users = repository.findAll(example, new PageRequest(0, 10, new Sort(DESC, "age")));

		assertThat(users.getSize(), is(10));
		assertThat(users.hasNext(), is(true));
		assertThat(users.getTotalElements(), is(100L));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
	public void findAllByExampleShouldNotAllowCycles() {

		flushTestUsers();

		User user1 = new User();
		user1.setFirstname("user1");

		user1.setManager(user1);

		Example<User> example = Example.of(user1, matching().withIgnoreCase().withIgnorePaths("age", "createdAt")
				.withStringMatcher(StringMatcher.STARTING).withIgnoreCase());

		repository.findAll(example, new PageRequest(0, 10, new Sort(DESC, "age")));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test(expected = InvalidDataAccessApiUsageException.class)
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

		repository.findAll(example, new PageRequest(0, 10, new Sort(DESC, "age")));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void findOneByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		User users = repository.findOne(example);

		assertThat(users, is(firstUser));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void countByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		long count = repository.count(example);

		assertThat(count, is(1L));
	}

	/**
	 * @see DATAJPA-218
	 */
	@Test
	public void existsByExampleWithExcludedAttributes() {

		flushTestUsers();

		User prototype = new User();
		prototype.setAge(28);

		Example<User> example = Example.of(prototype, matching().withIgnorePaths("createdAt"));
		boolean exists = repository.exists(example);

		assertThat(exists, is(true));
	}

	/**
	 * @see DATAJPA-905
	 */
	@Test
	public void excutesPagedSpecificationSettingAnOrder() {

		flushTestUsers();

		Page<User> result = repository.findAll(where(userHasLastnameLikeWithSort("e")), new PageRequest(0, 1));

		assertThat(result.getTotalElements(), is(2L));
		assertThat(result.getNumberOfElements(), is(1));
		assertThat(result.getContent().get(0), is(thirdUser));
	}

	private Page<User> executeSpecWithSort(Sort sort) {

		flushTestUsers();

		Specification<User> spec = where(userHasFirstname("Oliver")).or(userHasLastname("Matthews"));

		Page<User> result = repository.findAll(spec, new PageRequest(0, 1, sort));
		assertThat(result.getTotalElements(), is(2L));
		return result;
	}
}
