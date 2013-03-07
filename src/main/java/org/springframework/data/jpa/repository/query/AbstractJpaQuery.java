/*
 * Copyright 2008-2013 the original author or authors.
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

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.QueryHint;
import javax.persistence.TypedQuery;

import org.springframework.data.jpa.repository.query.JpaQueryExecution.CollectionExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.PagedExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.SingleEntityExecution;
import org.springframework.data.repository.augment.QueryAugmentationEngine;
import org.springframework.data.repository.augment.QueryAugmentationEngineAware;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.util.Assert;

/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public abstract class AbstractJpaQuery implements RepositoryQuery, QueryAugmentationEngineAware {

	private final JpaQueryMethod method;
	private final EntityManager em;

	private QueryAugmentationEngine augmentationEngine;

	/**
	 * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
	 * 
	 * @param method
	 * @param em
	 */
	public AbstractJpaQuery(JpaQueryMethod method, EntityManager em) {

		Assert.notNull(method);
		Assert.notNull(em);

		this.method = method;
		this.em = em;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.augment.QueryAugmentationEngineAware#setQueryAugmentationEngine(org.springframework.data.repository.augment.QueryAugmentationEngine)
	 */
	public void setQueryAugmentationEngine(QueryAugmentationEngine engine) {
		this.augmentationEngine = engine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.RepositoryQuery#getQueryMethod
	 * ()
	 */
	public JpaQueryMethod getQueryMethod() {

		return method;
	}

	/**
	 * @return the em
	 */
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * @return the augmentationEngine
	 */
	public QueryAugmentationEngine getAugmentationEngine() {
		return augmentationEngine;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.springframework.data.repository.query.RepositoryQuery#execute(java
	 * .lang.Object[])
	 */
	public Object execute(Object[] parameters) {

		return doExecute(getExecution(), parameters);
	}

	/**
	 * @param execution
	 * @param values
	 * @return
	 */
	private Object doExecute(JpaQueryExecution execution, Object[] values) {

		return execution.execute(this, values);
	}

	protected JpaQueryExecution getExecution() {

		if (method.isCollectionQuery()) {
			return new CollectionExecution();
		} else if (method.isPageQuery()) {
			return new PagedExecution(method.getParameters());
		} else if (method.isModifyingQuery()) {
			return method.getClearAutomatically() ? new ModifyingExecution(method, em) : new ModifyingExecution(method, null);
		} else {
			return new SingleEntityExecution();
		}
	}

	/**
	 * Applies the declared query hints to the given query.
	 * 
	 * @param query
	 * @return
	 */
	private <T extends Query> T applyHints(T query, JpaQueryMethod method) {

		for (QueryHint hint : method.getHints()) {
			query.setHint(hint.name(), hint.value());
		}

		return query;
	}

	/**
	 * Applies the {@link LockModeType} provided by the {@link JpaQueryMethod} to the given {@link Query}.
	 * 
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private Query applyLockMode(Query query, JpaQueryMethod method) {

		LockModeType lockModeType = method.getLockModeType();
		return lockModeType == null ? query : query.setLockMode(lockModeType);
	}

	protected ParameterBinder createBinder(Object[] values) {
		return new ParameterBinder(getQueryMethod().getParameters(), values);
	}

	protected Query createQuery(Object[] values) {
		return applyLockMode(applyHints(doCreateQuery(values), method), method);
	}

	protected TypedQuery<Long> createCountQuery(Object[] values) {
		TypedQuery<Long> countQuery = doCreateCountQuery(values);
		return method.applyHintsToCountQuery() ? applyHints(countQuery, method) : countQuery;
	}

	/**
	 * Creates a {@link Query} instance for the given values.
	 * 
	 * @param values must not be {@literal null}.
	 * @return
	 */
	protected abstract Query doCreateQuery(Object[] values);

	/**
	 * Creates a {@link TypedQuery} for counting using the given values.
	 * 
	 * @param values must not be {@literal null}.
	 * @return
	 */
	protected abstract TypedQuery<Long> doCreateCountQuery(Object[] values);
}