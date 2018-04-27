/*
 * Copyright 2011-2018 the original author or authors.
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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link QuerydslRepositorySupport}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QuerydslRepositorySupportTests {

	@PersistenceContext EntityManager em;

	UserRepository repository;
	User dave, carter;

	@Before
	public void setup() {

		dave = new User("Dave", "Matthews", "dave@matthews.com");
		em.persist(dave);

		carter = new User("Carter", "Beauford", "carter@beauford.com");
		em.persist(carter);

		UserRepositoryImpl repository = new UserRepositoryImpl();
		repository.setEntityManager(em);
		repository.validate();

		this.repository = repository;
	}

	@Test
	public void readsUsersCorrectly() throws Exception {

		List<User> result = repository.findUsersByLastname("Matthews");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(dave));

		result = repository.findUsersByLastname("Beauford");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(carter));
	}

	@Test
	public void updatesUsersCorrectly() throws Exception {

		long updates = repository.updateLastnamesTo("Foo");
		assertThat(updates, is(2L));

		List<User> result = repository.findUsersByLastname("Matthews");
		assertThat(result.size(), is(0));

		result = repository.findUsersByLastname("Beauford");
		assertThat(result.size(), is(0));

		result = repository.findUsersByLastname("Foo");
		assertThat(result.size(), is(2));
		assertThat(result, hasItems(dave, carter));
	}

	@Test
	public void deletesAllWithLastnameCorrectly() throws Exception {

		long updates = repository.deleteAllWithLastname("Matthews");
		assertThat(updates, is(1L));

		List<User> result = repository.findUsersByLastname("Matthews");
		assertThat(result.size(), is(0));

		result = repository.findUsersByLastname("Beauford");
		assertThat(result.size(), is(1));
		assertThat(result.get(0), is(carter));
	}

	@Test(expected = IllegalArgumentException.class)
	public void rejectsUnsetEntityManager() throws Exception {

		UserRepositoryImpl repositoryImpl = new UserRepositoryImpl();
		repositoryImpl.validate();
	}

	interface UserRepository {

		List<User> findUsersByLastname(String firstname);

		long updateLastnamesTo(String lastname);

		long deleteAllWithLastname(String lastname);
	}

	static class UserRepositoryImpl extends QuerydslRepositorySupport implements UserRepository {

		private static final QUser user = QUser.user;

		public UserRepositoryImpl() {
			super(User.class);
		}

		@Override
		@PersistenceContext(unitName = "default")
		public void setEntityManager(EntityManager entityManager) {
			super.setEntityManager(entityManager);
		}

		@Override
		public List<User> findUsersByLastname(String lastname) {
			return from(user).where(user.lastname.eq(lastname)).fetch();
		}

		@Override
		public long updateLastnamesTo(String lastname) {
			return update(user).set(user.lastname, lastname).execute();
		}

		@Override
		public long deleteAllWithLastname(String lastname) {
			return delete(user).where(user.lastname.eq(lastname)).execute();
		}
	}
}
