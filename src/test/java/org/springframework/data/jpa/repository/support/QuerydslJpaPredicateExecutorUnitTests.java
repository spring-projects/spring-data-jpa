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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
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
import org.springframework.data.querydsl.SimpleEntityPathResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.PathBuilderFactory;

/**
 * Integration test for {@link QuerydslJpaPredicateExecutor}.
 *
 * @author Jens Schauder
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QuerydslJpaPredicateExecutorUnitTests {

	@PersistenceContext EntityManager em;

	QuerydslJpaPredicateExecutor<User> predicateExecutor;
	QUser user = new QUser("user");
	User dave, carter, oliver;
	Role adminRole;

	@Before
	public void setUp() {

		JpaEntityInformation<User, Integer> information = new JpaMetamodelEntityInformation<>(User.class,
				em.getMetamodel());

		SimpleJpaRepository<User, Integer> repository = new SimpleJpaRepository<>(information, em);
		dave = repository.save(new User("Dave", "Matthews", "dave@matthews.com"));
		carter = repository.save(new User("Carter", "Beauford", "carter@beauford.com"));
		oliver = repository.save(new User("Oliver", "matthews", "oliver@matthews.com"));
		adminRole = em.merge(new Role("admin"));

		this.predicateExecutor = new QuerydslJpaPredicateExecutor<>(information, em, SimpleEntityPathResolver.INSTANCE, null);
	}

	@Test
	public void executesPredicatesCorrectly() throws Exception {

		BooleanExpression isCalledDave = user.firstname.eq("Dave");
		BooleanExpression isBeauford = user.lastname.eq("Beauford");

		List<User> result = predicateExecutor.findAll(isCalledDave.or(isBeauford));

		assertThat(result).containsExactlyInAnyOrder(carter, dave);
	}

	@Test
	public void executesStringBasedPredicatesCorrectly() throws Exception {

		PathBuilder<User> builder = new PathBuilderFactory().create(User.class);

		BooleanExpression isCalledDave = builder.getString("firstname").eq("Dave");
		BooleanExpression isBeauford = builder.getString("lastname").eq("Beauford");

		List<User> result = predicateExecutor.findAll(isCalledDave.or(isBeauford));

		assertThat(result).containsExactlyInAnyOrder(carter, dave);
	}

	@Test // DATAJPA-243
	public void considersSortingProvidedThroughPageable() {

		Predicate lastnameContainsE = user.lastname.contains("e");

		Page<User> result = predicateExecutor.findAll(lastnameContainsE, PageRequest.of(0, 1, Direction.ASC, "lastname"));

		assertThat(result).containsExactly(carter);

		result = predicateExecutor.findAll(lastnameContainsE, PageRequest.of(0, 2, Direction.DESC, "lastname"));

		assertThat(result).containsExactly(oliver, dave);
	}

	@Test // DATAJPA-296
	public void appliesIgnoreCaseOrdering() {

		Sort sort = Sort.by(new Order(Direction.DESC, "lastname").ignoreCase(), new Order(Direction.ASC, "firstname"));

		Page<User> result = predicateExecutor.findAll(user.lastname.contains("e"), PageRequest.of(0, 2, sort));

		assertThat(result.getContent()).containsExactly(dave, oliver);
	}

	@Test // DATAJPA-427
	public void findBySpecificationWithSortByPluralAssociationPropertyInPageableShouldUseSortNullValuesLast() {

		oliver.getColleagues().add(dave);
		dave.getColleagues().add(oliver);

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "colleagues.firstname")));

		assertThat(page.getContent()).hasSize(3).contains(oliver, dave, carter);
	}

	@Test // DATAJPA-427
	public void findBySpecificationWithSortBySingularAssociationPropertyInPageableShouldUseSortNullValuesLast() {

		oliver.setManager(dave);
		dave.setManager(carter);

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "manager.firstname")));

		assertThat(page.getContent()).hasSize(3).contains(dave, oliver, carter);
	}

	@Test // DATAJPA-427
	public void findBySpecificationWithSortBySingularPropertyInPageableShouldUseSortNullValuesFirst() {

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "firstname")));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-427
	public void findBySpecificationWithSortByOrderIgnoreCaseBySingularPropertyInPageableShouldUseSortNullValuesFirst() {

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(new Order(Sort.Direction.ASC, "firstname").ignoreCase())));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-427
	public void findBySpecificationWithSortByNestedEmbeddedPropertyInPageableShouldUseSortNullValuesFirst() {

		oliver.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent()).containsExactly(dave, carter, oliver);
	}

	@Test // DATAJPA-12
	public void findBySpecificationWithSortByQueryDslOrderSpecifierWithQPageRequestAndQSort() {

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				new QPageRequest(0, 10, new QSort(user.firstname.asc())));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-12
	public void findBySpecificationWithSortByQueryDslOrderSpecifierWithQPageRequest() {

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(), new QPageRequest(0, 10, user.firstname.asc()));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-12
	public void findBySpecificationWithSortByQueryDslOrderSpecifierForAssociationShouldGenerateLeftJoinWithQPageRequest() {

		oliver.setManager(dave);
		dave.setManager(carter);

		QUser user = QUser.user;

		Page<User> page = predicateExecutor.findAll(user.firstname.isNotNull(),
				new QPageRequest(0, 10, user.manager.firstname.asc()));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-500, DATAJPA-635
	public void sortByNestedEmbeddedAttribute() {

		carter.setAddress(new Address("U", "Z", "Y", "41"));
		dave.setAddress(new Address("U", "A", "Y", "41"));
		oliver.setAddress(new Address("G", "D", "X", "42"));

		List<User> users = predicateExecutor.findAll(QUser.user.address.streetName.asc());

		assertThat(users).hasSize(3).contains(dave, oliver, carter);
	}

	@Test // DATAJPA-566, DATAJPA-635
	public void shouldSupportSortByOperatorWithDateExpressions() {

		carter.setDateOfBirth(new LocalDate(2000, 2, 1).toDate());
		dave.setDateOfBirth(new LocalDate(2000, 1, 1).toDate());
		oliver.setDateOfBirth(new LocalDate(2003, 5, 1).toDate());

		List<User> users = predicateExecutor.findAll(QUser.user.dateOfBirth.yearMonth().asc());

		assertThat(users).containsExactly(dave, carter, oliver);
	}

	@Test // DATAJPA-665
	public void shouldSupportExistsWithPredicate() throws Exception {

		assertThat(predicateExecutor.exists(user.firstname.eq("Dave"))).isEqualTo(true);
		assertThat(predicateExecutor.exists(user.firstname.eq("Unknown"))).isEqualTo(false);
		assertThat(predicateExecutor.exists((Predicate) null)).isEqualTo(true);
	}

	@Test // DATAJPA-679
	public void shouldSupportFindAllWithPredicateAndSort() {

		List<User> users = predicateExecutor.findAll(user.dateOfBirth.isNull(), Sort.by(Direction.ASC, "firstname"));

		assertThat(users).contains(carter, dave, oliver);
	}

	@Test // DATAJPA-585
	public void worksWithUnpagedPageable() {
		assertThat(predicateExecutor.findAll(user.dateOfBirth.isNull(), Pageable.unpaged()).getContent()).hasSize(3);
	}

	@Test // DATAJPA-912
	public void pageableQueryReportsTotalFromResult() {

		Page<User> firstPage = predicateExecutor.findAll(user.dateOfBirth.isNull(), PageRequest.of(0, 10));
		assertThat(firstPage.getContent()).hasSize(3);
		assertThat(firstPage.getTotalElements()).isEqualTo(3L);

		Page<User> secondPage = predicateExecutor.findAll(user.dateOfBirth.isNull(), PageRequest.of(1, 2));
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.getTotalElements()).isEqualTo(3L);
	}

	@Test // DATAJPA-912
	public void pageableQueryReportsTotalFromCount() {

		Page<User> firstPage = predicateExecutor.findAll(user.dateOfBirth.isNull(), PageRequest.of(0, 3));
		assertThat(firstPage.getContent()).hasSize(3);
		assertThat(firstPage.getTotalElements()).isEqualTo(3L);

		Page<User> secondPage = predicateExecutor.findAll(user.dateOfBirth.isNull(), PageRequest.of(10, 10));
		assertThat(secondPage.getContent()).hasSize(0);
		assertThat(secondPage.getTotalElements()).isEqualTo(3L);
	}

	@Test // DATAJPA-1115
	public void findOneWithPredicateReturnsResultCorrectly() {
		assertThat(predicateExecutor.findOne(user.eq(dave))).contains(dave);
	}

	@Test // DATAJPA-1115
	public void findOneWithPredicateReturnsOptionalEmptyWhenNoDataFound() {
		assertThat(predicateExecutor.findOne(user.firstname.eq("batman"))).isNotPresent();
	}

	@Test(expected = IncorrectResultSizeDataAccessException.class) // DATAJPA-1115
	public void findOneWithPredicateThrowsExceptionForNonUniqueResults() {
		predicateExecutor.findOne(user.emailAddress.contains("com"));
	}
}
