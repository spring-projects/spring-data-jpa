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
package org.springframework.data.jpa.repository.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.QUser;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration test for {@link QuerydslRepositorySupport}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
class QuerydslRepositorySupportTests {

	@PersistenceContext EntityManager em;

	private UserRepository repository;
	private User dave;
	private User carter;

	@BeforeEach
	void setup() {

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
	void readsUsersCorrectly() {

		List<User> result = repository.findUsersByLastname("Matthews");
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(dave);

		result = repository.findUsersByLastname("Beauford");
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(carter);
	}

	@Test
	void updatesUsersCorrectly() {

		long updates = repository.updateLastnamesTo("Foo");
		assertThat(updates).isEqualTo(2L);

		List<User> result = repository.findUsersByLastname("Matthews");
		assertThat(result).isEmpty();

		result = repository.findUsersByLastname("Beauford");
		assertThat(result).isEmpty();

		result = repository.findUsersByLastname("Foo");
		assertThat(result).hasSize(2);
		assertThat(result).contains(dave, carter);
	}

	@Test
	void deletesAllWithLastnameCorrectly() {

		long updates = repository.deleteAllWithLastname("Matthews");
		assertThat(updates).isOne();

		List<User> result = repository.findUsersByLastname("Matthews");
		assertThat(result).isEmpty();

		result = repository.findUsersByLastname("Beauford");
		assertThat(result).hasSize(1);
		assertThat(result.get(0)).isEqualTo(carter);
	}

	@Test
	void rejectsUnsetEntityManager() {

		UserRepositoryImpl repositoryImpl = new UserRepositoryImpl();
		assertThatIllegalArgumentException().isThrownBy(repositoryImpl::validate);
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
