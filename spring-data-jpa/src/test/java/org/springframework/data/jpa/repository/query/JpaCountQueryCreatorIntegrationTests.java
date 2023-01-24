/*
 * Copyright 2017-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;

import java.lang.reflect.Method;
import java.util.List;

import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.tree.expression.SqmDistinct;
import org.hibernate.query.sqm.tree.expression.SqmFunction;
import org.hibernate.query.sqm.tree.select.SqmSelectClause;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.Role;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

/**
 * Integration tests for {@link JpaCountQueryCreator}.
 *
 * @author Oliver Gierke
 * @author Jens Schauder
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class JpaCountQueryCreatorIntegrationTests {

	@PersistenceContext EntityManager entityManager;

	@Test // DATAJPA-1044
	void distinctFlagOnCountQueryIssuesCountDistinct() throws Exception {

		Method method = SomeRepository.class.getMethod("findDistinctByRolesIn", List.class);

		PersistenceProvider provider = PersistenceProvider.fromEntityManager(entityManager);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method,
				AbstractRepositoryMetadata.getMetadata(SomeRepository.class), new SpelAwareProxyProjectionFactory(), provider);

		PartTree tree = new PartTree("findDistinctByRolesIn", User.class);
		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(entityManager.getCriteriaBuilder(),
				queryMethod.getParameters(), EscapeCharacter.DEFAULT);

		JpaCountQueryCreator creator = new JpaCountQueryCreator(tree, queryMethod.getResultProcessor().getReturnedType(),
				entityManager.getCriteriaBuilder(), metadataProvider);

		TypedQuery<? extends Object> query = entityManager.createQuery(creator.createQuery());

		SqmQuery sqmQuery = ((SqmQuery) query);
		SqmSelectStatement<?> select = (SqmSelectStatement<?>) sqmQuery.getSqmStatement();

		// Verify distinct (should this even be there for a count query?)
		SqmSelectClause clause = select.getQuerySpec().getSelectClause();
		assertThat(clause.isDistinct()).isTrue();

		// Verify count(distinct(…))
		SqmFunction<?> function = ((SqmFunction<?>) clause.getSelectionItems().get(0));
		assertThat(function.getFunctionName()).isEqualTo("count");
		assertThat(function.getArguments().get(0)).isInstanceOf(SqmDistinct.class);
	}

	interface SomeRepository extends Repository<User, Integer> {
		List<User> findDistinctByRolesIn(List<Role> roles);
	}
}
