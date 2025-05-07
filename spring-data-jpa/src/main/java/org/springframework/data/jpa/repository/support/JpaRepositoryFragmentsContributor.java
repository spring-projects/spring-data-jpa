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

import jakarta.persistence.EntityManager;

import org.springframework.data.querydsl.EntityPathResolver;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition;
import org.springframework.data.repository.core.support.RepositoryFragmentsContributor;
import org.springframework.util.Assert;

import com.querydsl.core.types.EntityPath;

/**
 * JPA-specific {@link RepositoryFragmentsContributor} contributing fragments based on the repository.
 * <p>
 * Implementations must define a no-args constructor.
 * <p>
 * Contributed fragments may implement the {@link JpaRepositoryConfigurationAware} interface to access configuration
 * settings.
 *
 * @author Mark Paluch
 * @since 4.0
 */
public interface JpaRepositoryFragmentsContributor extends RepositoryFragmentsContributor {

	JpaRepositoryFragmentsContributor DEFAULT = QuerydslContributor.INSTANCE;

	/**
	 * Returns a composed {@code JpaRepositoryFragmentsContributor} that first applies this contributor to its inputs, and
	 * then applies the {@code after} contributor concatenating effectively both results. If evaluation of either
	 * contributors throws an exception, it is relayed to the caller of the composed contributor.
	 *
	 * @param after the contributor to apply after this contributor is applied.
	 * @return a composed contributor that first applies this contributor and then applies the {@code after} contributor.
	 */
	default JpaRepositoryFragmentsContributor andThen(JpaRepositoryFragmentsContributor after) {

		Assert.notNull(after, "JpaRepositoryFragmentsContributor must not be null");

		return new JpaRepositoryFragmentsContributor() {

			@Override
			public RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
					JpaEntityInformation<?, ?> entityInformation, EntityManager entityManager, EntityPathResolver resolver) {
				return JpaRepositoryFragmentsContributor.this.contribute(metadata, entityInformation, entityManager, resolver)
						.append(after.contribute(metadata, entityInformation, entityManager, resolver));
			}

			@Override
			public RepositoryComposition.RepositoryFragments describe(RepositoryMetadata metadata) {
				return JpaRepositoryFragmentsContributor.this.describe(metadata).append(after.describe(metadata));
			}
		};
	}

	/**
	 * Creates {@link RepositoryComposition.RepositoryFragments} based on {@link RepositoryMetadata} to add JPA-specific
	 * extensions. Typically, adds a {@link QuerydslJpaPredicateExecutor} if the repository interface uses Querydsl.
	 *
	 * @param metadata repository metadata.
	 * @param entityInformation must not be {@literal null}.
	 * @param entityManager the entity manager.
	 * @param resolver resolver to translate a plain domain class into a {@link EntityPath}.
	 * @return {@link RepositoryComposition.RepositoryFragments} to be added to the repository.
	 */
	RepositoryComposition.RepositoryFragments contribute(RepositoryMetadata metadata,
			JpaEntityInformation<?, ?> entityInformation, EntityManager entityManager, EntityPathResolver resolver);

}
