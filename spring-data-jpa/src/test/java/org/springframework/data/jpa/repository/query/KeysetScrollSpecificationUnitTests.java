/*
 * Copyright 2023-present the original author or authors.
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.sample.SampleWithIdClass;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.HibernateUtils;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Unit tests for {@link KeysetScrollSpecification}.
 *
 * @author Mark Paluch
 * @author Seongho Eom
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:hibernate-infrastructure.xml")
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

	@Test // GH-4303
	void shouldResolveJpaMetamodelAttributeNameForKeysetPredicate() {

		Map<String, Object> keys = Map.of("id", 1L, "isActive", false);
		KeysetScrollPosition position = ScrollPosition.of(keys, ScrollPosition.Direction.FORWARD);
		JpaMetamodelEntityInformation<PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess, Long> entityInformation = new JpaMetamodelEntityInformation<>(
				PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess.class, em.getMetamodel(),
				em.getEntityManagerFactory().getPersistenceUnitUtil());
		KeysetScrollSpecification<PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess> specification = new KeysetScrollSpecification<>(
				position, Sort.by("isActive"), entityInformation);
		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess> query = builder
				.createQuery(PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess.class);
		Root<PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess> root = query
				.from(PartTreeJpaQueryIntegrationTests.IsActivePropertyAccess.class);

		query.where(specification.createPredicate(root, builder));

		assertThat(HibernateUtils.getHibernateQuery(em.createQuery(query).unwrap(org.hibernate.query.Query.class)))
				.contains(".active")
				.doesNotContain(".isActive");
	}

}
