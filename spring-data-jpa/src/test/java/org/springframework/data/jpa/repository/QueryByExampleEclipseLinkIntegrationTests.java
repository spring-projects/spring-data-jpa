/*
 * Copyright 2008-2023 the original author or authors.
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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.convert.QueryByExamplePredicateBuilder;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.repository.sample.RoleRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Greg Turnquist
 * @since 3.0
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:eclipselink.xml", "classpath:config/namespace-application-context.xml" })
@Transactional
class QueryByExampleEclipseLinkIntegrationTests {

	@Autowired RoleRepository repository;
	@Autowired EntityManager em;

	private Role drummer;
	private Role guitarist;
	private Role singer;

	@BeforeEach
	void setUp() {

		drummer = repository.save(new Role("drummer"));
		guitarist = repository.save(new Role("guitarist"));
		singer = repository.save(new Role("singer"));
	}

	@AfterEach
	void clearUp() {
		repository.deleteAll();
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
		assertThat(repository.findAll(example)).containsExactlyInAnyOrder(drummer, guitarist, singer);
	}
}
