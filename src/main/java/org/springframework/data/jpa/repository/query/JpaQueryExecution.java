/*
 * Copyright 2008-2015 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.StoredProcedureQuery;

import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.repository.query.ParameterAccessor;
import org.springframework.data.repository.query.Parameters;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.util.CloseableIterator;
import org.springframework.data.util.StreamUtils;
import org.springframework.util.Assert;

/**
 * Set of classes to contain query execution strategies. Depending (mostly) on the return type of a
 * {@link org.springframework.data.repository.query.QueryMethod} a {@link AbstractStringBasedJpaQuery} can be executed
 * in various flavors.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public abstract class JpaQueryExecution {

	private static final ConversionService CONVERSION_SERVICE;

	static {

		ConfigurableConversionService conversionService = new DefaultConversionService();
		conversionService.removeConvertible(Collection.class, Object.class);
		conversionService.addConverter(JpaResultConverters.BlobToByteArrayConverter.INSTANCE);

		CONVERSION_SERVICE = conversionService;
	}

	/**
	 * Executes the given {@link AbstractStringBasedJpaQuery} with the given {@link ParameterBinder}.
	 * 
	 * @param query must not be {@literal null}.
	 * @param binder must not be {@literal null}.
	 * @return
	 */
	public Object execute(AbstractJpaQuery query, Object[] values) {

		Assert.notNull(query);
		Assert.notNull(values);

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

		return CONVERSION_SERVICE.canConvert(result.getClass(), requiredType)
				? CONVERSION_SERVICE.convert(result, requiredType) : result;
	}

	/**
	 * Method to implement {@link AbstractStringBasedJpaQuery} executions by single enum values.
	 * 
	 * @param query
	 * @param binder
	 * @return
	 */
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
		protected Object doExecute(AbstractJpaQuery repositoryQuery, Object[] values) {

			// Execute query to compute total
			Query projection = repositoryQuery.createCountQuery(values);

			List<?> totals = projection.getResultList();
			Long total = totals.size() == 1 ? CONVERSION_SERVICE.convert(totals.get(0), Long.class) : totals.size();

			ParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);
			Pageable pageable = accessor.getPageable();

			if (total.equals(0L)) {
				return new PageImpl<Object>(Collections.emptyList(), pageable, total);
			}

			Query query = repositoryQuery.createQuery(values);

			List<Object> content = pageable == null || total > pageable.getOffset() ? query.getResultList()
					: Collections.emptyList();

			return new PageImpl<Object>(content, pageable, total);
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

		/**
		 * Creates an execution that automatically clears the given {@link EntityManager} after execution if the given
		 * {@link EntityManager} is not {@literal null}.
		 * 
		 * @param em
		 */
		public ModifyingExecution(JpaQueryMethod method, EntityManager em) {

			Class<?> returnType = method.getReturnType();

			boolean isVoid = void.class.equals(returnType) || Void.class.equals(returnType);
			boolean isInt = int.class.equals(returnType) || Integer.class.equals(returnType);

			Assert.isTrue(isInt || isVoid, "Modifying queries can only use void or int/Integer as return type!");

			this.em = em;
		}

		@Override
		protected Object doExecute(AbstractJpaQuery query, Object[] values) {

			int result = query.createQuery(values).executeUpdate();

			if (em != null) {
				em.clear();
			}

			return result;
		}
	}

	/**
	 * {@link Execution} removing entities matching the query.
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
	 * {@link Execution} executing a stored procedure.
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
	 * {@link Execution} executing a Java 8 Stream.
	 * 
	 * @author Thomas Darimont
	 * @since 1.8
	 */
	static class StreamExecution extends JpaQueryExecution {

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.JpaQueryExecution#doExecute(org.springframework.data.jpa.repository.query.AbstractJpaQuery, java.lang.Object[])
		 */
		@Override
		protected Object doExecute(final AbstractJpaQuery query, Object[] values) {

			Query jpaQuery = query.createQuery(values);
			PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(query.getEntityManager());
			CloseableIterator<Object> iter = persistenceProvider.executeQueryWithResultStream(jpaQuery);

			return StreamUtils.createStreamFromIterator(iter);
		}
	}
}
