/*
 * Copyright 2025 the original author or authors.
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

import static org.springframework.data.querydsl.QuerydslUtils.*;

import jakarta.persistence.EntityManager;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragment;
import org.springframework.data.repository.core.support.RepositoryFragmentsContributor;

/**
 * JPA-specific {@link RepositoryFragmentsContributor} contributing Querydsl fragments if a repository implements
 * {@link QuerydslPredicateExecutor}.
 *
 * @author Mark Paluch
 * @since 4.0
 * @see QuerydslJpaPredicateExecutor
 */
enum QuerydslContributor implements JpaRepositoryFragmentsContributor {

	INSTANCE;

	@Override
	public RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
			JpaEntityInformation<?, ?> entityInformation, EntityManager entityManager, EntityPathResolver resolver) {

		if (isQuerydslRepository(metadata)) {

			if (metadata.isReactiveRepository()) {
				throw new InvalidDataAccessApiUsageException(
						"Cannot combine Querydsl and reactive repository support in a single interface");
			}

			QuerydslJpaPredicateExecutor<?> executor = new QuerydslJpaPredicateExecutor<>(entityInformation, entityManager,
					resolver, null);

			return RepositoryComposition.RepositoryFragments
					.of(RepositoryFragment.implemented(QuerydslPredicateExecutor.class, executor));
		}

		return RepositoryComposition.RepositoryFragments.empty();
	}

	@Override
	public RepositoryComposition.RepositoryFragments describe(RepositoryMetadata metadata) {

		if (isQuerydslRepository(metadata)) {
			return RepositoryComposition.RepositoryFragments
					.of(RepositoryFragment.structural(QuerydslPredicateExecutor.class, QuerydslJpaPredicateExecutor.class));
		}

		return RepositoryComposition.RepositoryFragments.empty();
	}

	private static boolean isQuerydslRepository(RepositoryMetadata metadata) {
		return QUERY_DSL_PRESENT && QuerydslPredicateExecutor.class.isAssignableFrom(metadata.getRepositoryInterface());
	}

}
