/*
 * Copyright 2024-present the original author or authors.
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

import jakarta.persistence.EntityManager;

import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.sample.SoftDeleteUser;
import org.springframework.data.jpa.repository.sample.SoftDeleteUserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * EclipseLink integration tests for derived query compatibility.
 *
 * @author Spring Data Team
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = { "classpath:config/namespace-application-context-h2.xml", "classpath:eclipselink-h2.xml" })
@Transactional
class EclipseLinkDerivedQueryCompatibilityTests {

	@Autowired SoftDeleteUserRepository repository;
	@Autowired EntityManager em;

	private Long activeId;
	private Long otherId;

	@BeforeEach
	void setUp() {

		SoftDeleteUser active = repository.save(new SoftDeleteUser("user@example.com", 0));
		SoftDeleteUser other = repository.save(new SoftDeleteUser("other@example.com", 0));

		this.activeId = active.getId();
		this.otherId = other.getId();

		em.flush();
		em.clear();
	}

	@AfterEach
	void tearDown() {
		repository.deleteAll();
	}

	@Test
	void existsProjectionShouldWorkWithEclipseLinkHermes() {

		boolean exists = repository.existsByEmailIgnoreCaseAndIdNotAndIsDeletedNot("USER@EXAMPLE.COM", otherId, 1);

		assertThat(exists).isTrue();
	}

	@Test
	void bindsParametersForIsDeletedDerivedQuery() {

		Optional<SoftDeleteUser> found = repository.findByIdAndIsDeleted(activeId, 0);

		assertThat(found).isPresent();
		assertThat(found.get().getId()).isEqualTo(activeId);
	}
}
