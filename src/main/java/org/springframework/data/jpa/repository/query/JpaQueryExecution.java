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
package org.springframework.data.jpa.repository.query;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.core.support.SurroundingTransactionDetectorMethodInterceptor;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.support.PageableExecutionUtils;
import org.springframework.data.util.CloseableIterator;
import org.springframework.data.util.StreamUtils;
import org.springframework.lang.Nullable;
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
 */
public abstract class JpaQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();

		conversionService.addConverter(JpaResultConverters.BlobToByteArrayConverter.INSTANCE);
		conversionService.removeConvertible(Collection.class, Object.class);
		potentiallyRemoveOptionalConverter(conversionService);

		CONVERSION_SERVICE = conversionService;
	}

	/**
	 * Executes the given {@link AbstractStringBasedJpaQuery} with the given {@link ParameterBinder}.
	 *
	 * @param query must not be {@literal null}.
	 * @param values must not be {@literal null}.
	 * @return
	 */
	@Nullable
	public Object execute(AbstractJpaQuery query, Object[] values) {

		Assert.notNull(query, "AbstractJpaQuery must not be null!");
		Assert.notNull(values, "Values must not be null!");

		Object result;

		try {
			result = doExecute(query, values);
		} catch (NoResultException e) {
			return null;
		}

		if (result == null) {
			return null;
		}

		JpaQueryMethod queryMethod = query.getQueryMethod();
		Class<?> requiredType = queryMethod.getReturnType();

		if (void.class.equals(requiredType) || requiredType.isAssignableFrom(result.getClass())) {
			return result;
		}

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType) //
				? CONVERSION_SERVICE.convert(result, requiredType) //
				: result;
	}

	/**
	 * Method to implement {@link AbstractStringBasedJpaQuery} executions by single enum values.
	 *
	 * @param query
	 * @param values
	 * @return
	 */
	@Nullable
	protected abstract Object doExecute(AbstractJpaQuery query, Object[] values);

	/**
	 * Executes the query to return a simple collection of entities.
	 */
	static class CollectionExecution extends JpaQueryExecution {

		@Override
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {
			return query.createQuery(values).getResultList();
		}
	}

	/**
	 * Executes the query to return a {@link Slice} of entities.
	 *
	 * @author Oliver Gierke
	 * @since 1.6
	 */
	static class SlicedExecution extends JpaQueryExecution {

		private final Parameters<?, ?> parameters;

		/**
		 * Creates a new {@link SlicedExecution} using the given {@link Parameters}.
		 *
		 * @param parameters must not be {@literal null}.
		 */
		public SlicedExecution(Parameters<?, ?> parameters) {
			this.parameters = parameters;
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
		 */
		@Override
		@SuppressWarnings("unchecked")
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {

			ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
			Pageable pageable = accessor.getPageable();

			Query createQuery = query.createQuery(values);
			int pageSize = pageable.getPageSize();
			createQuery.setMaxResults(pageSize + 1);

			List<Object> resultList = createQuery.getResultList();
			boolean hasNext = resultList.size() > pageSize;

			return new SliceImpl<Object>(hasNext ? resultList.subList(0, pageSize) : resultList, pageable, hasNext);
		}
	}

	/**
	 * Executes the {@link AbstractStringBasedJpaQuery} to return a {@link org.springframework.data.domain.Page} of
	 * entities.
	 */
	static class PagedExecution extends JpaQueryExecution {

		private final Parameters<?, ?> parameters;

		public PagedExecution(Parameters<?, ?> parameters) {

			this.parameters = parameters;
		}

		@Override
		@SuppressWarnings("unchecked")
		protected Object doExecute(final AbstractJpaQuery repositoryQuery, final Object[] values) {

			ParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
			Query query = repositoryQuery.createQuery(values);

			return PageableExecutionUtils.getPage(query.getResultList(), accessor.getPageable(),
					() -> count(repositoryQuery, values));

		}

		private long count(AbstractJpaQuery repositoryQuery, Object[] values) {

			List<?> totals = repositoryQuery.createCountQuery(values).getResultList();
			return (totals.size() == 1 ? CONVERSION_SERVICE.convert(totals.get(0), Long.class) : totals.size());
		}
	}

	/**
	 * Executes a {@link AbstractStringBasedJpaQuery} to return a single entity.
	 */
	static class SingleEntityExecution extends JpaQueryExecution {

		@Override
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {

			return query.createQuery(values).getSingleResult();
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
		 * Creates an execution that automatically flushes the given {@link EntityManager} before execution and/or
		 * clears the given {@link EntityManager} after execution.
		 *
		 * @param em Must not be {@literal null}.
		 */
		public ModifyingExecution(JpaQueryMethod method, EntityManager em) {

			Assert.notNull(em, "The EntityManager must not be null.");

			Class<?> returnType = method.getReturnType();

			boolean isVoid = void.class.equals(returnType) || Void.class.equals(returnType);
			boolean isInt = int.class.equals(returnType) || Integer.class.equals(returnType);

			Assert.isTrue(isInt || isVoid, "Modifying queries can only use void or int/Integer as return type!");

			this.em = em;
			this.flush = method.getFlushAutomatically();
			this.clear = method.getClearAutomatically();
		}

		@Override
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {

			if (flush) {
				em.flush();
			}

			int result = query.createQuery(values).executeUpdate();

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

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
		 */
		@Override
		protected Object doExecute(AbstractJpaQuery jpaQuery, Object[] values) {

			Query query = jpaQuery.createQuery(values);
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
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {
			return !query.createQuery(values).getResultList().isEmpty();
		}
	}

	/**
	 * {@link JpaQueryExecution} executing a stored procedure.
	 *
	 * @author Thomas Darimont
	 * @since 1.6
	 */
	static class ProcedureExecution extends JpaQueryExecution {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
		 */
		@Override
		protected Object doExecute(AbstractJpaQuery jpaQuery, Object[] values) {

			Assert.isInstanceOf(StoredProcedureJpaQuery.class, jpaQuery);

			StoredProcedureJpaQuery storedProcedureJpaQuery = (StoredProcedureJpaQuery) jpaQuery;
			StoredProcedureQuery storedProcedure = storedProcedureJpaQuery.createQuery(values);
			storedProcedure.execute();

			return storedProcedureJpaQuery.extractOutputValue(storedProcedure);
		}
	}

	/**
	 * {@link JpaQueryExecution} executing a Java 8 Stream.
	 *
	 * @author Thomas Darimont
	 * @since 1.8
	 */
	static class StreamExecution extends JpaQueryExecution {

		private static final String NO_SURROUNDING_TRANSACTION = "You're trying to execute a streaming query method without a surrounding transaction that keeps the connection open so that the Stream can actually be consumed. Make sure the code consuming the stream uses @Transactional or any other way of declaring a (read-only) transaction.";

		private static Method streamMethod = ReflectionUtils.findMethod(Query.class, "getResultStream");

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
		 */
		@Override
		protected Object doExecute(final AbstractJpaQuery query, Object[] values) {

			if (!SurroundingTransactionDetectorMethodInterceptor.INSTANCE.isSurroundingTransactionActive()) {
				throw new InvalidDataAccessApiUsageException(NO_SURROUNDING_TRANSACTION);
			}

			Query jpaQuery = query.createQuery(values);

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

	/**
	 * Removes the converter being able to convert any object into an {@link Optional} from the given
	 * {@link ConversionService} in case we're running on Java 8.
	 *
	 * @param conversionService must not be {@literal null}.
	 */
	public static void potentiallyRemoveOptionalConverter(ConfigurableConversionService conversionService) {

		ClassLoader classLoader = JpaQueryExecution.class.getClassLoader();

		if (ClassUtils.isPresent("java.util.Optional", classLoader)) {

			try {

				Class<?> optionalType = ClassUtils.forName("java.util.Optional", classLoader);
				conversionService.removeConvertible(Object.class, optionalType);

			} catch (ClassNotFoundException | LinkageError o_O) {}
		}
	}
}
