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
package org.springframework.data.jpa.repository.query;

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.DeleteExecution;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.repository.query.ParametersParameterAccessor;
import org.springframework.data.repository.query.parser.PartTree;

/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 * 
 * @author Oliver Gierke
 * @author Thomas Darimont
 */
public class PartTreeJpaQuery extends AbstractJpaQuery {

	private final Class<?> domainClass;
	private final PartTree tree;
	private final JpaParameters parameters;

	private final QueryPreparer query;
	private final QueryPreparer countQuery;
	private final EntityManager em;

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	public PartTreeJpaQuery(JpaQueryMethod method, EntityManager em) {

		super(method, em);
		this.em = em;

		this.domainClass = method.getEntityInformation().getJavaType();
		this.tree = new PartTree(method.getName(), domainClass);
		this.parameters = method.getParameters();

		this.countQuery = new CountQueryPreparer(parameters.potentiallySortsDynamically());
		this.query = tree.isCountProjection() ? countQuery : new QueryPreparer(parameters.potentiallySortsDynamically());
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateQuery(java.lang.Object[])
	 */
	@Override
	public Query doCreateQuery(Object[] values) {
		return query.createQuery(values);
	}

	/*
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#doCreateCountQuery(java.lang.Object[])
	 */
	@Override
	@SuppressWarnings("unchecked")
	public TypedQuery<Long> doCreateCountQuery(Object[] values) {
		return (TypedQuery<Long>) countQuery.createQuery(values);
	}

	/* 
	 * (non-Javadoc)
	 * @see org.springframework.data.jpa.repository.query.AbstractJpaQuery#getExecution()
	 */
	@Override
	protected JpaQueryExecution getExecution() {
		return this.tree.isDelete() ? new DeleteExecution(em) : super.getExecution();
	}

	/**
	 * Query preparer to create {@link CriteriaQuery} instances and potentially cache them.
	 * 
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private class QueryPreparer {

		private final CriteriaQuery<?> cachedCriteriaQuery;
		private final List<ParameterMetadata<?>> expressions;

		public QueryPreparer(boolean recreateQueries) {

			JpaQueryCreator creator = createCreator(null);
			this.cachedCriteriaQuery = recreateQueries ? null : creator.createQuery();
			this.expressions = recreateQueries ? null : creator.getParameterExpressions();
		}

		/**
		 * Creates a new {@link Query} for the given parameter values.
		 * 
		 * @param values
		 * @return
		 */
		public Query createQuery(Object[] values) {

			CriteriaQuery<?> criteriaQuery = cachedCriteriaQuery;
			List<ParameterMetadata<?>> expressions = this.expressions;
			ParametersParameterAccessor accessor = new ParametersParameterAccessor(parameters, values);

			if (cachedCriteriaQuery == null || accessor.hasBindableNullValue()) {
				JpaQueryCreator creator = createCreator(accessor);
				criteriaQuery = creator.createQuery(getDynamicSort(values));
				expressions = creator.getParameterExpressions();
			}

			TypedQuery<?> jpaQuery = createQuery(criteriaQuery);

			return restrictMaxResultsIfNecessary(invokeBinding(getBinder(values, expressions), jpaQuery));
		}

		/**
		 * Restricts the max results of the given {@link Query} if the current {@code tree} marks this {@code query} as
		 * limited.
		 * 
		 * @param query
		 * @return
		 */
		private Query restrictMaxResultsIfNecessary(Query query) {

			if (tree.isLimiting()) {

				if (query.getMaxResults() != Integer.MAX_VALUE) {
					/*
					 * In order to return the correct results, we have to adjust the first result offset to be returned if:
					 * - a Pageable parameter is present 
					 * - AND the requested page number > 0
					 * - AND the requested page size was bigger than the derived result limitation via the First/Top keyword.
					 */
					if (query.getMaxResults() > tree.getMaxResults() && query.getFirstResult() > 0) {
						query.setFirstResult(query.getFirstResult() - (query.getMaxResults() - tree.getMaxResults()));
					}
				}

				query.setMaxResults(tree.getMaxResults());
			}

			return query;
		}

		/**
		 * Checks whether we are working with a cached {@link CriteriaQuery} and snychronizes the creation of a
		 * {@link TypedQuery} instance from it. This is due to non-thread-safety in the {@link CriteriaQuery} implementation
		 * of some persistence providers (i.e. Hibernate in this case).
		 * 
		 * @see DATAJPA-396
		 * @param criteriaQuery must not be {@literal null}.
		 * @return
		 */
		private TypedQuery<?> createQuery(CriteriaQuery<?> criteriaQuery) {

			if (this.cachedCriteriaQuery != null) {
				synchronized (this.cachedCriteriaQuery) {
					return getEntityManager().createQuery(criteriaQuery);
				}
			}

			return getEntityManager().createQuery(criteriaQuery);
		}

		protected JpaQueryCreator createCreator(ParametersParameterAccessor accessor) {

			EntityManager entityManager = getEntityManager();
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(entityManager);

			ParameterMetadataProvider provider = accessor == null ? new ParameterMetadataProvider(builder, parameters,
					persistenceProvider) : new ParameterMetadataProvider(builder, accessor, persistenceProvider);

			return new JpaQueryCreator(tree, domainClass, builder, provider);
		}

		/**
		 * Invokes parameter binding on the given {@link TypedQuery}.
		 * 
		 * @param binder
		 * @param query
		 * @return
		 */
		protected Query invokeBinding(ParameterBinder binder, TypedQuery<?> query) {

			return binder.bindAndPrepare(query);
		}

		private ParameterBinder getBinder(Object[] values, List<ParameterMetadata<?>> expressions) {
			return new CriteriaQueryParameterBinder(parameters, values, expressions);
		}

		private Sort getDynamicSort(Object[] values) {

			return parameters.potentiallySortsDynamically() ? new ParametersParameterAccessor(parameters, values).getSort()
					: null;
		}
	}

	/**
	 * Special {@link QueryPreparer} to create count queries.
	 * 
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private class CountQueryPreparer extends QueryPreparer {

		public CountQueryPreparer(boolean recreateQueries) {
			super(recreateQueries);
		}

		/*
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.PartTreeJpaQuery.QueryPreparer#createCreator(org.springframework.data.repository.query.ParametersParameterAccessor)
		 */
		@Override
		protected JpaQueryCreator createCreator(ParametersParameterAccessor accessor) {

			EntityManager entityManager = getEntityManager();
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			PersistenceProvider persistenceProvider = PersistenceProvider.fromEntityManager(entityManager);

			ParameterMetadataProvider provider = accessor == null ? new ParameterMetadataProvider(builder, parameters,
					persistenceProvider) : new ParameterMetadataProvider(builder, accessor, persistenceProvider);

			return new JpaCountQueryCreator(tree, domainClass, builder, provider);
		}

		/**
		 * Customizes binding by skipping the pagination.
		 * 
		 * @see org.springframework.data.jpa.repository.query.PartTreeJpaQuery.QueryPreparer#invokeBinding(org.springframework.data.jpa.repository.query.ParameterBinder,
		 *      javax.persistence.TypedQuery)
		 */
		@Override
		protected Query invokeBinding(ParameterBinder binder, javax.persistence.TypedQuery<?> query) {
			return binder.bind(query);
		}
	}
}
