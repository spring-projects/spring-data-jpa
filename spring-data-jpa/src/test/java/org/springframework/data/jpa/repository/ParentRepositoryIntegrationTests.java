/*
 * Copyright 2013-2023 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.sample.Child;
import org.springframework.data.jpa.domain.sample.Parent;
import org.springframework.data.jpa.repository.sample.ParentRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

/**
 * @author Jens Schauder
 * @author Krzysztof Krason
 */
@Transactional
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:config/namespace-application-context.xml")
class ParentRepositoryIntegrationTests {

	@Autowired ParentRepository repository;

	@BeforeEach
	void setUp() {

		repository.save(new Parent().add(new Child()));
		repository.save(new Parent().add(new Child()).add(new Child()));
		repository.save(new Parent().add(new Child()));
		repository.save(new Parent());
		repository.flush();
	}

	@Test // DATAJPA-287
	void testWithoutJoin() {

		Page<Parent> page = repository.findAll(new Specification<Parent>() {
			@Override
			public Predicate toPredicate(Root<Parent> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				Path<Set<Child>> childrenPath = root.get("children");
				query.distinct(true);
				return cb.isNotEmpty(childrenPath);
			}
		}, PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "id")));

		List<Parent> content = page.getContent();

		assertThat(content).hasSize(3);
		assertThat(page.getSize()).isEqualTo(5);
		assertThat(page.getNumber()).isZero();
		assertThat(page.getTotalElements()).isEqualTo(3L);
		assertThat(page.getTotalPages()).isOne();
	}

	@Test // DATAJPA-287
	void testWithJoin() {

		Page<Parent> page = repository.findAll(new Specification<Parent>() {
			@Override
			public Predicate toPredicate(Root<Parent> root, CriteriaQuery<?> query, CriteriaBuilder cb) {
				root.join("children");
				// we are interesting in distinct items, especially when join presents in query
				query.distinct(true);
				return cb.isNotEmpty(root.<Set<Child>> get("children"));
			}
		}, PageRequest.of(0, 5, Sort.by(Sort.Direction.ASC, "id")));

		List<Parent> content = page.getContent();

		// according to the initial setup there should be
		// 3 parents which children collection is not empty
		assertThat(content).hasSize(3);
		assertThat(page.getSize()).isEqualTo(5);
		assertThat(page.getNumber()).isZero();

		// we get here wrong total elements number since
		// count query doesn't take into account the distinct marker of query
		assertThat(page.getTotalElements()).isEqualTo(3L);
		assertThat(page.getTotalPages()).isOne();
	}
}
