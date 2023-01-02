/*
 * Copyright 2013-2023 the original author or authors.
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
package org.springframework.data.jpa.domain;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.domain.Sort.Direction.*;
import static org.springframework.data.jpa.domain.JpaSort.*;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.PluralAttribute;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.JpaSort.*;
import org.springframework.data.jpa.domain.sample.Address_;
import org.springframework.data.jpa.domain.sample.MailMessage_;
import org.springframework.data.jpa.domain.sample.MailSender_;
import org.springframework.data.jpa.domain.sample.User_;
import org.springframework.lang.Nullable;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link JpaSort}. This has to be an integration test due to the design of the statically
 * generated meta-model classes. The properties cannot be referred to statically (quite a surprise, as they're static)
 * but only after they've been enhanced by the persistence provider. This requires an {@link EntityManagerFactory} to be
 * bootstrapped.
 *
 * @author Thomas Darimont
 * @author Oliver Gierke
 * @author Christoph Strobl
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class JpaSortTests {

	private static final @Nullable Attribute<?, ?> NULL_ATTRIBUTE = null;
	private static final Attribute<?, ?>[] EMPTY_ATTRIBUTES = new Attribute<?, ?>[0];

	private static final @Nullable PluralAttribute<?, ?, ?> NULL_PLURAL_ATTRIBUTE = null;
	private static final PluralAttribute<?, ?, ?>[] EMPTY_PLURAL_ATTRIBUTES = new PluralAttribute<?, ?, ?>[0];

	@Test // DATAJPA-12
	void rejectsNullAttribute() {
		assertThatIllegalArgumentException().isThrownBy(() -> of(NULL_ATTRIBUTE));
	}

	@Test // DATAJPA-12
	void rejectsEmptyAttributes() {
		assertThatIllegalArgumentException().isThrownBy(() -> of(EMPTY_ATTRIBUTES));
	}

	@Test // DATAJPA-12
	void rejectsNullPluralAttribute() {
		assertThatIllegalArgumentException().isThrownBy(() -> of(NULL_PLURAL_ATTRIBUTE));
	}

	@Test // DATAJPA-12
	void rejectsEmptyPluralAttributes() {
		assertThatIllegalArgumentException().isThrownBy(() -> of(EMPTY_PLURAL_ATTRIBUTES));
	}

	@Test // DATAJPA-12
	void sortBySinglePropertyWithDefaultSortDirection() {
		assertThat(JpaSort.of(path(User_.firstname))).contains(Order.asc("firstname"));
	}

	@Test // DATAJPA-12
	void sortByMultiplePropertiesWithDefaultSortDirection() {
		assertThat(JpaSort.of(User_.firstname, User_.lastname)).contains(Order.asc("firstname"), Order.asc("lastname"));
	}

	@Test // DATAJPA-12
	void sortByMultiplePropertiesWithDescSortDirection() {

		assertThat(JpaSort.of(DESC, User_.firstname, User_.lastname)).contains(new Order(DESC, "firstname"),
				Order.desc("lastname"));
	}

	@Test // DATAJPA-12
	void combiningSortByMultipleProperties() {

		assertThat(JpaSort.of(User_.firstname).and(JpaSort.of(User_.lastname))).contains(Order.asc("firstname"),
				Order.asc("lastname"));
	}

	@Test // DATAJPA-12
	void combiningSortByMultiplePropertiesWithDifferentSort() {

		assertThat(JpaSort.of(User_.firstname).and(JpaSort.of(DESC, User_.lastname))).contains(Order.asc("firstname"),
				Order.desc("lastname"));
	}

	@Test // DATAJPA-12
	void combiningSortByNestedEmbeddedProperty() {
		assertThat(JpaSort.of(path(User_.address).dot(Address_.streetName))).contains(Order.asc("address.streetName"));
	}

	@Test // DATAJPA-12
	void buildJpaSortFromJpaMetaModelSingleAttribute() {

		assertThat(JpaSort.of(ASC, path(User_.firstname))).contains(Order.asc("firstname"));
	}

	@Test // DATAJPA-12
	void buildJpaSortFromJpaMetaModelNestedAttribute() {

		assertThat(JpaSort.of(ASC, path(MailMessage_.mailSender).dot(MailSender_.name)))
				.contains(Order.asc("mailSender.name"));
	}

	@Test // DATAJPA-702
	void combiningSortByMultiplePropertiesWithDifferentSortUsingSimpleAnd() {

		assertThat(JpaSort.of(User_.firstname).and(DESC, User_.lastname)).containsExactly(Order.asc("firstname"),
				Order.desc("lastname"));
	}

	@Test // DATAJPA-702
	void combiningSortByMultiplePathsWithDifferentSortUsingSimpleAnd() {

		assertThat(JpaSort.of(User_.firstname).and(DESC, path(MailMessage_.mailSender).dot(MailSender_.name)))
				.containsExactly(Order.asc("firstname"), Order.desc("mailSender.name"));
	}

	@Test // DATAJPA-702
	void rejectsNullAttributesForCombiningCriterias() {
		assertThatIllegalArgumentException().isThrownBy(() -> of(User_.firstname).and(DESC, (Attribute<?, ?>[]) null));
	}

	@Test // DATAJPA-702
	void rejectsNullPathsForCombiningCriterias() {
		assertThatIllegalArgumentException().isThrownBy(() -> of(User_.firstname).and(DESC, (Path<?, ?>[]) null));
	}

	@Test // DATAJPA-702
	void buildsUpPathForPluralAttributesCorrectly() {

		// assertThat(JpaSort.of(JpaSort.path(User_.colleagues).dot(User_.roles).dot(Role_.name)), //
		// hasItem(new Order(ASC, "colleagues.roles.name")));
	}

	@Test // DATAJPA-965
	void createsUnsafeSortCorrectly() {

		JpaSort sort = JpaSort.unsafe(DESC, "foo.bar");

		assertThat(sort).contains(Order.desc("foo.bar"));
		assertThat(sort.getOrderFor("foo.bar")).isInstanceOf(JpaOrder.class);
	}

	@Test // DATAJPA-965
	void createsUnsafeSortWithMultiplePropertiesCorrectly() {

		JpaSort sort = JpaSort.unsafe(DESC, "foo.bar", "spring.data");

		assertThat(sort).contains(Order.desc("foo.bar"), Order.desc("spring.data"));
		assertThat(sort.getOrderFor("foo.bar")).isInstanceOf(JpaOrder.class);
		assertThat(sort.getOrderFor("spring.data")).isInstanceOf(JpaOrder.class);
	}

	@Test // DATAJPA-965
	void combinesSafeAndUnsafeSortCorrectly() {

		// JpaSort sort = JpaSort.of(path(User_.colleagues).dot(User_.roles).dot(Role_.name)).andUnsafe(DESC, "foo.bar");
		//
		// assertThat(sort, hasItems(new Order(ASC, "colleagues.roles.name"), new Order(DESC, "foo.bar")));
		// assertThat(sort.getOrderFor("colleagues.roles.name"), is(not(instanceOf(JpaOrder.class))));
		// assertThat(sort.getOrderFor("foo.bar"), is(instanceOf(JpaOrder.class)));
	}

	@Test // DATAJPA-965
	void combinesUnsafeAndSafeSortCorrectly() {

		// Sort sort = JpaSort.unsafe(DESC, "foo.bar").and(ASC, path(User_.colleagues).dot(User_.roles).dot(Role_.name));
		//
		// assertThat(sort, hasItems(new Order(ASC, "colleagues.roles.name"), new Order(DESC, "foo.bar")));
		// assertThat(sort.getOrderFor("colleagues.roles.name"), is(not(instanceOf(JpaOrder.class))));
		// assertThat(sort.getOrderFor("foo.bar"), is(instanceOf(JpaOrder.class)));
	}
}
