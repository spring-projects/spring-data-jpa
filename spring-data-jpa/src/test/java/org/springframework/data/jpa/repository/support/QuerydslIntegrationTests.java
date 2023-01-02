/*
 * Copyright 2014-2023 the original author or authors.
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

import java.util.stream.Stream;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;

/**
 * Integration tests for {@link Querydsl}.
 *
 * @author Thomas Darimont
 * @author Jens Schauder
 * @author Marcus Voltolim
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
class QuerydslIntegrationTests {

	@PersistenceContext EntityManager em;

	private Querydsl querydsl;
	private PathBuilder<User> userPath;
	private JPQLQuery<User> userQuery;

	@BeforeEach
	void setup() {

		userPath = new PathBuilder<>(User.class, "user");
		querydsl = new Querydsl(em, userPath);
		userQuery = querydsl.createQuery().select(userPath);
	}

	@Test // DATAJPA-499
	void defaultOrderingShouldNotGenerateAnNullOrderingHint() {

		JPQLQuery<User> result = querydsl.applySorting(Sort.by("firstname"), userQuery);

		assertThat(result).isNotNull();
		assertThat(result.toString()) //
				.doesNotContain("nulls first") //
				.doesNotContain("nulls last");
	}

	@Test // DATAJPA-1779
	void orderWithIgnoreCaseAddLowerOnlyStringType() {

		// firstname (String); id (Integer); dateOfBirth (Date)
		Sort.Order[] orders = Stream.of("firstname", "id", "dateOfBirth").map(name -> Sort.Order.asc(name).ignoreCase()).toArray(Sort.Order[]::new);
		JPQLQuery<User> result = querydsl.applySorting(Sort.by(orders), userQuery);

		assertThat(result).isNotNull();
		assertThat(result.toString()) //
				.startsWith("select user") //
				.endsWith("order by lower(user.firstname) asc, user.id asc, user.dateOfBirth asc");
	}

}
