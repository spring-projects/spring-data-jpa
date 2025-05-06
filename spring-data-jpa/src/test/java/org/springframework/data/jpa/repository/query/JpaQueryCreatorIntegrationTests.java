/*
 * Copyright 2017-2024 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import org.hibernate.query.spi.SqmQuery;
import org.hibernate.query.sqm.tree.from.SqmRoot;
import org.hibernate.query.sqm.tree.select.SqmQuerySpec;
import org.hibernate.query.sqm.tree.select.SqmSelectStatement;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.data.jpa.domain.sample.User;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.projection.SpelAwareProxyProjectionFactory;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.AbstractRepositoryMetadata;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.lang.reflect.Method;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration tests for {@link JpaQueryCreator}.
 *
 * @author Yanming Zhou
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration("classpath:infrastructure.xml")
class JpaQueryCreatorIntegrationTests {

	@PersistenceContext
	EntityManager entityManager;

	@Test // GH-3349
	void implicitJoin() throws Exception {

		Method method = SomeRepository.class.getMethod("findByManagerId", Integer.class);

		PersistenceProvider provider = PersistenceProvider.fromEntityManager(entityManager);
		JpaQueryMethod queryMethod = new JpaQueryMethod(method,
				AbstractRepositoryMetadata.getMetadata(SomeRepository.class), new SpelAwareProxyProjectionFactory(), provider);

		PartTree tree = new PartTree("findByManagerId", User.class);
		ParameterMetadataProvider metadataProvider = new ParameterMetadataProvider(entityManager.getCriteriaBuilder(),
				queryMethod.getParameters(), EscapeCharacter.DEFAULT);

		JpaQueryCreator creator = new JpaQueryCreator(tree, queryMethod.getResultProcessor().getReturnedType(),
				entityManager.getCriteriaBuilder(), metadataProvider);

		TypedQuery<?> query = entityManager.createQuery(creator.createQuery());
		SqmQuery sqmQuery = ((SqmQuery) query);
		SqmSelectStatement<?> statement = (SqmSelectStatement<?>) sqmQuery.getSqmStatement();
		SqmQuerySpec<?> spec = (SqmQuerySpec<?>) statement.getQueryPart();
		SqmRoot<?> root = spec.getFromClause().getRoots().get(0);

		assertThat(root.getJoins()).isEmpty();
	}

	interface SomeRepository extends Repository<User, Integer> {
		List<User> findByManagerId(Integer managerId);
	}
}
