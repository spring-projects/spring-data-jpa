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
package org.springframework.data.jpa.repository.query;

import static org.assertj.core.api.Assertions.*;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Root;

import java.util.List;

import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.springframework.data.core.PropertyPath;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Transactional;

/**
 * EclipseLink variant of {@link QueryUtilsIntegrationTests}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 * @author Mark Paluch
 */
@ContextConfiguration("classpath:eclipselink.xml")
class EclipseLinkQueryUtilsIntegrationTests extends QueryUtilsIntegrationTests {

	int getNumberOfJoinsAfterCreatingAPath() {
		return 1;
	}

	@Test // GH-2756
	@Override
	void prefersFetchOverJoin() {

		CriteriaBuilder builder = em.getCriteriaBuilder();
		CriteriaQuery<User> query = builder.createQuery(User.class);
		Root<User> from = query.from(User.class);
		from.fetch("manager");
		from.join("manager");

		PropertyPath managerFirstname = PropertyPath.from("manager.firstname", User.class);
		PropertyPath managerLastname = PropertyPath.from("manager.lastname", User.class);

		QueryUtils.toExpressionRecursively(from, managerLastname);
		Path<Object> expr = (Path) QueryUtils.toExpressionRecursively(from, managerFirstname);

		assertThat(expr.getParentPath()).hasFieldOrPropertyWithValue("isFetch", true);
		assertThat(from.getFetches()).hasSize(1);
		assertThat(from.getJoins()).hasSize(1);
	}

	@Test // GH-3349
	@Disabled
	@Override
	void doesNotCreateJoinForRelationshipSimpleId() {
		// eclipse link produces join for path.get(relationship)
	}

	@Test // GH-3349
	@Disabled
	@Override
	void doesNotCreateJoinForRelationshipEmbeddedId() {
		// eclipse link produces join for path.get(relationship)
	}

	@Test // GH-3349
	@Disabled
	@Override
	void doesNotCreateJoinForRelationshipIdClass() {
		// eclipse link produces join for path.get(relationship)
	}

	@Test // GH-3983, GH-2870
	@Disabled("Not supported by EclipseLink")
	@Transactional
	@Override
	void applyAndBindOptimizesIn() {}

	@Test // GH-3983, GH-2870
	@Transactional
	@Override
	void applyAndBindExpandsToPositionalPlaceholders() {

		em.getCriteriaBuilder();
		EJBQueryImpl<?> query = (EJBQueryImpl) QueryUtils.applyAndBind("DELETE FROM User u",
				List.of(new User(), new User()), em.unwrap(null));

		assertThat(query.getDatabaseQuery().getJPQLString()).isEqualTo("DELETE FROM User u where u = ?1 or u = ?2");
	}

}
