/*
 * Copyright 2023 the original author or authors.
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
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Set of classes that define how to execute various types of JPA queries.
 *
 * @author Greg Turnquist
 */
abstract class AbstractQueryEngine implements QueryEngine {

	static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService.addConverter(JpaResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		conversionService.removeConvertible(Object.class, Optional.class);

		CONVERSION_SERVICE = conversionService;
	}

	private final EntityManager entityManager;
	private final QueryContext queryContext;
	private final PersistenceProvider provider;

	AbstractQueryEngine(EntityManager entityManager, QueryContext queryContext) {

		this.entityManager = entityManager;
		this.queryContext = queryContext;
		this.provider = PersistenceProvider.fromEntityManager(entityManager);
	}

	@Override
	public JpaQueryMethod getQueryMethod() {
		return queryContext.queryMethod();
	}

	public EntityManager getEntityManager() {
		return entityManager;
	}

	public QueryContext getQueryContext() {
		return queryContext;
	}

	/**
	 * Execute the query with no arguments.
	 */
	@Override
	public Object execute() {
		return execute(new Object[0]);
	}

	/**
	 * Extract a JPA query from the {@link QueryContext} and then use the {@link AbstractQueryEngine} to run it.
	 * 
	 * @param parameters must not be {@literal null}, it can be an empty array.
	 * @return
	 */
	@Nullable
	@Override
	final public Object execute(Object[] parameters) {

		JpaParametersParameterAccessor accessor = obtainParameterAccessor(parameters);

		Query queryToExecute = queryContext.createJpaQuery(accessor);

		Object rawResults = executeQuery(queryToExecute, accessor);

		Object unwrappedResults = unwrapAndApplyProjections(rawResults, accessor);

		return unwrappedResults;
	}

	/**
	 * Transform the incoming array of arguments into a {@link JpaParametersParameterAccessor}.
	 *
	 * @param values
	 * @return
	 */
	protected JpaParametersParameterAccessor obtainParameterAccessor(Object[] values) {

		return queryContext.getQueryMethod() //
				.map(queryMethod -> {
					return queryMethod.isNativeQuery() && PersistenceProvider.HIBERNATE.equals(provider) //
							? new HibernateJpaParametersParameterAccessor(queryMethod.getParameters(), values, entityManager) //
							: new JpaParametersParameterAccessor(queryMethod.getParameters(), values);
				}) //
				.orElseGet(() -> new JpaParametersParameterAccessor(new JpaParameters(List.of()), values));
	}

	/**
	 * Core function that defines the flow of executing a {@link Query}.
	 *
	 * @param query
	 * @param accessor
	 * @return
	 */
	@Nullable
	private Object executeQuery(Query query, JpaParametersParameterAccessor accessor) {

		Assert.notNull(query, "Query must not be null");
		Assert.notNull(accessor, "JpaParametersParameterAccessor must not be null");

		Object result;

		try {
			result = doExecute(query, accessor);
		} catch (NoResultException ex) {
			return null;
		}

		if (result == null) {
			return null;
		}

		return queryContext.getQueryMethod() //
				.map(queryMethod -> {

					Class<?> requiredType = queryMethod.getReturnType();

					if (ClassUtils.isAssignable(requiredType, void.class) || ClassUtils.isAssignableValue(requiredType, result)) {
						return result;
					}

					return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType) //
							? CONVERSION_SERVICE.convert(result, requiredType) //
							: result;
				}) //
				.orElse(result);
	}

	/**
	 * Execute the query itself.
	 *
	 * @param query
	 * @param accessor
	 * @return
	 */
	@Nullable
	protected abstract Object doExecute(Query query, JpaParametersParameterAccessor accessor);

	/**
	 * Execute a {@link QueryContext}'s count-based query with no arguments.
	 */
	@Override
	public long executeCount() {
		return executeCount(obtainParameterAccessor(new Object[0]));
	}

	/**
	 * Execute a {@link QueryContext}'s count-based query with {@link JpaParametersParameterAccessor} applied.
	 *
	 * @param accessor
	 */
	protected long executeCount(JpaParametersParameterAccessor accessor) {

		List<?> totals = queryContext.createJpaCountQuery(accessor).getResultList();

		return totals.size() == 1 //
				? CONVERSION_SERVICE.convert(totals.get(0), Long.class) //
				: totals.size();
	}

	/**
	 * Unwrap the results and apply any projections.
	 *
	 * @param result
	 * @param accessor
	 * @return
	 */
	@Nullable
	private Object unwrapAndApplyProjections(@Nullable Object result, JpaParametersParameterAccessor accessor) {

		return queryContext.getQueryMethod() //
				.map(queryMethod -> {

					ResultProcessor withDynamicProjection = queryMethod.getResultProcessor().withDynamicProjection(accessor);

					return withDynamicProjection.processResult(result,
							new TupleConverter(withDynamicProjection.getReturnedType()));
				}) //
				.orElse(result);
	}

	/**
	 * Execute a JPA query for a {@link Collection}-returning repository method.
	 *
	 * @author Greg Turnquist
	 */
	static class CollectionQueryEngine extends AbstractQueryEngine {

		CollectionQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {
			return query.getResultList();
		}
	}

	/**
	 * Execute a JPA query for a repository method using the Scroll API.
	 *
	 * @author Greg Turnquist
	 */
	static class ScrollQueryEngine extends AbstractQueryEngine {

		private final Sort sort;
		private final ScrollDelegate<?> delegate;

		public ScrollQueryEngine(EntityManager entityManager, CustomFinderQueryContext queryContext,
				ScrollDelegate<?> delegate) {

			super(entityManager, queryContext);

			this.sort = queryContext.getTree().getSort();
			this.delegate = delegate;
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {

			ScrollPosition scrollPosition = accessor.getScrollPosition();

			return delegate.scroll(query, sort.and(accessor.getSort()), scrollPosition);
		}
	}

	/**
	 * Execute a JPA query for a Spring Data {@link org.springframework.data.domain.Slice}-returning repository method.
	 *
	 * @author Greg Turnquist
	 */
	static class SlicedQueryEngine extends AbstractQueryEngine {

		public SlicedQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {

			Pageable pageable = accessor.getPageable();

			int pageSize = 0;
			if (pageable.isPaged()) {

				pageSize = pageable.getPageSize();
				query.setMaxResults(pageSize + 1);
			}

			List<?> resultList = query.getResultList();

			boolean hasNext = pageable.isPaged() && resultList.size() > pageSize;

			if (hasNext) {
				return new SliceImpl<>(resultList.subList(0, pageSize), pageable, hasNext);
			} else {
				return new SliceImpl<>(resultList, pageable, hasNext);
			}
		}
	}

	/**
	 * Execute a JPA query for a Spring Data {@link org.springframework.data.domain.Page}-returning repository method.
	 *
	 * @author Greg Turnquist
	 */
	static class PagedQueryEngine extends AbstractQueryEngine {

		public PagedQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {
			return PageableExecutionUtils.getPage(query.getResultList(), accessor.getPageable(),
					() -> executeCount(accessor));
		}
	}

	/**
	 * Execute a JPA query for a repository method returning a single value.
	 *
	 * @author Greg Turnquist
	 */
	static class SingleEntityQueryEngine extends AbstractQueryEngine {

		public SingleEntityQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {
			return query.getSingleResult();
		}

	}

	/**
	 * Execute a JPA query for a repository with an @{@link org.springframework.data.jpa.repository.Modifying} annotation
	 * applied.
	 *
	 * @author Greg Turnquist
	 */
	static class ModifyingQueryEngine extends AbstractQueryEngine {

		private final EntityManager entityManager;

		public ModifyingQueryEngine(EntityManager entityManager, QueryContext queryContext) {

			super(entityManager, queryContext);

			Assert.notNull(entityManager, "EntityManager must not be null");

			this.entityManager = entityManager;
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {

			Class<?> returnType = getQueryMethod().getReturnType();

			boolean isVoid = ClassUtils.isAssignable(returnType, Void.class);
			boolean isInt = ClassUtils.isAssignable(returnType, Integer.class);

			Assert.isTrue(isInt || isVoid,
					"Modifying queries can only use void or int/Integer as return type; Offending method: " + getQueryMethod());

			if (getQueryMethod().getFlushAutomatically()) {
				entityManager.flush();
			}

			int result = query.executeUpdate();

			if (getQueryMethod().getClearAutomatically()) {
				entityManager.clear();
			}

			return result;
		}
	}

	/**
	 * Execute a JPA query for a {@link java.util.stream.Stream}-returning repository method.
	 *
	 * @author Greg Turnquist
	 */
	static class StreamQueryEngine extends AbstractQueryEngine {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a streaming query method without a surrounding transaction that keeps the connection open so that the Stream can actually be consumed; Make sure the code consuming the stream uses @Transactional or any other way of declaring a (read-only) transaction";

		public StreamQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			return query.getResultStream();
		}
	}

	/**
	 * Execute a JPA delete.
	 *
	 * @author Greg Turnquist
	 */
	static class DeleteQueryEngine extends AbstractQueryEngine {

		public DeleteQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		/**
		 * After deleting all the items, either return the retrieve {@link Collection} or the number of elements deleted.
		 * 
		 * @param query
		 * @param accessor
		 * @return
		 */
		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {

			List<?> resultList = query.getResultList();

			resultList.forEach(item -> getEntityManager().remove(item));

			return getQueryMethod().isCollectionQuery() //
					? resultList //
					: resultList.size();
		}

	}

	/**
	 * Execute a JPA exists.
	 *
	 * @author Greg Turnquist
	 */
	static class ExistsQueryEngine extends AbstractQueryEngine {

		public ExistsQueryEngine(EntityManager entityManager, QueryContext queryContext) {
			super(entityManager, queryContext);
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {
			return !query.getResultList().isEmpty();
		}
	}

	/**
	 * Execute a JPA stored procedure.
	 *
	 * @author Greg Turnquist
	 */
	static class ProcedureQueryEngine extends AbstractQueryEngine {

		private final StoredProcedureQueryContext storedProcedureContext;

		public ProcedureQueryEngine(EntityManager entityManager, QueryContext context) {

			super(entityManager, context);

			Assert.isInstanceOf(StoredProcedureQueryContext.class, context);
			this.storedProcedureContext = (StoredProcedureQueryContext) context;
		}

		@Override
		protected Object doExecute(Query query, JpaParametersParameterAccessor accessor) {

			Assert.isInstanceOf(StoredProcedureQuery.class, query);
			StoredProcedureQuery procedure = (StoredProcedureQuery) query;

			try {
				boolean returnsResultSet = procedure.execute();

				if (returnsResultSet) {

					if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
						throw new InvalidDataAccessApiUsageException(
								"You're trying to execute a @Procedure method without a surrounding transaction that keeps the connection open so that the ResultSet can actually be consumed; Make sure the consumer code uses @Transactional or any other way of declaring a (read-only) transaction");
					}

					return storedProcedureContext.queryMethod().isCollectionQuery() //
							? procedure.getResultList() //
							: procedure.getSingleResult();
				}

				return storedProcedureContext.extractOutputValue(procedure);
			} finally {
				if (procedure instanceof AutoCloseable autoCloseable) {
					try {
						autoCloseable.close();
					} catch (Exception ignored) {}
				}
			}
		}
	}
}
