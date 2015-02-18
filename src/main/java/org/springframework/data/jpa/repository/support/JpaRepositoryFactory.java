/*
 * Copyright 2008-2014 the original author or authors.
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

import static org.springframework.data.querydsl.QueryDslUtils.*;

import java.io.Serializable;

import javax.persistence.EntityManager;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.util.Assert;

/**
 * JPA specific generic repository factory.
 *
 * @author Oliver Gierke
 */
public class JpaRepositoryFactory extends RepositoryFactorySupport {

	private final EntityManager entityManager;
	private final QueryExtractor extractor;
	private final CrudMethodMetadataPostProcessor lockModePostProcessor;

	/**
	 * Creates a new {@link JpaRepositoryFactory}.
	 *
	 * @param entityManager must not be {@literal null}
	 */
	public JpaRepositoryFactory(EntityManager entityManager) {

		Assert.notNull(entityManager);

		this.entityManager = entityManager;
		this.extractor = PersistenceProvider.fromEntityManager(entityManager);
		this.lockModePostProcessor = CrudMethodMetadataPostProcessor.INSTANCE;

		addRepositoryProxyPostProcessor(lockModePostProcessor);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Object getTargetRepository(RepositoryMetadata metadata) {

		SimpleJpaRepository<?, ?> repository = getTargetRepository(metadata, entityManager);
		repository.setRepositoryMethodMetadata(lockModePostProcessor.getLockMetadataProvider());

		return repository;
	}

  /**
   * Creates a new {@link SimpleJpaRepository} suitable for handling JPA operations
   * on a specified JPA entity.
   *
   * @param entityInformation A {@link JpaEntityInformation} containing information
   *                          on the JPA entity for which the repository is required.
   * @param entityManager A JPA {@link EntityManager} that the repository can use
   *                      to execute JPA commands.
   * @return A {@link SimpleJpaRepository}.
   */
  protected SimpleJpaRepository<?, ?> getDefaultTargetRepository(JpaEntityInformation<?, Serializable> entityInformation, EntityManager entityManager) {
    return new SimpleJpaRepository(entityInformation, entityManager);
  }

  /**
   * Creates a new {@link QueryDslJpaRepository} suitable for handling JPA operations
   * on a specified JPA entity.
   *
   * @param entityInformation A {@link JpaEntityInformation} containing information
   *                          on the JPA entity for which the repository is required.
   * @param entityManager A JPA {@link EntityManager} that the repository can use
   *                      to execute JPA commands.
   * @return A {@link QueryDslJpaRepository}.
   */
  protected QueryDslJpaRepository<?, ?> getQueryDslTargetRepository(JpaEntityInformation<?, Serializable> entityInformation, EntityManager entityManager) {
    return new QueryDslJpaRepository(entityInformation, entityManager);
  }

	/**
	 * Callback to create a {@link JpaRepository} instance with the given {@link EntityManager}
	 *
	 * @param <T>
	 * @param <ID>
	 * @param entityManager
	 * @see #getTargetRepository(RepositoryMetadata)
	 * @return
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <T, ID extends Serializable> SimpleJpaRepository<?, ?> getTargetRepository(RepositoryMetadata metadata,
			EntityManager entityManager) {
		JpaEntityInformation<?, Serializable> entityInformation = getEntityInformation(metadata.getDomainType());

		return isQueryDslExecutor(metadata.getRepositoryInterface())
        ? getQueryDslTargetRepository(entityInformation, entityManager)
        : getDefaultTargetRepository(entityInformation, entityManager);
	}

  /**
   * Gets the base class for all repositories instantiated by this factory.
   *
   * @return {@code SimpleJpaRepository.class}.
   */
  protected Class<?> getDefaultTargetRepositoryClass() {
    return SimpleJpaRepository.class;
  }

  /**
   * Gets the base class for all repositories instantiated by this factory
   * when QueryDSL is enabled.
   *
   * @return {@code QueryDslJpaRepository.class}.
   */
  protected Class<?> getQueryDslTargetRepositoryClass() {
    return QueryDslJpaRepository.class;
  }

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.repository.support.RepositoryFactorySupport#
	 * getRepositoryBaseClass()
	 */
	@Override
	protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
    return isQueryDslExecutor(metadata.getRepositoryInterface())
		  ? getQueryDslTargetRepositoryClass()
		  : getDefaultTargetRepositoryClass();
	}

	/**
	 * Returns whether the given repository interface requires a QueryDsl specific implementation to be chosen.
	 *
	 * @param repositoryInterface
	 * @return
	 */
	private boolean isQueryDslExecutor(Class<?> repositoryInterface) {

		return QUERY_DSL_PRESENT && QueryDslPredicateExecutor.class.isAssignableFrom(repositoryInterface);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getQueryLookupStrategy(org.springframework.data.repository.query.QueryLookupStrategy.Key, org.springframework.data.repository.query.EvaluationContextProvider)
	 */
	@Override
	protected QueryLookupStrategy getQueryLookupStrategy(Key key, EvaluationContextProvider evaluationContextProvider) {
		return JpaQueryLookupStrategy.create(entityManager, key, extractor, evaluationContextProvider);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see
	 * org.springframework.data.repository.support.RepositoryFactorySupport#
	 * getEntityInformation(java.lang.Class)
	 */
	@Override
	@SuppressWarnings("unchecked")
	public <T, ID extends Serializable> JpaEntityInformation<T, ID> getEntityInformation(Class<T> domainClass) {

		return (JpaEntityInformation<T, ID>) JpaEntityInformationSupport.getEntityInformation(domainClass, entityManager);
	}
}
