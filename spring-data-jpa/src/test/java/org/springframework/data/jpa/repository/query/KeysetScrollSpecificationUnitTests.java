/*
 * Copyright 2023 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.sample.SampleWithIdClass;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests for {@link KeysetScrollSpecification}.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
class KeysetScrollSpecificationUnitTests {

	@PersistenceContext EntityManager em;

	@Test // GH-2996
	void shouldAddIdentifierToSort() {

		Sort sort = KeysetScrollSpecification.createSort(ScrollPosition.keyset(), Sort.by("firstname"),
				new JpaMetamodelEntityInformation<>(User.class, em.getMetamodel(),
						em.getEntityManagerFactory().getPersistenceUnitUtil()));

		assertThat(sort).extracting(Order::getProperty).containsExactly("firstname", "id");
	}

	@Test // GH-2996
	void shouldAddCompositeIdentifierToSort() {

		Sort sort = KeysetScrollSpecification.createSort(ScrollPosition.keyset(), Sort.by("first", "firstname"),
				new JpaMetamodelEntityInformation<>(SampleWithIdClass.class, em.getMetamodel(),
						em.getEntityManagerFactory().getPersistenceUnitUtil()));

		assertThat(sort).extracting(Order::getProperty).containsExactly("first", "firstname", "second");
	}

	@Test // GH-2996
	void shouldSkipExistingIdentifiersInSort() {

		Sort sort = KeysetScrollSpecification.createSort(ScrollPosition.keyset(), Sort.by("id", "firstname"),
				new JpaMetamodelEntityInformation<>(User.class, em.getMetamodel(),
						em.getEntityManagerFactory().getPersistenceUnitUtil()));

		assertThat(sort).extracting(Order::getProperty).containsExactly("id", "firstname");
	}

}
