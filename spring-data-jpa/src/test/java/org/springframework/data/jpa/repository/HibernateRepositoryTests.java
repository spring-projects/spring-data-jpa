/*
 * Copyright 2025-present the original author or authors.
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
import static org.assertj.core.api.Assumptions.*;

import jakarta.persistence.EntityManager;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate-specific repository tests.
 *
 * @author Mark Paluch
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration()
@Transactional
class HibernateRepositoryTests {

	@Autowired UserRepository userRepository;
	@Autowired RoleRepository roleRepository;
	@Autowired CteUserRepository cteUserRepository;
	@Autowired EntityManager em;

	PersistenceProvider provider;
	User dave;
	User carter;
	User oliver;
	Role drummer;
	Role guitarist;
	Role singer;

	@BeforeEach
	void setUp() {
		provider = PersistenceProvider.fromEntityManager(em);

		assumeThat(provider).isEqualTo(PersistenceProvider.HIBERNATE);
		roleRepository.deleteAll();
		userRepository.deleteAll();

		drummer = roleRepository.save(new Role("DRUMMER"));
		guitarist = roleRepository.save(new Role("GUITARIST"));
		singer = roleRepository.save(new Role("SINGER"));

		dave = userRepository.save(new User("Dave", "Matthews", "dave@dmband.com", singer));
		carter = userRepository.save(new User("Carter", "Beauford", "carter@dmband.com", singer, drummer));
		oliver = userRepository.save(new User("Oliver August", "Matthews", "oliver@dmband.com"));
	}

	@Test // GH-3726
	void testQueryWithCTE() {

		Page<UserExcerptDto> result = cteUserRepository.findWithCTE(PageRequest.of(0, 1));
		assertThat(result.getTotalElements()).isEqualTo(3);
	}

	@ImportResource({ "classpath:hibernate-infrastructure.xml" })
	@Configuration
	@EnableJpaRepositories(basePackageClasses = HibernateRepositoryTests.class, considerNestedRepositories = true,
			includeFilters = @ComponentScan.Filter(
					classes = { CteUserRepository.class, UserRepository.class, RoleRepository.class },
					type = FilterType.ASSIGNABLE_TYPE))
	static class TestConfig {}

	interface CteUserRepository extends CrudRepository<User, Integer> {

		/*
		WITH entities AS (
					SELECT
						e.id as id,
						e.number as number
					FROM TestEntity e
				)
		SELECT new com.example.demo.Result('X', c.id, c.number)
		FROM entities c
		*/

		@Query("""
				WITH cte_select AS (select u.firstname as firstname, u.lastname as lastname from User u)
				SELECT new org.springframework.data.jpa.repository.UserExcerptDto(c.firstname, c.lastname)
				FROM cte_select c
				""")
		Page<UserExcerptDto> findWithCTE(Pageable page);

	}

}
