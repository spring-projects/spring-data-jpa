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

import java.io.Serializable;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

import org.springframework.data.mapping.context.MappingContext;
import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;
import org.springframework.data.repository.core.support.TransactionalRepositoryFactoryBeanSupport;
import org.springframework.util.Assert;

/**
 * Special adapter for Springs {@link org.springframework.beans.factory.FactoryBean} interface to allow easy setup of
 * repository factories via Spring configuration.
 * 
 * @author Oliver Gierke
 * @author Eberhard Wolff
 * @param <T> the type of the repository
 */
public class JpaRepositoryFactoryBean<T extends Repository<S, ID>, S, ID extends Serializable> extends
		TransactionalRepositoryFactoryBeanSupport<T, S, ID> {

	protected EntityManager entityManager;

	private ExpressionEvaluationContextProvider expressionEvaluationContextProvider = StandardExpressionEvaluationContextProvider.INSTANCE;

	private JpaResultPostProcessor jpaResultPostProcessor;

	/**
	 * The {@link EntityManager} to be used.
	 * 
	 * @param entityManager the entityManager to set
	 */
	@PersistenceContext
	public void setEntityManager(EntityManager entityManager) {
		this.entityManager = entityManager;
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.core.support.RepositoryFactoryBeanSupport#setMappingContext(org.springframework.data.mapping.context.MappingContext)
	 */
	@Override
	public void setMappingContext(MappingContext<?, ?> mappingContext) {
		super.setMappingContext(mappingContext);
	}

	/**
	 * The {@link ExpressionEvaluationContextProvider} to be used.
	 * 
	 * @param expressionEvaluationContextProvider the expressionEvaluationContextProvider to set.
	 * @since 1.7
	 */
	public void setExpressionEvaluationContextProvider(
			ExpressionEvaluationContextProvider expressionEvaluationContextProvider) {
		this.expressionEvaluationContextProvider = expressionEvaluationContextProvider;
	}

	/**
	 * The {@link JpaResultPostProcessor} to be used.
	 * 
	 * @param jpaResultPostProcessor the jpaResultPostProcessor to set.
	 * @since 1.7
	 */
	public void setJpaResultPostProcessor(JpaResultPostProcessor jpaResultPostProcessor) {
		this.jpaResultPostProcessor = jpaResultPostProcessor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.springframework.data.repository.support.
	 * TransactionalRepositoryFactoryBeanSupport#doCreateRepositoryFactory()
	 */
	@Override
	protected RepositoryFactorySupport doCreateRepositoryFactory() {
		return createRepositoryFactory(entityManager);
	}

	/**
	 * Returns a {@link RepositoryFactorySupport}.
	 * 
	 * @param entityManager
	 * @return
	 */
	protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
		return new JpaRepositoryFactory(entityManager, expressionEvaluationContextProvider, jpaResultPostProcessor);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.beans.factory.InitializingBean#afterPropertiesSet()
	 */
	@Override
	public void afterPropertiesSet() {

		Assert.notNull(entityManager, "EntityManager must not be null!");
		super.afterPropertiesSet();
	}
}
