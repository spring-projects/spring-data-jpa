/*
 * Copyright 2014-2019 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.querydsl.core.types.dsl.PathBuilder;
import com.querydsl.jpa.JPQLQuery;

/**
 * Integration tests for {@link Querydsl}.
 *
 * @author Thomas Darimont
 * @author Jens Schauder
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QuerydslIntegrationTests {

	@PersistenceContext EntityManager em;

	Querydsl querydsl;
	PathBuilder<User> userPath;
	JPQLQuery<User> userQuery;

	@Before
	public void setup() {

		userPath = new PathBuilder<User>(User.class, "user");
		querydsl = new Querydsl(em, userPath);
		userQuery = querydsl.createQuery().select(userPath);
	}

	@Test // DATAJPA-499
	public void defaultOrderingShouldNotGenerateAnNullOrderingHint() {

		JPQLQuery<User> result = querydsl.applySorting(Sort.by("firstname"), userQuery);

		assertThat(result).isNotNull();
		assertThat(result.toString()) //
				.doesNotContain("nulls first") //
				.doesNotContain("nulls last");
	}
}
