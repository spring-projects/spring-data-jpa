/*
 * Copyright 2013-2019 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.Tuple;
import javax.persistence.TupleElement;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.Attribute;
import javax.persistence.metamodel.Bindable.BindableType;
import javax.persistence.metamodel.ManagedType;
import javax.persistence.metamodel.Metamodel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
public abstract class MetamodelIntegrationTests {

	@PersistenceContext EntityManager em;

	@Test
	public void considersOneToOneAttributeAnAssociation() {

		Metamodel metamodel = em.getMetamodel();
		ManagedType<User> type = metamodel.managedType(User.class);

		Attribute<? super User, ?> attribute = type.getSingularAttribute("manager");
		assertThat(attribute.isAssociation()).isTrue();
	}

	@Test
	public void pathToEntityIsOfBindableTypeEntityType() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);

		Root<User> root = query.from(User.class);
		Path<Object> path = root.get("manager");

		assertThat(path.getModel().getBindableType()).isEqualTo(BindableType.ENTITY_TYPE);
	}

	@Test
	public void canAccessParametersByIndexForNativeQueries() {

		Query query = em.createNativeQuery("SELECT u from User u where u.lastname = ?1");

		assertThat(query.getParameter(1)).isNotNull();
	}

	@Test
	@Transactional
	public void doesNotExposeAliasForTupleIfNoneDefined() {

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
	public void returnsAliasesInTuple() {

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
