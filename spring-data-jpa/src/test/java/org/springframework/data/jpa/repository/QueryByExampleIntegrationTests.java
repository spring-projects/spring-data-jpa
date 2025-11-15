/*
 * Copyright 2008-present the original author or authors.
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
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.ExampleMatcher;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.data.jpa.repository.sample.UserRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @since 3.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:hibernate-infrastructure.xml",
		"classpath:config/namespace-application-context.xml" })
@Transactional
class QueryByExampleIntegrationTests {

	@Autowired RoleRepository roleRepository;
	@Autowired UserRepository userRepository;
	@Autowired EntityManager em;

	private Role drummer;
	private Role guitarist;
	private Role singer;

	@BeforeEach
	void setUp() {

		drummer = roleRepository.save(new Role("drummer"));
		guitarist = roleRepository.save(new Role("guitarist"));
		singer = roleRepository.save(new Role("singer"));
	}

	@AfterEach
	void clearUp() {
		roleRepository.deleteAll();
	}

	@Test // GH-2283
	void queryByExampleWithNoPredicatesShouldHaveNoWhereClause() {

		// given
		Role probe = new Role();
		Example<Role> example = Example.of(probe);

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<Role> query = builder.createQuery(Role.class);
		Root<Role> root = query.from(Role.class);

		// when
		Predicate predicate = QueryByExamplePredicateBuilder.getPredicate(root, builder, example);

		// then
		assertThat(predicate).isNull();
		assertThat(roleRepository.findAll(example)).containsExactlyInAnyOrder(drummer, guitarist, singer);
	}

	@Test // GH-3763
	void usesAnyMatchOnJoins() {

		User manager = new User("mighty", "super user", "msu@u.io");

		userRepository.save(manager);

		User dave = new User();
		dave.setFirstname("dave");
		dave.setLastname("matthews");
		dave.setEmailAddress("d@dmb.com");
		dave.addRole(singer);

		User carter = new User();
		carter.setFirstname("carter");
		carter.setLastname("beaufort");
		carter.setEmailAddress("c@dmb.com");
		carter.addRole(drummer);
		carter.addRole(singer);
		carter.setManager(manager);

		userRepository.saveAllAndFlush(List.of(dave, carter));

		User probe = new User();
		probe.setLastname(dave.getLastname());
		probe.setManager(manager);

		Example<User> example = Example.of(probe,
				ExampleMatcher.matchingAny().withIgnorePaths("id", "createdAt", "age", "active", "emailAddress",
						"secondaryEmailAddress", "colleagues", "address", "binaryData", "attributes", "dateOfBirth"));
		assertThat(userRepository.findAll(example)).containsExactly(dave, carter);
	}
}
