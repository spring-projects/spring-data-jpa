/*
 * Copyright 2013-2014 the original author or authors.
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
package org.springframework.data.jpa.infrastructure;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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

/**
 * @author Oliver Gierke
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
		assertThat(attribute.isAssociation(), is(true));
	}

	@Test
	public void pathToEntityIsOfBindableTypeEntityType() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);

		Root<User> root = query.from(User.class);
		Path<Object> path = root.get("manager");

		assertThat(path.getModel().getBindableType(), is(BindableType.ENTITY_TYPE));
	}

	@Test
	public void canAccessParametersByIndexForNativeQueries() {

		Query query = em.createNativeQuery("SELECT u from User u where u.lastname = ?1");

		assertThat(query.getParameter(1), is(notNullValue()));
	}
}
