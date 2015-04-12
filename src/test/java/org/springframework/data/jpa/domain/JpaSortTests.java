/*
 * Copyright 2013-2015 the original author or authors.
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
import org.springframework.data.jpa.domain.JpaSort.Path;
import org.springframework.data.jpa.domain.sample.Address_;
import org.springframework.data.jpa.domain.sample.MailMessage_;
import org.springframework.data.jpa.domain.sample.MailSender_;
import org.springframework.data.jpa.domain.sample.Role_;
import org.springframework.data.jpa.domain.sample.User_;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Integration tests for {@link JpaSort}. This has to be an integration test due to the design of the statically
 * generated meta-model classes. The properties cannot be referred to statically (quite a surprise, as they're static)
 * but only after they've been enhanced by the persistence provider. This requires an {@link EntityManagerFactory} to be
 * bootstrapped.
 * 
 * @see DATAJPA-12
 * @author Thomas Darimont
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
public class JpaSortTests {

	private static final Attribute<?, ?> NULL_ATTRIBUTE = null;
	private static final Attribute<?, ?>[] EMPTY_ATTRIBUTES = new Attribute<?, ?>[0];

	private static final PluralAttribute<?, ?, ?> NULL_PLURAL_ATTRIBUTE = null;
	private static final PluralAttribute<?, ?, ?>[] EMPTY_PLURAL_ATTRIBUTES = new PluralAttribute<?, ?, ?>[0];

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAttribute() {
		new JpaSort(NULL_ATTRIBUTE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyAttributes() {
		new JpaSort(EMPTY_ATTRIBUTES);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPluralAttribute() {
		new JpaSort(NULL_PLURAL_ATTRIBUTE);
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsEmptyPluralAttributes() {
		new JpaSort(EMPTY_PLURAL_ATTRIBUTES);
	}

	@Test
	public void sortBySinglePropertyWithDefaultSortDirection() {
		assertThat(new JpaSort(path(User_.firstname)), hasItems(new Sort.Order("firstname")));
	}

	@Test
	public void sortByMultiplePropertiesWithDefaultSortDirection() {
		assertThat(new JpaSort(User_.firstname, User_.lastname), hasItems(new Order("firstname"), new Order("lastname")));
	}

	@Test
	public void sortByMultiplePropertiesWithDescSortDirection() {

		assertThat(new JpaSort(DESC, User_.firstname, User_.lastname),
				hasItems(new Order(DESC, "firstname"), new Order(Direction.DESC, "lastname")));
	}

	@Test
	public void combiningSortByMultipleProperties() {

		assertThat(new JpaSort(User_.firstname).and(new JpaSort(User_.lastname)),
				hasItems(new Order("firstname"), new Order("lastname")));
	}

	@Test
	public void combiningSortByMultiplePropertiesWithDifferentSort() {

		assertThat(new JpaSort(User_.firstname).and(new JpaSort(DESC, User_.lastname)),
				hasItems(new Order("firstname"), new Order(DESC, "lastname")));
	}

	@Test
	public void combiningSortByNestedEmbeddedProperty() {
		assertThat(new JpaSort(path(User_.address).dot(Address_.streetName)), hasItems(new Order("address.streetName")));
	}

	@Test
	public void buildJpaSortFromJpaMetaModelSingleAttribute() {

		assertThat(new JpaSort(ASC, path(User_.firstname)), //
				hasItems(new Order("firstname")));
	}

	@Test
	public void buildJpaSortFromJpaMetaModelNestedAttribute() {

		assertThat(new JpaSort(ASC, path(MailMessage_.mailSender).dot(MailSender_.name)), //
				hasItems(new Order("mailSender.name")));
	}

	/**
	 * @see DATAJPA-702
	 */
	@Test
	public void combiningSortByMultiplePropertiesWithDifferentSortUsingSimpleAnd() {

		assertThat(new JpaSort(User_.firstname).and(DESC, User_.lastname),
				contains(new Order("firstname"), new Order(DESC, "lastname")));
	}

	/**
	 * @see DATAJPA-702
	 */
	@Test
	public void combiningSortByMultiplePathsWithDifferentSortUsingSimpleAnd() {

		assertThat(new JpaSort(User_.firstname).and(DESC, path(MailMessage_.mailSender).dot(MailSender_.name)),
				contains(new Order("firstname"), new Order(DESC, "mailSender.name")));
	}

	/**
	 * @see DATAJPA-702
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullAttributesForCombiningCriterias() {
		new JpaSort(User_.firstname).and(DESC, (Attribute<?, ?>[]) null);
	}

	/**
	 * @see DATAJPA-702
	 */
	@Test(expected = IllegalArgumentException.class)
	public void rejectsNullPathsForCombiningCriterias() {
		new JpaSort(User_.firstname).and(DESC, (Path<?, ?>[]) null);
	}

	/**
	 * @see DATAJPA-702
	 */
	@Test
	public void buildsUpPathForPluralAttributesCorrectly() {

		assertThat(new JpaSort(path(User_.colleagues).dot(User_.roles).dot(Role_.name)), //
				hasItem(new Order(ASC, "colleagues.roles.name")));
	}
}
