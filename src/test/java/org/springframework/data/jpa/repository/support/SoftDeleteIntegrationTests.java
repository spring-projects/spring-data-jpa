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
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.augment.JpaSoftDeleteQueryAugmentor;
import org.springframework.data.jpa.repository.augment.QueryDslSoftDeleteQueryAugmentor;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.SoftDelete;
import org.springframework.data.repository.SoftDelete.FlagMode;
import org.springframework.data.repository.augment.QueryAugmentor;
import org.springframework.data.repository.augment.QueryContext;
import org.springframework.data.repository.augment.UpdateContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.mysema.query.types.Predicate;

/**
 * @author Oliver Gierke
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration("classpath:infrastructure.xml")
@Transactional
public class SoftDeleteIntegrationTests {

	@PersistenceContext EntityManager em;

	SoftUserRepository softRepository;
	SpecialUserRepository repository;

	@Before
	public void setUp() {

		JpaRepositoryFactory factory = new JpaRepositoryFactory(em);
		JpaSoftDeleteQueryAugmentor jpaAugmentor = new JpaSoftDeleteQueryAugmentor();
		QueryDslSoftDeleteQueryAugmentor queryDslAugmentor = new QueryDslSoftDeleteQueryAugmentor();

		List<QueryAugmentor<? extends QueryContext<?>, ? extends QueryContext<?>, ? extends UpdateContext<?>>> augmentors = //
		new ArrayList<QueryAugmentor<? extends QueryContext<?>, ? extends QueryContext<?>, ? extends UpdateContext<?>>>();
		augmentors.add(jpaAugmentor);
		augmentors.add(queryDslAugmentor);

		factory.setQueryAugmentors(augmentors);

		softRepository = factory.getRepository(SoftUserRepository.class);
		repository = factory.getRepository(SpecialUserRepository.class);
	}

	@Test
	public void basicSaveAndDelete() {

		User user = new User("Foo", "Bar", "foo@bar.de");
		user = softRepository.save(user);

		assertThat(repository.findAll(), hasItem(user));
		assertThat(softRepository.findAll(), hasItem(user));

		softRepository.delete(user);

		assertThat(softRepository.findAll(), is(emptyIterable()));
		assertThat(softRepository.count(), is(0L));
		assertThat(softRepository.findOne(user.getId()), is(nullValue()));

		assertThat(repository.count(), is(1L));
		assertThat(repository.findAll(), hasItem(user));
		assertThat(repository.findOne(user.getId()), is(notNullValue()));

		Predicate predicate = QUser.user.firstname.eq("Foo");
		assertThat(softRepository.findAll(predicate), is(emptyIterable()));
		assertThat(softRepository.count(predicate), is(0L));
		assertThat(softRepository.findOne(predicate), is(nullValue()));

		assertThat(repository.count(predicate), is(1L));
		assertThat(repository.findAll(predicate), hasItem(user));
		assertThat(repository.findOne(predicate), is(notNullValue()));
	}

	@Test
	public void basicSaveAndDeleteWithQueryDslPredicate() {

		User user = new User("Tony", "Stark", "tony@stark.com");
		user = softRepository.save(user);

		assertThat(repository.findAll(), hasItem(user));
		assertThat(softRepository.findAll(), hasItem(user));

		softRepository.delete(user);

		Predicate predicate = QUser.user.firstname.eq("Tony");
		assertThat(softRepository.findAll(predicate), is(emptyIterable()));
		assertThat(softRepository.count(predicate), is(0L));
		assertThat(softRepository.findOne(predicate), is(nullValue()));

		assertThat(repository.count(predicate), is(1L));
		assertThat(repository.findAll(predicate), hasItem(user));
		assertThat(repository.findOne(predicate), is(notNullValue()));
	}

	@SoftDelete(value = "active", flagMode = FlagMode.ACTIVE)
	interface SoftUserRepository extends CrudRepository<User, Integer>, QueryDslPredicateExecutor<User> {

		List<User> findByLastname();
	}

	interface SpecialUserRepository extends CrudRepository<User, Integer>, QueryDslPredicateExecutor<User> {

		List<User> findAll();
	}
}
