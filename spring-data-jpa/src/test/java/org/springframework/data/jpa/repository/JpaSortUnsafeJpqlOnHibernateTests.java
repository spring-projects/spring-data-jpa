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
package org.springframework.data.jpa.repository;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.data.jpa.domain.sample.UserSpecifications.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.HidePersistenceProviders;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.sample.SampleEvaluationContextExtension;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Verify that {@link JpaSort#unsafe(String...)} works properly with JPQL on Hibernate.
 *
 * @author Greg Turnquist
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:application-context.xml")
@Transactional
@HidePersistenceProviders({ PersistenceProvider.HIBERNATE, PersistenceProvider.ECLIPSELINK })
class JpaSortUnsafeJpqlOnHibernateTests {

	@PersistenceContext EntityManager em;

	// CUT
	@Autowired UserRepository repository;

	// Test fixture
	private User firstUser;
	private User secondUser;
	private User thirdUser;
	private User fourthUser;
	private Integer id;
	private Role adminRole;

	@BeforeEach
	void setUp() throws Exception {

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);
		Thread.sleep(10);
		thirdUser = new User("Dave", "Matthews", "no@email.com");
		thirdUser.setAge(43);
		fourthUser = new User("kevin", "raymond", "no@gmail.com");
		fourthUser.setAge(31);
		adminRole = new Role("admin");

		SampleEvaluationContextExtension.SampleSecurityContextHolder.clear();
	}

	void flushTestUsers() {

		em.persist(adminRole);

		firstUser = repository.save(firstUser);
		secondUser = repository.save(secondUser);
		thirdUser = repository.save(thirdUser);
		fourthUser = repository.save(fourthUser);

		repository.flush();

		id = firstUser.getId();

		assertThat(id).isNotNull();
		assertThat(secondUser.getId()).isNotNull();
		assertThat(thirdUser.getId()).isNotNull();
		assertThat(fourthUser.getId()).isNotNull();

		assertThat(repository.existsById(id)).isTrue();
		assertThat(repository.existsById(secondUser.getId())).isTrue();
		assertThat(repository.existsById(thirdUser.getId())).isTrue();
		assertThat(repository.existsById(fourthUser.getId())).isTrue();
	}

	@Test // GH-3172
	void unsafeFindAllWithPageRequest() {

		flushTestUsers();

		firstUser.setManager(firstUser);
		secondUser.setManager(firstUser);
		thirdUser.setManager(secondUser);
		fourthUser.setManager(secondUser);

		repository.saveAllAndFlush(List.of(firstUser, secondUser, thirdUser, fourthUser));

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("firstname"))))
				.containsExactly(thirdUser, secondUser, firstUser, fourthUser);

		// Path-based expression
		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("manager.firstname"))))
				.containsExactly(thirdUser, fourthUser, firstUser, secondUser);

		assertThat(repository.findAll( //
				PageRequest.of(0, 4, JpaSort.unsafe("manager.manager.firstname"))))
				.containsExactly(firstUser, secondUser, thirdUser, fourthUser);

		// Compound JpaOrder.unsafe operation
		assertThat(repository.findAll(PageRequest.of(0, 4, JpaSort.unsafe("lastname") //
				.and(JpaSort.unsafe("firstname").descending())))).containsExactly(secondUser, firstUser, thirdUser, fourthUser);

		//
		// Also works with custom Specification
		//

		Specification<User> spec = userHasFirstname("Oliver").or(userHasLastname("Matthews"));

		assertThat(repository.findAll(spec, //
				PageRequest.of(0, 4, JpaSort.unsafe("firstname")))).containsExactly(thirdUser, firstUser);
	}
}
