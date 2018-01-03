/*
 * Copyright 2013-2018 the original author or authors.
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
package org.springframework.data.jpa.domain;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.jpa.domain.JpaSort.*;

import javax.persistence.EntityManagerFactory;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.PluralAttribute;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort.JpaOrder;
import org.springframework.data.jpa.domain.JpaSort.Path;
import org.springframework.data.jpa.domain.sample.Address_;
import org.springframework.data.jpa.domain.sample.MailMessage_;
import org.springframework.data.jpa.domain.sample.MailSender_;
import org.springframework.data.jpa.domain.sample.User_;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link JpaSort}. This has to be an integration test due to the design of the statically
 * generated meta-model classes. The properties cannot be referred to statically (quite a surprise, as they're static)
 * but only after they've been enhanced by the persistence provider. This requires an {@link EntityManagerFactory} to be
 * bootstrapped.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class JpaSortTests {

	private static final @Nullable Attribute<?, ?> NULL_ATTRIBUTE = null;
	private static final Attribute<?, ?>[] EMPTY_ATTRIBUTES = new Attribute<?, ?>[0];

	private static final @Nullable PluralAttribute<?, ?, ?> NULL_PLURAL_ATTRIBUTE = null;
	private static final PluralAttribute<?, ?, ?>[] EMPTY_PLURAL_ATTRIBUTES = new PluralAttribute<?, ?, ?>[0];

	@Test(expected = IllegalArgumentException.class) // DATAJPA-12
	public void rejectsNullAttribute() {
		new JpaSort(NULL_ATTRIBUTE);
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-12
	public void rejectsEmptyAttributes() {
		new JpaSort(EMPTY_ATTRIBUTES);
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-12
	public void rejectsNullPluralAttribute() {
		new JpaSort(NULL_PLURAL_ATTRIBUTE);
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-12
	public void rejectsEmptyPluralAttributes() {
		new JpaSort(EMPTY_PLURAL_ATTRIBUTES);
	}

	@Test // DATAJPA-12
	public void sortBySinglePropertyWithDefaultSortDirection() {
		assertThat(new JpaSort(path(User_.firstname)), hasItems(new Sort.Order("firstname")));
	}

	@Test // DATAJPA-12
	public void sortByMultiplePropertiesWithDefaultSortDirection() {
		assertThat(new JpaSort(User_.firstname, User_.lastname), hasItems(new Order("firstname"), new Order("lastname")));
	}

	@Test // DATAJPA-12
	public void sortByMultiplePropertiesWithDescSortDirection() {

		assertThat(new JpaSort(DESC, User_.firstname, User_.lastname),
				hasItems(new Order(DESC, "firstname"), new Order(Direction.DESC, "lastname")));
	}

	@Test // DATAJPA-12
	public void combiningSortByMultipleProperties() {

		assertThat(new JpaSort(User_.firstname).and(new JpaSort(User_.lastname)),
				hasItems(new Order("firstname"), new Order("lastname")));
	}

	@Test // DATAJPA-12
	public void combiningSortByMultiplePropertiesWithDifferentSort() {

		assertThat(new JpaSort(User_.firstname).and(new JpaSort(DESC, User_.lastname)),
				hasItems(new Order("firstname"), new Order(DESC, "lastname")));
	}

	@Test // DATAJPA-12
	public void combiningSortByNestedEmbeddedProperty() {
		assertThat(new JpaSort(path(User_.address).dot(Address_.streetName)), hasItems(new Order("address.streetName")));
	}

	@Test // DATAJPA-12
	public void buildJpaSortFromJpaMetaModelSingleAttribute() {

		assertThat(new JpaSort(ASC, path(User_.firstname)), //
				hasItems(new Order("firstname")));
	}

	@Test // DATAJPA-12
	public void buildJpaSortFromJpaMetaModelNestedAttribute() {

		assertThat(new JpaSort(ASC, path(MailMessage_.mailSender).dot(MailSender_.name)), //
				hasItems(new Order("mailSender.name")));
	}

	@Test // DATAJPA-702
	public void combiningSortByMultiplePropertiesWithDifferentSortUsingSimpleAnd() {

		assertThat(new JpaSort(User_.firstname).and(DESC, User_.lastname),
				contains(new Order("firstname"), new Order(DESC, "lastname")));
	}

	@Test // DATAJPA-702
	public void combiningSortByMultiplePathsWithDifferentSortUsingSimpleAnd() {

		assertThat(new JpaSort(User_.firstname).and(DESC, path(MailMessage_.mailSender).dot(MailSender_.name)),
				contains(new Order("firstname"), new Order(DESC, "mailSender.name")));
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-702
	public void rejectsNullAttributesForCombiningCriterias() {
		new JpaSort(User_.firstname).and(DESC, (Attribute<?, ?>[]) null);
	}

	@Test(expected = IllegalArgumentException.class) // DATAJPA-702
	public void rejectsNullPathsForCombiningCriterias() {
		new JpaSort(User_.firstname).and(DESC, (Path<?, ?>[]) null);
	}

	@Test // DATAJPA-702
	public void buildsUpPathForPluralAttributesCorrectly() {

		// assertThat(new JpaSort(JpaSort.path(User_.colleagues).dot(User_.roles).dot(Role_.name)), //
		// hasItem(new Order(ASC, "colleagues.roles.name")));
	}

	@Test // DATAJPA-965
	public void createsUnsafeSortCorrectly() {

		JpaSort sort = JpaSort.unsafe(DESC, "foo.bar");

		assertThat(sort, hasItem(new Order(DESC, "foo.bar")));
		assertThat(sort.getOrderFor("foo.bar"), is(instanceOf(JpaOrder.class)));
	}

	@Test // DATAJPA-965
	public void createsUnsafeSortWithMultiplePropertiesCorrectly() {

		JpaSort sort = JpaSort.unsafe(DESC, "foo.bar", "spring.data");

		assertThat(sort, hasItems(new Order(DESC, "foo.bar"), new Order(DESC, "spring.data")));
		assertThat(sort.getOrderFor("foo.bar"), is(instanceOf(JpaOrder.class)));
		assertThat(sort.getOrderFor("spring.data"), is(instanceOf(JpaOrder.class)));
	}

	@Test // DATAJPA-965
	public void combinesSafeAndUnsafeSortCorrectly() {

		// JpaSort sort = new JpaSort(path(User_.colleagues).dot(User_.roles).dot(Role_.name)).andUnsafe(DESC, "foo.bar");
		//
		// assertThat(sort, hasItems(new Order(ASC, "colleagues.roles.name"), new Order(DESC, "foo.bar")));
		// assertThat(sort.getOrderFor("colleagues.roles.name"), is(not(instanceOf(JpaOrder.class))));
		// assertThat(sort.getOrderFor("foo.bar"), is(instanceOf(JpaOrder.class)));
	}

	@Test // DATAJPA-965
	public void combinesUnsafeAndSafeSortCorrectly() {

		// Sort sort = JpaSort.unsafe(DESC, "foo.bar").and(ASC, path(User_.colleagues).dot(User_.roles).dot(Role_.name));
		//
		// assertThat(sort, hasItems(new Order(ASC, "colleagues.roles.name"), new Order(DESC, "foo.bar")));
		// assertThat(sort.getOrderFor("colleagues.roles.name"), is(not(instanceOf(JpaOrder.class))));
		// assertThat(sort.getOrderFor("foo.bar"), is(instanceOf(JpaOrder.class)));
	}
}
