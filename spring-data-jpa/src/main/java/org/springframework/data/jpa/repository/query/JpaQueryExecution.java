/*
 * Copyright 2008-2025 the original author or authors.
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
import jakarta.persistence.Query;
import jakarta.persistence.StoredProcedureQuery;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.ConversionService;

import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.data.util.CloseableIterator;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.ReflectionUtils;

/**
 * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a
 * {@link org.springframework.data.repository.query.QueryMethod} a {@link AbstractStringBasedJpaQuery} can be executed
 * in various flavors.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Mark Paluch
 * @author Christoph Strobl
 * @author Nicolas Cirigliano
 * @author Jens Schauder
 * @author Gabriel Basilio
 * @author Greg Turnquist
 */
public abstract class JpaQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService.addConverter(JpaResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		conversionService.removeConvertible(Object.class, Optional.class);

		CONVERSION_SERVICE = conversionService;
	}

	/**
	 * Executes the given {@link AbstractStringBasedJpaQuery} with the given {@link ParameterBinder}.
	 *
	 * @param query must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 * @return
	 */
	public @Nullable Object execute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

		Assert.notNull(query, "AbstractJpaQuery must not be null");
		Assert.notNull(accessor, "JpaParametersParameterAccessor must not be null");

		Object result = doExecute(query, accessor);

		if (result == null) {
			return null;
		}

		JpaQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();

		if (ClassUtils.isAssignable(requiredType, void.class) || ClassUtils.isAssignableValue(requiredType, result)) {
			return result;
		}

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType) //
				? CONVERSION_SERVICE.convert(result, requiredType) //
				: result;
	}

	/**
	 * Method to implement {@link AbstractStringBasedJpaQuery} executions by single enum values.
	 *
	 * @param query must not be {@literal null}.
	 * @param accessor must not be {@literal null}.
	 */
	protected abstract @Nullable Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor);

	/**
	 * Executes the query to return a simple collection of entities.
	 */
	static class CollectionExecution extends JpaQueryExecution {

		@Override
		protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {
			return query.createQuery(accessor).getResultList();
		}
	}

	/**
	 * Executes the query to return a {@link org.springframework.data.domain.Window} of entities.
	 *
	 * @author Mark Paluch
	 * @since 3.1
	 */
	static class ScrollExecution extends JpaQueryExecution {

		private final Sort sort;
		private final ScrollDelegate<?> delegate;

		ScrollExecution(Sort sort, ScrollDelegate<?> delegate) {

			this.sort = sort;
			this.delegate = delegate;
		}

		@Override
		@SuppressWarnings("NullAway")
		protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

			ScrollPosition scrollPosition = accessor.getScrollPosition();
			Query scrollQuery = query.createQuery(accessor);

			return delegate.scroll(scrollQuery, sort.and(accessor.getSort()), scrollPosition);
		}
	}

	/**
	 * Executes the query to return a {@link Slice} of entities.
	 *
	 * @author Oliver Gierke
	 * @since 1.6
	 */
	static class SlicedExecution extends JpaQueryExecution {

		@Override
		@SuppressWarnings("unchecked")
		protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

			Pageable pageable = accessor.getPageable();
			Query createQuery = query.createQuery(accessor);

			int pageSize = 0;
			if (pageable.isPaged()) {

				pageSize = pageable.getPageSize();
				createQuery.setMaxResults(pageSize + 1);
			}

			List<Object> resultList = createQuery.getResultList();

			boolean hasNext = pageable.isPaged() && resultList.size() > pageSize;

			return new SliceImpl<>(hasNext ? resultList.subList(0, pageSize) : resultList, pageable, hasNext);

		}
	}

	/**
	 * Executes the {@link AbstractStringBasedJpaQuery} to return a {@link org.springframework.data.domain.Page} of
	 * entities.
	 */
	static class PagedExecution extends JpaQueryExecution {

		@Override
		@SuppressWarnings("unchecked")
		protected Object doExecute(AbstractJpaQuery repositoryQuery, JpaParametersParameterAccessor accessor) {

			Query query = repositoryQuery.createQuery(accessor);

			return PageableExecutionUtils.getPage(query.getResultList(), accessor.getPageable(),
					() -> count(repositoryQuery, accessor));
		}

		private long count(AbstractJpaQuery repositoryQuery, JpaParametersParameterAccessor accessor) {

			List<?> totals = repositoryQuery.createCountQuery(accessor).getResultList();
			return (totals.size() == 1 ? CONVERSION_SERVICE.convert(totals.get(0), Long.class) : totals.size());
		}
	}

	/**
	 * Executes a {@link AbstractStringBasedJpaQuery} to return a single entity.
	 */
	static class SingleEntityExecution extends JpaQueryExecution {

		@Override
		protected @Nullable Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

			return query.createQuery(accessor).getSingleResultOrNull();
		}
	}

	/**
	 * Executes a modifying query such as an update, insert or delete.
	 */
	static class ModifyingExecution extends JpaQueryExecution {

		private final EntityManager em;
		private final boolean flush;
		private final boolean clear;

		/**
		 * Creates an execution that automatically flushes the given {@link EntityManager} before execution and/or clears
		 * the given {@link EntityManager} after execution.
		 *
		 * @param em Must not be {@literal null}.
		 */
		public ModifyingExecution(JpaQueryMethod method, EntityManager em) {

			Assert.notNull(em, "The EntityManager must not be null");

			Class<?> returnType = method.getReturnType();

			boolean isVoid = ClassUtils.isAssignable(returnType, Void.class);
			boolean isInt = ClassUtils.isAssignable(returnType, Integer.class);

			Assert.isTrue(isInt || isVoid,
					"Modifying queries can only use void or int/Integer as return type; Offending method: " + method);

			this.em = em;
			this.flush = method.getFlushAutomatically();
			this.clear = method.getClearAutomatically();
		}

		@Override
		protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

			if (flush) {
				em.flush();
			}

			int result = query.createQuery(accessor).executeUpdate();

			if (clear) {
				em.clear();
			}

			return result;
		}
	}

	/**
	 * {@link JpaQueryExecution} removing entities matching the query.
	 *
	 * @author Thomas Darimont
	 * @author Oliver Gierke
	 * @since 1.6
	 */
	static class DeleteExecution extends JpaQueryExecution {

		private final EntityManager em;

		public DeleteExecution(EntityManager em) {
			this.em = em;
		}

		@Override
		protected Object doExecute(AbstractJpaQuery jpaQuery, JpaParametersParameterAccessor accessor) {

			Query query = jpaQuery.createQuery(accessor);
			List<?> resultList = query.getResultList();

			for (Object o : resultList) {
				em.remove(o);
			}

			return jpaQuery.getQueryMethod().isCollectionQuery() ? resultList : resultList.size();
		}
	}

	/**
	 * {@link JpaQueryExecution} performing an exists check on the query.
	 *
	 * @author Mark Paluch
	 * @since 1.11
	 */
	static class ExistsExecution extends JpaQueryExecution {

		@Override
		protected Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {
			return !query.createQuery(accessor).getResultList().isEmpty();
		}
	}

	/**
	 * {@link JpaQueryExecution} executing a stored procedure.
	 *
	 * @author Thomas Darimont
	 * @since 1.6
	 */
	static class ProcedureExecution extends JpaQueryExecution {

		private final boolean collectionQuery;

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a @Procedure method without a surrounding transaction that keeps the connection open so that the ResultSet can actually be consumed; Make sure the consumer code uses @Transactional or any other way of declaring a (read-only) transaction";

		ProcedureExecution(boolean collectionQuery) {
			this.collectionQuery = collectionQuery;
		}

		@Override
		protected @Nullable Object doExecute(AbstractJpaQuery jpaQuery, JpaParametersParameterAccessor accessor) {

			Assert.isInstanceOf(StoredProcedureJpaQuery.class, jpaQuery);

			StoredProcedureJpaQuery query = (StoredProcedureJpaQuery) jpaQuery;
			StoredProcedureQuery procedure = query.createQuery(accessor);
			Class<?> returnType = query.getQueryMethod().getReturnType();

			try {

				boolean returnsResultSet = procedure.execute();

				if (returnsResultSet) {

					if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
						throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
					}

					if (!Map.class.isAssignableFrom(returnType)) {
						return collectionQuery ? procedure.getResultList() : procedure.getSingleResult();
					}
				}

				return query.extractOutputValue(procedure);
			} finally {

				if (procedure instanceof AutoCloseable ac) {
					try {
						ac.close();
					} catch (Exception ignored) {}
				}
			}
		}
	}

	/**
	 * {@link JpaQueryExecution} executing a Java 8 Stream.
	 *
	 * @author Thomas Darimont
	 * @since 1.8
	 */
	static class StreamExecution extends JpaQueryExecution {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a streaming query method without a surrounding transaction that keeps the connection open so that the Stream can actually be consumed; Make sure the code consuming the stream uses @Transactional or any other way of declaring a (read-only) transaction";

		private static final @Nullable Method streamMethod = ReflectionUtils.findMethod(Query.class, "getResultStream");

		@Override
		protected @Nullable Object doExecute(AbstractJpaQuery query, JpaParametersParameterAccessor accessor) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			Query jpaQuery = query.createQuery(accessor);

			// JPA 2.2 on the classpath
			if (streamMethod != null) {
				return ReflectionUtils.invokeMethod(streamMethod, jpaQuery);
			}

			// Fall back to legacy stream execution
			PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(query.getEntityManager());
			CloseableIterator<Object> iter = persistenceProvider.executeQueryWithResultStream(jpaQuery);

			return StreamUtils.createStreamFromIterator(iter);
		}
	}

}
