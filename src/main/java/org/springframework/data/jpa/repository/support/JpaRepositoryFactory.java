/*
 * Copyright 2008-2018 the original author or authors.
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

import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.Tuple;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.data.jpa.projection.CollectionAwareProjectionFactory;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.provider.QueryExtractor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.query.AbstractJpaQuery;
import org.springframework.data.jpa.repository.query.JpaQueryLookupStrategy;
import org.springframework.data.jpa.repository.query.JpaQueryMethod;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.querydsl.QueryDslPredicateExecutor;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.QueryCreationListener;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.query.EvaluationContextProvider;
import org.springframework.data.repository.query.QueryLookupStrategy;
import org.springframework.data.repository.query.QueryLookupStrategy.Key;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.util.Assert;

/**
 * JPA specific generic repository factory.
 * 
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Jens Schauder
 */
public class JpaRepositoryFactory extends RepositoryFactorySupport {

	private final EntityManager entityManager;
	private final QueryExtractor extractor;
	private final CrudMethodMetadataPostProcessor crudMethodMetadataPostProcessor;

	/**
	 * Creates a new {@link JpaRepositoryFactory}.
	 * 
	 * @param entityManager must not be {@literal null}
	 */
	public JpaRepositoryFactory(EntityManager entityManager) {

		Assert.notNull(entityManager, "EntityManager must not be null!");

		this.entityManager = entityManager;
		this.extractor = PersistenceProvider.fromEntityManager(entityManager);
		this.crudMethodMetadataPostProcessor = new CrudMethodMetadataPostProcessor();

		addRepositoryProxyPostProcessor(crudMethodMetadataPostProcessor);

		if (extractor.equals(PersistenceProvider.ECLIPSELINK)) {
			addQueryCreationListener(new EclipseLinkProjectionQueryCreationListener(entityManager));
		}
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#setBeanClassLoader(java.lang.ClassLoader)
	 */
	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {

		super.setBeanClassLoader(classLoader);
		this.crudMethodMetadataPostProcessor.setBeanClassLoader(classLoader);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getTargetRepository(org.springframework.data.repository.core.RepositoryMetadata)
	 */
	@Override
	protected Object getTargetRepository(RepositoryInformation information) {

		SimpleJpaRepository<?, ?> repository = getTargetRepository(information, entityManager);
		repository.setRepositoryMethodMetadata(crudMethodMetadataPostProcessor.getCrudMethodMetadata());

		return repository;
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
	protected <T, ID extends Serializable> SimpleJpaRepository<?, ?> getTargetRepository(
			RepositoryInformation information, EntityManager entityManager) {

		JpaEntityInformation<?, Serializable> entityInformation = getEntityInformation(information.getDomainType());

		return getTargetRepositoryViaReflection(information, entityInformation, entityManager);
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

		if (isQueryDslExecutor(metadata.getRepositoryInterface())) {
			return QueryDslJpaRepository.class;
		} else {
			return SimpleJpaRepository.class;
		}
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactorySupport#getProjectionFactory(java.lang.ClassLoader, org.springframework.beans.factory.BeanFactory)
	 */
	@Override
	protected ProjectionFactory getProjectionFactory(ClassLoader classLoader, BeanFactory beanFactory) {

		CollectionAwareProjectionFactory factory = new CollectionAwareProjectionFactory();
		factory.setBeanClassLoader(classLoader);
		factory.setBeanFactory(beanFactory);

		return factory;
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

	/**
	 * Query creation listener that informs EclipseLink users that they have to be extra careful when defining repository
	 * query methods using projections as we have to rely on the declaration order of the accessors in projection
	 * interfaces matching the order in columns. Alias-based mapping doesn't work with EclipseLink as it doesn't support
	 * {@link Tuple} based queries yet.
	 *
	 * @author Oliver Gierke
	 * @since 2.0.5
	 * @see https://bugs.eclipse.org/bugs/show_bug.cgi?id=289141
	 */
	@Slf4j
	private static class EclipseLinkProjectionQueryCreationListener implements QueryCreationListener<AbstractJpaQuery> {

		private static final String ECLIPSELINK_PROJECTIONS = "Usage of Spring Data projections detected on persistence provider EclipseLink. Make sure the following query methods declare result columns in exactly the order the accessors are declared in the projecting interface or the order of parameters for DTOs:";

		private final JpaMetamodel metamodel;

		private boolean warningLogged = false;

		/**
		 * Creates a new {@link EclipseLinkProjectionQueryCreationListener} for the given {@link EntityManager}.
		 * 
		 * @param em must not be {@literal null}.
		 */
		public EclipseLinkProjectionQueryCreationListener(EntityManager em) {

			Assert.notNull(em, "EntityManager must not be null!");

			this.metamodel = new JpaMetamodel(em.getMetamodel());
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.repository.core.support.QueryCreationListener#onCreation(org.springframework.data.repository.query.RepositoryQuery)
		 */
		@Override
		public void onCreation(AbstractJpaQuery query) {

			JpaQueryMethod queryMethod = query.getQueryMethod();
			ReturnedType type = queryMethod.getResultProcessor().getReturnedType();

			if (type.isProjecting() && !metamodel.isJpaManaged(type.getReturnedType())) {

				if (!warningLogged) {
					log.info(ECLIPSELINK_PROJECTIONS);
					this.warningLogged = true;
				}

				log.info(" - {}", queryMethod);
			}
		}
	}
}
