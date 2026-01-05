/*
 * Copyright 2013-present the original author or authors.
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

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;

/**
 * Integration tests to execute {@link JpaRepositoryTests} against EclipseLink.
 *
 * @author Oliver Gierke
 * @author Greg Turnquist
 * @author Mark Paluch
 */
@ContextConfiguration("classpath:eclipselink.xml")
class EclipseLinkJpaRepositoryTests extends JpaRepositoryTests {

	@PersistenceContext EntityManager em;

	SimpleJpaRepository<User, Integer> repository;
	User firstUser, secondUser;

	@BeforeEach
	@Override
	void setUp() {

		super.setUp();

		repository = new SimpleJpaRepository<>(User.class, em);

		firstUser = new User("Oliver", "Gierke", "gierke@synyx.de");
		firstUser.setAge(28);
		secondUser = new User("Joachim", "Arrasz", "arrasz@synyx.de");
		secondUser.setAge(35);

		repository.deleteAll();
		repository.saveAllAndFlush(List.of(firstUser, secondUser));
	}

	@Test // GH-3990
	void deleteAllBySimpleIdInBatch() {

		repository.deleteAllByIdInBatch(List.of(firstUser.getId(), secondUser.getId()));

		assertThat(repository.count()).isZero();
	}

	@Test // GH-3990
	void deleteAllInBatch() {

		repository.deleteAllInBatch(List.of(firstUser, secondUser));

		assertThat(repository.count()).isZero();
	}

	@Override
	@Disabled("https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477")
	void deleteAllByIdInBatch() {
		// disabled
	}

	@Override
	@Disabled("https://bugs.eclipse.org/bugs/show_bug.cgi?id=349477")
	void deleteAllByIdInBatchShouldConvertAnIterableToACollection() {
		// disabled
	}

}
