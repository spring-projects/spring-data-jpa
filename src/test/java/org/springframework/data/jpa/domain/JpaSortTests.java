/*
 * Copyright 2013 the original author or authors.
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

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.springframework.data.jpa.support.JpaMetaModelPathBuilder.*;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Path;
import javax.persistence.criteria.Root;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.jpa.domain.sample.MailMessage_;
import org.springframework.data.jpa.domain.sample.MailSender_;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.domain.sample.User_;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * Unit test for {@link JpaSort}.
 * 
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JpaSortTests {

	@Configuration
	@ImportResource("classpath:infrastructure.xml")
	static class Config {}

	@PersistenceContext EntityManager em;

	private static final MailMessage_ jmail = null;
	private static final MailSender_ jsender = null;

	/**
	 * @see DATAJPA-12
	 */
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIfNoOrderSpecifiersAreGiven() {
		new JpaSort();
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test(expected = IllegalArgumentException.class)
	public void shouldThrowIfNullIsGiven() {
		new JpaSort((List<Path<?>>) null);
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void sortBySinglePropertyWithDefaultSortDirection() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> q = cb.createQuery(User.class);
		Root<User> c = q.from(User.class);

		JpaSort sort = new JpaSort(c.get("firstname"));

		assertThat(sort, hasItems(new Sort.Order("firstname")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void sortByMultiplePropertiesWithDefaultSortDirection() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> q = cb.createQuery(User.class);
		Root<User> c = q.from(User.class);

		JpaSort sort = new JpaSort(c.get("firstname"), c.get("lastname"));
		assertThat(sort, hasItems(new Sort.Order("firstname"), new Sort.Order("lastname")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void sortByMultiplePropertiesWithDescSortDirection() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> q = cb.createQuery(User.class);
		Root<User> c = q.from(User.class);

		JpaSort sort = new JpaSort(Direction.DESC, c.get("firstname"), c.get("lastname"));

		assertThat(sort, hasItems(new Sort.Order(Direction.DESC, "firstname"), new Sort.Order(Direction.DESC, "lastname")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void combiningSortByMultipleProperties() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> q = cb.createQuery(User.class);
		Root<User> c = q.from(User.class);

		Sort sort = new JpaSort(c.get("firstname")).and(new JpaSort(c.get("lastname")));

		assertThat(sort, hasItems(new Sort.Order("firstname"), new Sort.Order("lastname")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void combiningSortByMultiplePropertiesWithDifferentSort() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> q = cb.createQuery(User.class);
		Root<User> c = q.from(User.class);

		Sort sort = new JpaSort(c.get("firstname")).and(new JpaSort(Direction.DESC, c.get("lastname")));

		assertThat(sort, hasItems(new Sort.Order("firstname"), new Sort.Order(Direction.DESC, "lastname")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void combiningSortByNestedEmbeddedProperty() {

		CriteriaBuilder cb = em.getCriteriaBuilder();
		CriteriaQuery<User> q = cb.createQuery(User.class);
		Root<User> c = q.from(User.class);

		Sort sort = new JpaSort(c.get("address").get("streetName"));

		assertThat(sort, hasItems(new Sort.Order("address.streetName")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void buildJpaSortFromJpaMetaModelSingleAttribute() {

		Sort sort = new JpaSort(Direction.ASC, path(User_.firstname).build(em));

		assertThat(sort, hasItems(new Sort.Order("firstname")));
	}

	/**
	 * @see DATAJPA-12
	 */
	@Test
	public void buildJpaSortFromJpaMetaModelNestedAttribute() {

		Sort sort = new JpaSort(Direction.ASC, path(MailMessage_.mailSender).get(MailSender_.name).build(em));

		assertThat(sort, hasItems(new Sort.Order("mailSender.name")));
	}
}
