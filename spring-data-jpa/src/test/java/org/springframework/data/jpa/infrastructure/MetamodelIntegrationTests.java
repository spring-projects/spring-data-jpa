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
package org.springframework.data.jpa.infrastructure;

import static org.assertj.core.api.Assertions.*;

import java.util.List;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TupleElement;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable.BindableType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.Metamodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
abstract class MetamodelIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test
	void considersOneToOneAttributeAnAssociation() {

		Metamodel metamodel = em.getMetamodel();
		ManagedType<User> type = metamodel.managedType(User.class);

		Attribute<? super User, ?> attribute = type.getSingularAttribute("manager");
		assertThat(attribute.isAssociation()).isTrue();
	}

	@Test
	void pathToEntityIsOfBindableTypeEntityType() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);

		Root<User> root = query.from(User.class);
		Path<Object> path = root.get("manager");

		assertThat(path.getModel().getBindableType()).isEqualTo(BindableType.ENTITY_TYPE);
	}

	@Test
	void canAccessParametersByIndexForNativeQueries() {

		Query query = em.createNativeQuery("SELECT u from User u where u.lastname = ?1");

		assertThat(query.getParameter(1)).isNotNull();
	}

	@Test
	@Transactional
	void doesNotExposeAliasForTupleIfNoneDefined() {

		User user = new User();

		user.setFirstname("Dave");
		user.setEmailAddress("email");

		em.persist(user);

		TypedQuery<Tuple> query = em.createQuery("SELECT u.firstname from User u", Tuple.class);

		List<Tuple> result = query.getResultList();
		List<TupleElement<?>> elements = result.get(0).getElements();

		assertThat(elements).hasSize(1);
		assertThat(elements.get(0).getAlias()).isNull();
	}

	@Test
	@Transactional
	void returnsAliasesInTuple() {

		User user = new User();
		user.setFirstname("Dave");
		user.setLastname("Matthews");
		user.setEmailAddress("email");

		em.persist(user);

		TypedQuery<Tuple> query = em.createQuery(
				"SELECT u.lastname AS lastname, u.firstname AS firstname FROM User u ORDER BY u.lastname ASC", Tuple.class);

		List<Tuple> resultList = query.getResultList();
		List<TupleElement<?>> elements = resultList.get(0).getElements();

		assertThat(elements).hasSize(2);
		assertThat(elements).extracting(TupleElement::getAlias).contains("firstname", "lastname");
	}
}
