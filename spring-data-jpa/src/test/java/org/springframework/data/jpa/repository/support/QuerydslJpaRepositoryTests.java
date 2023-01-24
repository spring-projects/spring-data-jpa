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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.sql.Date;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.Predicate;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.core.types.dsl.PathBuilderFactory;

/**
 * Integration test for {@link QuerydslJpaRepository}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Malte Mauelshagen
 * @author Greg Turnquist
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
class QuerydslJpaRepositoryTests {

	@PersistenceContext EntityManager em;

	private QuerydslJpaRepository<User, Integer> repository;
	private QUser user = new QUser("user");
	private User dave;
	private User carter;
	private User oliver;
	private Role adminRole;

	@BeforeEach
	void setUp() {

		JpaEntityInformation<User, Integer> information = new JpaMetamodelEntityInformation<>(User.class, em.getMetamodel(),
				em.getEntityManagerFactory().getPersistenceUnitUtil());

		repository = new QuerydslJpaRepository<>(information, em);
		dave = repository.save(new User("Dave", "Matthews", "dave@matthews.com"));
		carter = repository.save(new User("Carter", "Beauford", "carter@beauford.com"));
		oliver = repository.save(new User("Oliver", "matthews", "oliver@matthews.com"));
		adminRole = em.merge(new Role("admin"));
	}

	@Test
	void executesPredicatesCorrectly() {

		BooleanExpression isCalledDave = user.firstname.eq("Dave");
		BooleanExpression isBeauford = user.lastname.eq("Beauford");

		List<User> result = repository.findAll(isCalledDave.or(isBeauford));

		assertThat(result).containsExactlyInAnyOrder(carter, dave);
	}

	@Test
	void executesStringBasedPredicatesCorrectly() {

		PathBuilder<User> builder = new PathBuilderFactory().create(User.class);

		BooleanExpression isCalledDave = builder.getString("firstname").eq("Dave");
		BooleanExpression isBeauford = builder.getString("lastname").eq("Beauford");

		List<User> result = repository.findAll(isCalledDave.or(isBeauford));

		assertThat(result).containsExactlyInAnyOrder(carter, dave);
	}

	@Test // DATAJPA-243
	void considersSortingProvidedThroughPageable() {

		Predicate lastnameContainsE = user.lastname.contains("e");

		Page<User> result = repository.findAll(lastnameContainsE, PageRequest.of(0, 1, Direction.ASC, "lastname"));

		assertThat(result).containsExactly(carter);

		result = repository.findAll(lastnameContainsE, PageRequest.of(0, 2, Direction.DESC, "lastname"));

		assertThat(result).containsExactly(oliver, dave);
	}

	@Test // DATAJPA-296
	void appliesIgnoreCaseOrdering() {

		Sort sort = Sort.by(new Order(Direction.DESC, "lastname").ignoreCase(), new Order(Direction.ASC, "firstname"));

		Page<User> result = repository.findAll(user.lastname.contains("e"), PageRequest.of(0, 2, sort));

		assertThat(result.getContent()).containsExactly(dave, oliver);
	}

	@Test // DATAJPA-427
	void findBySpecificationWithSortByPluralAssociationPropertyInPageableShouldUseSortNullValuesLast() {

		oliver.getColleagues().add(dave);
		dave.getColleagues().add(oliver);

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "colleagues.firstname")));

		assertThat(page.getContent()).hasSize(3).contains(oliver, dave, carter);
	}

	@Test // DATAJPA-427
	void findBySpecificationWithSortBySingularAssociationPropertyInPageableShouldUseSortNullValuesLast() {

		oliver.setManager(dave);
		dave.setManager(carter);

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "manager.firstname")));

		assertThat(page.getContent()).hasSize(3).contains(dave, oliver, carter);
	}

	@Test // DATAJPA-427
	void findBySpecificationWithSortBySingularPropertyInPageableShouldUseSortNullValuesFirst() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "firstname")));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-427
	void findBySpecificationWithSortByOrderIgnoreCaseBySingularPropertyInPageableShouldUseSortNullValuesFirst() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(new Order(Sort.Direction.ASC, "firstname").ignoreCase())));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-427
	void findBySpecificationWithSortByNestedEmbeddedPropertyInPageableShouldUseSortNullValuesFirst() {

		oliver.setAddress(new Address("Germany", "Saarbrücken", "HaveItYourWay", "123"));

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "address.streetName")));

		assertThat(page.getContent()).containsExactly(dave, carter, oliver);
	}

	@Test // DATAJPA-12
	void findBySpecificationWithSortByQueryDslOrderSpecifierWithQPageRequestAndQSort() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				QPageRequest.of(0, 10, new QSort(user.firstname.asc())));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-12
	void findBySpecificationWithSortByQueryDslOrderSpecifierWithQPageRequest() {

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(), QPageRequest.of(0, 10, user.firstname.asc()));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-12
	void findBySpecificationWithSortByQueryDslOrderSpecifierForAssociationShouldGenerateLeftJoinWithQPageRequest() {

		oliver.setManager(dave);
		dave.setManager(carter);

		QUser user = QUser.user;

		Page<User> page = repository.findAll(user.firstname.isNotNull(),
				QPageRequest.of(0, 10, user.manager.firstname.asc()));

		assertThat(page.getContent()).containsExactly(carter, dave, oliver);
	}

	@Test // DATAJPA-491
	void sortByNestedAssociationPropertyWithSpecificationAndSortInPageable() {

		oliver.setManager(dave);
		dave.getRoles().add(adminRole);

		Page<User> page = repository.findAll(PageRequest.of(0, 10, Sort.by(Direction.ASC, "manager.roles.name")));

		assertThat(page.getContent()).hasSize(3);
		assertThat(page.getContent().get(0)).isEqualTo(dave);
	}

	@Test // DATAJPA-500, DATAJPA-635
	void sortByNestedEmbeddedAttribute() {

		carter.setAddress(new Address("U", "Z", "Y", "41"));
		dave.setAddress(new Address("U", "A", "Y", "41"));
		oliver.setAddress(new Address("G", "D", "X", "42"));

		List<User> users = repository.findAll(QUser.user.address.streetName.asc());

		assertThat(users).hasSize(3).contains(dave, oliver, carter);
	}

	@Test // DATAJPA-566, DATAJPA-635
	void shouldSupportSortByOperatorWithDateExpressions() {

		carter.setDateOfBirth(Date.valueOf(LocalDate.of(2000, 2, 1)));
		dave.setDateOfBirth(Date.valueOf(LocalDate.of(2000, 1, 1)));
		oliver.setDateOfBirth(Date.valueOf(LocalDate.of(2003, 5, 1)));

		List<User> users = repository.findAll(QUser.user.dateOfBirth.yearMonth().asc());

		assertThat(users).containsExactly(dave, carter, oliver);
	}

	@Test // DATAJPA-665
	void shouldSupportExistsWithPredicate() {

		assertThat(repository.exists(user.firstname.eq("Dave"))).isTrue();
		assertThat(repository.exists(user.firstname.eq("Unknown"))).isFalse();
		assertThat(repository.exists((Predicate) null)).isTrue();
	}

	@Test // DATAJPA-679
	void shouldSupportFindAllWithPredicateAndSort() {

		List<User> users = repository.findAll(user.dateOfBirth.isNull(), Sort.by(Direction.ASC, "firstname"));

		assertThat(users).contains(carter, dave, oliver);
	}

	@Test // DATAJPA-585
	void worksWithUnpagedPageable() {
		assertThat(repository.findAll(user.dateOfBirth.isNull(), Pageable.unpaged()).getContent()).hasSize(3);
	}

	@Test // DATAJPA-912
	void pageableQueryReportsTotalFromResult() {

		Page<User> firstPage = repository.findAll(user.dateOfBirth.isNull(), PageRequest.of(0, 10));
		assertThat(firstPage.getContent()).hasSize(3);
		assertThat(firstPage.getTotalElements()).isEqualTo(3L);

		Page<User> secondPage = repository.findAll(user.dateOfBirth.isNull(), PageRequest.of(1, 2));
		assertThat(secondPage.getContent()).hasSize(1);
		assertThat(secondPage.getTotalElements()).isEqualTo(3L);
	}

	@Test // DATAJPA-912
	void pageableQueryReportsTotalFromCount() {

		Page<User> firstPage = repository.findAll(user.dateOfBirth.isNull(), PageRequest.of(0, 3));
		assertThat(firstPage.getContent()).hasSize(3);
		assertThat(firstPage.getTotalElements()).isEqualTo(3L);

		Page<User> secondPage = repository.findAll(user.dateOfBirth.isNull(), PageRequest.of(10, 10));
		assertThat(secondPage.getContent()).isEmpty();
		assertThat(secondPage.getTotalElements()).isEqualTo(3L);
	}

	@Test // DATAJPA-1115
	void findOneWithPredicateReturnsResultCorrectly() {
		assertThat(repository.findOne(user.eq(dave))).contains(dave);
	}

	@Test // DATAJPA-1115
	void findOneWithPredicateReturnsOptionalEmptyWhenNoDataFound() {
		assertThat(repository.findOne(user.firstname.eq("batman"))).isNotPresent();
	}

	@Test // DATAJPA-1115
	void findOneWithPredicateThrowsExceptionForNonUniqueResults() {

		assertThatExceptionOfType(IncorrectResultSizeDataAccessException.class)
				.isThrownBy(() -> repository.findOne(user.emailAddress.contains("com")));
	}

	@Test // GH-2294
	void findByFluentQuery() {

		assertThatExceptionOfType(UnsupportedOperationException.class)
				.isThrownBy(() -> repository.findBy(user.firstname.eq("Dave"), q -> q.sortBy(Sort.by("firstname")).all()));
	}
}
