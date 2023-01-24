/*
 * Copyright 2011-2023 the original author or authors.
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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.ParameterExpression;
import jakarta.persistence.criteria.Root;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:application-context.xml"
// , "classpath:eclipselink.xml"
// , "classpath:openjpa.xml"
})
@Transactional
class SimpleJpaParameterBindingTests {

	@PersistenceContext EntityManager em;

	@Test
	@Disabled
	void bindArray() {

		User user = new User("Dave", "Matthews", "foo@bar.de");
		em.persist(user);
		em.flush();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<User> criteria = builder.createQuery(User.class);
		Root<User> root = criteria.from(User.class);
		ParameterExpression<String[]> parameter = builder.parameter(String[].class);
		criteria.where(root.get("firstname").in(parameter));

		TypedQuery<User> query = em.createQuery(criteria);
		query.setParameter(parameter, new String[] { "Dave", "Carter" });

		List<User> result = query.getResultList();
		assertThat(result).isNotEmpty();
	}

	@Test
	@SuppressWarnings("rawtypes")
	void bindCollection() {

		User user = new User("Dave", "Matthews", "foo@bar.de");
		em.persist(user);
		em.flush();

		CriteriaBuilder builder = em.getCriteriaBuilder();

		CriteriaQuery<User> criteria = builder.createQuery(User.class);
		Root<User> root = criteria.from(User.class);
		ParameterExpression<Collection> parameter = builder.parameter(Collection.class);
		criteria.where(root.get("firstname").in(parameter));

		TypedQuery<User> query = em.createQuery(criteria);

		query.setParameter(parameter, Arrays.asList("Dave"));

		List<User> result = query.getResultList();
		assertThat(result).isNotEmpty();
		assertThat(result.get(0)).isEqualTo(user);
	}
}
