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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.sample.Address;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.querydsl.QPageRequest;
import org.springframework.data.querydsl.QSort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.PathBuilderFactory;

/**
 * Integration test for {@link QueryDslJpaRepository}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QueryDslJpaRepositoryTests {

	@PersistenceContext EntityManager em;

	QueryDslJpaRepository<User, Integer> repository;
	QUser user = new QUser("user");
	User dave, carter, oliver;
	Role adminRole;

	@Before
	public void setUp() {

		JpaEntityInformation<User, Integer> information = new JpaMetamodelEntityInformation<User, Integer>(User.class,
				em.getMetamodel());

		repository = new QueryDslJpaRepository<User, Integer>(information, em);
		dave = repository.save(new User("Dave", "Matthews", "dave@matthews.com"));
		carter = repository.save(new User("Carter", "Beauford", "carter@beauford.com"));
		oliver = repository.save(new User("Oliver", "matthews", "oliver@matthews.com"));
		adminRole = em.merge(new Role("admin"));
	}

	@Test
	public void executesPredicatesCorrectly() throws Exception {

		BooleanExpression isCalledDave = user.firstname.eq("Dave");
		BooleanExpression isBeauford = user.lastname.eq("Beauford");

		List<User> result = repository.findAll(isCalledDave.or(isBeauford));

		assertThat(result.size(), is(2));
		assertThat(result, hasItems(carter, dave));
	}

	@Test
	public void executesStringBasedPredicatesCorrectly() throws Exception {

		PathBuilder<User> builder = new PathBuilderFactory().create(User.class);

		BooleanExpression isCalledDave = builder.getString("firstname").eq("Dave");
		BooleanExpression isBeauford = builder.getString("lastname").eq("Beauford");

		List<User> result = repository.findAll(isCalledDave.or(isBeauford));

		assertThat(result.size(), is(2));
		assertThat(result, hasItems(carter, dave));
	}

	/**
	 * @see DATAJPA-243
	 */
	@Test
	public void considersSortingProvidedThroughPageable() {

		Predicate lastnameContainsE = user.lastname.contains("e");

		Page<User> result = repository.findAll(lastnameContainsE, new PageRequest(0, 1, Direction.ASC, "lastname"));

		assertThat(result.getContent(), hasSize(1));
		assertThat(result.getContent().get(0), is(carter));

		result = repository.findAll(lastnameContainsE, new PageRequest(0, 2, Direction.DESC, "lastname"));

		assertThat(result.getContent(), hasSize(2));
		assertThat(result.getContent().get(0), is(oliver));
		assertThat(result.getContent().get(1), is(dave));
	}

	/**
	 * @see DATAJPA-296
	 */
	@Test
	public void appliesIgnoreCaseOrdering() {

		Sort sort = new Sort(new Order(Direction.DESC, "lastname").ignoreCase(), new Order(Direction.ASC, "firstname"));

		Page<User> result = repository.findAll(user.lastname.contains("e"), new PageRequest(0, 2, sort));

		assertThat(result.getContent(), hasSize(2));
		assertThat(result.getContent().get(0), is(dave));
		assertThat(result.getContent().get(1), is(oliver));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void findBySpecificationWithSortByPluralAssociationPropertyInPageableShouldUseSortNullValuesLast() {

		oliver.getColleagues().add(dave);
		dave.getColleagues().add(oliver);

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "colleagues.firstname")));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(oliver, dave, carter));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void findBySpecificationWithSortBySingularAssociationPropertyInPageableShouldUseSortNullValuesLast() {

		oliver.setManager(dave);
		dave.setManager(carter);

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "manager.firstname")));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(dave, oliver, carter));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void findBySpecificationWithSortBySingularPropertyInPageableShouldUseSortNullValuesFirst() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "firstname")));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(carter, dave, oliver));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void findBySpecificationWithSortByOrderIgnoreCaseBySingularPropertyInPageableShouldUseSortNullValuesFirst() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new PageRequest(0, 10, new Sort(new Order(Sort.Direction.ASC, "firstname").ignoreCase())));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(carter, dave, oliver));
	}

	/**
	 * @see DATAJPA-427
	 */
	@Test
	public void findBySpecificationWithSortByNestedEmbeddedPropertyInPageableShouldUseSortNullValuesFirst() {

		oliver.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(dave, carter, oliver));
		assertThat(page.getContent().get(2), is(oliver));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void findBySpecificationWithSortByQueryDslOrderSpecifierWithQPageRequestAndQSort() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new QPageRequest(0, 10, new QSort(user.firstname.asc())));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(carter, dave, oliver));
		assertThat(page.getContent().get(0), is(carter));
		assertThat(page.getContent().get(1), is(dave));
		assertThat(page.getContent().get(2), is(oliver));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void findBySpecificationWithSortByQueryDslOrderSpecifierWithQPageRequest() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(), new QPageRequest(0, 10, user.firstname.asc()));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(carter, dave, oliver));
		assertThat(page.getContent().get(0), is(carter));
		assertThat(page.getContent().get(1), is(dave));
		assertThat(page.getContent().get(2), is(oliver));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void findBySpecificationWithSortByQueryDslOrderSpecifierForAssociationShouldGenerateLeftJoinWithQPageRequest() {

		oliver.setManager(dave);
		dave.setManager(carter);

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				new QPageRequest(0, 10, user.manager.firstname.asc()));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent(), hasItems(carter, dave, oliver));
		assertThat(page.getContent().get(0), is(carter));
		assertThat(page.getContent().get(1), is(dave));
		assertThat(page.getContent().get(2), is(oliver));
	}

	/**
	 * @see DATAJPA-491
	 */
	@Test
	public void sortByNestedAssociationPropertyWithSpecificationAndSortInPageable() {

		oliver.setManager(dave);
		dave.getRoles().add(adminRole);

		Page<User> page = repository.findAll(new PageRequest(0, 10, new Sort(Sort.Direction.ASC, "manager.roles.name")));

		assertThat(page.getContent(), hasSize(3));
		assertThat(page.getContent().get(0), is(dave));
	}

	/**
	 * @see DATAJPA-500, DATAJPA-635
	 */
	@Test
	public void sortByNestedEmbeddedAttribite() {

		carter.setAddress(new Address("U", "Z", "Y", "41"));
		dave.setAddress(new Address("U", "A", "Y", "41"));
		oliver.setAddress(new Address("G", "D", "X", "42"));

		List<User> users = repository.findAll(QUser.user.address.streetName.asc());

		assertThat(users, hasSize(3));
		assertThat(users, hasItems(dave, oliver, carter));
	}

	/**
	 * @see DATAJPA-566, DATAJPA-635
	 */
	@Test
	public void shouldSupportSortByOperatorWithDateExpressions() {

		carter.setDateOfBirth(new LocalDate(2000, 2, 1).toDate());
		dave.setDateOfBirth(new LocalDate(2000, 1, 1).toDate());
		oliver.setDateOfBirth(new LocalDate(2003, 5, 1).toDate());

		List<User> users = repository.findAll(QUser.user.dateOfBirth.yearMonth().asc());

		assertThat(users, hasSize(3));
		assertThat(users, hasItems(dave, carter, oliver));
	}

	/**
	 * @see DATAJPA-665
	 */
	@Test
	public void shouldSupportExistsWithPredicate() throws Exception {

		assertThat(repository.exists(user.firstname.eq("Dave")), is(true));
		assertThat(repository.exists(user.firstname.eq("Unknown")), is(false));
		assertThat(repository.exists((Predicate) null), is(true));
	}

	/**
	 * @see DATAJPA-679
	 */
	@Test
	public void shouldSupportFindAllWithPredicateAndSort() {

		List<User> users = repository.findAll(user.dateOfBirth.isNull(), new Sort(Direction.ASC, "firstname"));

		assertThat(users, hasSize(3));
		assertThat(users.get(0).getFirstname(), is(carter.getFirstname()));
		assertThat(users.get(2).getFirstname(), is(oliver.getFirstname()));
		assertThat(users, hasItems(carter, dave, oliver));
	}

	/**
	 * @see DATAJPA-585
	 */
	@Test
	public void worksWithNullPageable() {
		assertThat(repository.findAll(user.dateOfBirth.isNull(), (Pageable) null).getContent(), hasSize(3));
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pageableQueryReportsTotalFromResult() {

		Page<User> firstPage = repository.findAll(user.dateOfBirth.isNull(), new PageRequest(0, 10));
		assertThat(firstPage.getContent(), hasSize(3));
		assertThat(firstPage.getTotalElements(), is(3L));

		Page<User> secondPage = repository.findAll(user.dateOfBirth.isNull(), new PageRequest(1, 2));
		assertThat(secondPage.getContent(), hasSize(1));
		assertThat(secondPage.getTotalElements(), is(3L));
	}

	/**
	 * @see DATAJPA-912
	 */
	@Test
	public void pageableQueryReportsTotalFromCount() {

		Page<User> firstPage = repository.findAll(user.dateOfBirth.isNull(), new PageRequest(0, 3));
		assertThat(firstPage.getContent(), hasSize(3));
		assertThat(firstPage.getTotalElements(), is(3L));

		Page<User> secondPage = repository.findAll(user.dateOfBirth.isNull(), new PageRequest(10, 10));
		assertThat(secondPage.getContent(), hasSize(0));
		assertThat(secondPage.getTotalElements(), is(3L));
	}
}
