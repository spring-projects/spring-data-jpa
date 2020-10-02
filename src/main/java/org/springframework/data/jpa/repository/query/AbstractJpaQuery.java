/*
 * Copyright 2008-2020 the original author or authors.
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

import java.util.List;
import java.util.Map;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.QueryHint;
import javax.persistence.Tuple;
import javax.persistence.TypedQuery;

import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.CollectionExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ModifyingExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.PagedExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ProcedureExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.SingleEntityExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.SlicedExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.StreamExecution;
import org.springframework.data.jpa.util.JpaMetamodel;
import org.springframework.data.jpa.util.TupleConverter;
import org.springframework.data.repository.query.RepositoryQuery;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.util.Lazy;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Abstract base class to implement {@link RepositoryQuery}s.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Nicolas Cirigliano
 * @author Jens Schauder
 * @author Сергей Цыпанов
 * @author Lorenzo Dee
 */
public abstract class AbstractJpaQuery implements RepositoryQuery {

	private final JpaQueryMethod method;
	private final EntityManager em;
	private final JpaMetamodel metamodel;
	private final PersistenceProvider provider;
	private final Lazy<JpaQueryExecution> execution;

	final Lazy<ParameterBinder> parameterBinder = new Lazy<>(this::createBinder);

	/**
	 * Creates a new {@link AbstractJpaQuery} from the given {@link JpaQueryMethod}.
	 *
	 * @param method
	 * @param em
	 */
	public AbstractJpaQuery(JpaQueryMethod method, EntityManager em) {

		Assert.notNull(method, "JpaQueryMethod must not be null!");
		Assert.notNull(em, "EntityManager must not be null!");

		this.method = method;
		this.em = em;
		this.metamodel = JpaMetamodel.of(em.getMetamodel());
		this.provider = PersistenceProvider.fromEntityManager(em);
		this.execution = Lazy.of(() -> {

			if (method.isStreamQuery()) {
				return new StreamExecution();
			} else if (method.isProcedureQuery()) {
				return new ProcedureExecution();
			} else if (method.isCollectionQuery()) {
				return new CollectionExecution();
			} else if (method.isSliceQuery()) {
				return new SlicedExecution();
			} else if (method.isPageQuery()) {
				return new PagedExecution();
			} else if (method.isModifyingQuery()) {
				return null;
			} else {
				return new SingleEntityExecution();
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#getQueryMethod()
	 */
	@Override
	public JpaQueryMethod getQueryMethod() {
		return method;
	}

	/**
	 * Returns the {@link EntityManager}.
	 *
	 * @return will never be {@literal null}.
	 */
	protected EntityManager getEntityManager() {
		return em;
	}

	/**
	 * Returns the {@link JpaMetamodel}.
	 *
	 * @return
	 */
	protected JpaMetamodel getMetamodel() {
		return metamodel;
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.repository.query.RepositoryQuery#execute(java.lang.Object[])
	 */
	@Nullable
	@Override
	public Object execute(Object[] parameters) {
		return doExecute(getExecution(), parameters);
	}

	/**
	 * @param execution
	 * @param values
	 * @return
	 */
	@Nullable
	private Object doExecute(JpaQueryExecution execution, Object[] values) {

		JpaParametersParameterAccessor accessor = new JpaParametersParameterAccessor(method.getParameters(), values);
		Object result = execution.execute(this, accessor);

		ResultProcessor withDynamicProjection = method.getResultProcessor().withDynamicProjection(accessor);
		return withDynamicProjection.processResult(result, new TupleConverter(withDynamicProjection.getReturnedType().getReturnedType()));
	}

	protected JpaQueryExecution getExecution() {

		JpaQueryExecution execution = this.execution.getNullable();

		if (execution != null) {
			return execution;
		}

		if (method.isModifyingQuery()) {
			return new ModifyingExecution(method, em);
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
	protected <T extends Query> T applyHints(T query, JpaQueryMethod method) {

		List<QueryHint> hints = method.getHints();

		if (!hints.isEmpty()) {
			for (QueryHint hint : hints) {
				applyQueryHint(query, hint);
			}
		}

		return query;
	}

	/**
	 * Protected to be able to customize in sub-classes.
	 *
	 * @param query must not be {@literal null}.
	 * @param hint must not be {@literal null}.
	 */
	protected <T extends Query> void applyQueryHint(T query, QueryHint hint) {

		Assert.notNull(query, "Query must not be null!");
		Assert.notNull(hint, "QueryHint must not be null!");

		query.setHint(hint.name(), hint.value());
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

	protected ParameterBinder createBinder() {
		return ParameterBinderFactory.createBinder(getQueryMethod().getParameters());
	}

	protected Query createQuery(JpaParametersParameterAccessor parameters) {
		return applyLockMode(applyEntityGraphConfiguration(applyHints(doCreateQuery(parameters), method), method), method);
	}

	/**
	 * Configures the {@link javax.persistence.EntityGraph} to use for the given {@link JpaQueryMethod} if the
	 * {@link EntityGraph} annotation is present.
	 *
	 * @param query must not be {@literal null}.
	 * @param method must not be {@literal null}.
	 * @return
	 */
	private Query applyEntityGraphConfiguration(Query query, JpaQueryMethod method) {

		JpaEntityGraph entityGraph = method.getEntityGraph();

		if (entityGraph != null) {
			Map<String, Object> hints = Jpa21Utils.tryGetFetchGraphHints(em, method.getEntityGraph(),
					getQueryMethod().getEntityInformation().getJavaType());

			for (Map.Entry<String, Object> hint : hints.entrySet()) {
				query.setHint(hint.getKey(), hint.getValue());
			}
		}

		return query;
	}

	protected Query createCountQuery(JpaParametersParameterAccessor values) {
		Query countQuery = doCreateCountQuery(values);
		return method.applyHintsToCountQuery() ? applyHints(countQuery, method) : countQuery;
	}

	/**
	 * Returns the type to be used when creating the JPA query.
	 *
	 * @return
	 * @since 2.0.5
	 */
	@Nullable
	protected Class<?> getTypeToRead(ReturnedType returnedType) {

		if (PersistenceProvider.ECLIPSELINK.equals(provider)) {
			return null;
		}

		return returnedType.isProjecting() && !getMetamodel().isJpaManaged(returnedType.getReturnedType()) //
				? Tuple.class //
				: null;
	}

	/**
	 * Creates a {@link Query} instance for the given values.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract Query doCreateQuery(JpaParametersParameterAccessor accessor);

	/**
	 * Creates a {@link TypedQuery} for counting using the given values.
	 *
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	protected abstract Query doCreateCountQuery(JpaParametersParameterAccessor accessor);

}
