/*
 * Copyright 2012-2023 the original author or authors.
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
package org.springframework.data.envers.repository.support;

import java.util.Optional;

import jakarta.persistence.EntityManager;

import org.hibernate.envers.DefaultRevisionEntity;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryComposition.RepositoryFragments;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.history.RevisionRepository;
import org.springframework.data.repository.history.support.RevisionEntityInformation;

/**
 * {@link FactoryBean} creating {@link RevisionRepository} instances.
 *
 * @author Oliver Gierke
 * @author Michael Igler
 */
public class EnversRevisionRepositoryFactoryBean<T extends RevisionRepository<S, ID, N>, S, ID, N extends Number & Comparable<N>>
		extends JpaRepositoryFactoryBean<T, S, ID> {

	private Class<?> revisionEntityClass;

	/**
	 * Creates a new {@link EnversRevisionRepositoryFactoryBean} for the given repository interface.
	 *
	 * @param repositoryInterface must not be {@literal null}.
	 */
	public EnversRevisionRepositoryFactoryBean(Class<? extends T> repositoryInterface) {
		super(repositoryInterface);
	}

	/**
	 * Configures the revision entity class. Will default to {@link DefaultRevisionEntity}.
	 *
	 * @param revisionEntityClass
	 */
	public void setRevisionEntityClass(Class<?> revisionEntityClass) {
		this.revisionEntityClass = revisionEntityClass;
	}

	@Override
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new RevisionRepositoryFactory<T, ID, N>(entityManager, revisionEntityClass);
	}

	/**
	 * Repository factory creating {@link RevisionRepository} instances.
	 *
	 * @author Oliver Gierke
	 * @author Jens Schauder
	 */
	private static class RevisionRepositoryFactory<T, ID, N extends Number & Comparable<N>> extends JpaRepositoryFactory {

		private final RevisionEntityInformation revisionEntityInformation;
		private final EntityManager entityManager;

		/**
		 * Creates a new {@link RevisionRepositoryFactory} using the given {@link EntityManager} and revision entity class.
		 *
		 * @param entityManager must not be {@literal null}.
		 * @param revisionEntityClass can be {@literal null}, will default to {@link DefaultRevisionEntity}.
		 */
		public RevisionRepositoryFactory(EntityManager entityManager, Class<?> revisionEntityClass) {

			super(entityManager);

			this.entityManager = entityManager;
			this.revisionEntityInformation = Optional.ofNullable(revisionEntityClass) //
					.filter(it -> !it.equals(DefaultRevisionEntity.class))//
					.<RevisionEntityInformation> map(ReflectionRevisionEntityInformation::new) //
					.orElseGet(DefaultRevisionEntityInformation::new);
		}

		@Override
		protected RepositoryFragments getRepositoryFragments(RepositoryMetadata metadata) {

			Object fragmentImplementation = getTargetRepositoryViaReflection( //
					EnversRevisionRepositoryImpl.class, //
					getEntityInformation(metadata.getDomainType()), //
					revisionEntityInformation, //
					entityManager //
			);

			return RepositoryFragments //
					.just(fragmentImplementation) //
					.append(super.getRepositoryFragments(metadata));
		}
	}
}
