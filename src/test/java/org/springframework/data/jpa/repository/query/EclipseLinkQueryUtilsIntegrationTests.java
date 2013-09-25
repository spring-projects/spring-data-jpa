/*
 * Copyright 2013 the original author or authors.
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
package org.springframework.data.jpa.repository.query;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.Set;

import javax.persistence.criteria.Join;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.springframework.data.jpa.domain.sample.Order;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Oliver Gierke
 */
@ContextConfiguration("classpath:eclipselink.xml")
public class EclipseLinkQueryUtilsIntegrationTests extends QueryUtilsIntegrationTests {

	/**
	 * Required as EclipseLink generates an inner join for plain association traversal.
	 */
	@Override
	protected void assertNoJoinRequestedForOptionalAssociation(Root<Order> root) {

		Set<Join<Order, ?>> joins = root.getJoins();
		assertThat(joins, hasSize(1));

		Join<Order, ?> join = joins.iterator().next();
		assertThat(join.getAttribute().getName(), is("manager"));
		assertThat(join.getJoinType(), is(JoinType.INNER));
	}
}
