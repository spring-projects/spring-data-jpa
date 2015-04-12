/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.jpa.repository.support;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

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

import com.mysema.query.jpa.JPQLQuery;
import com.mysema.query.types.path.PathBuilder;

/**
 * Integration tests for {@link Querydsl}.
 * 
 * @author Thomas Darimont
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration({ "classpath:infrastructure.xml" })
@Transactional
public class QuerydslIntegrationTests {

	@PersistenceContext EntityManager em;

	Querydsl querydsl;
	PathBuilder<User> userPath;
	JPQLQuery userQuery;

	@Before
	public void setup() {

		userPath = new PathBuilder<User>(User.class, "user");
		querydsl = new Querydsl(em, userPath);
		userQuery = querydsl.createQuery().from(userPath);
	}

	/**
	 * @see DATAJPA-499
	 */
	@Test
	public void defaultOrderingShouldNotGenerateAnNullOrderingHint() {

		JPQLQuery result = querydsl.applySorting(new Sort(new Sort.Order("firstname")), userQuery);

		assertThat(result, is(notNullValue()));
		assertThat(result.toString(), is(not(anyOf(containsString("nulls first"), containsString("nulls last")))));
	}
}
