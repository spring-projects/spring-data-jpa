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

import java.util.List;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.jpa.repository.support.JpaCriteriaQueryContext;
import org.springframework.data.repository.augment.QueryAugmentationEngine;
import org.springframework.data.repository.augment.QueryContext.QueryMode;
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

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 * 
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	public PartTreeJpaQuery(JpaQueryMethod method, EntityManager em) {

		super(method, em);

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

	/**
	 * Query preparer to create {@link CriteriaQuery} instances and potentially cache them.
	 * 
	 * @author Oliver Gierke
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
				criteriaQuery = potentiallyAugment(creator.createQuery(getDynamicSort(values)));
				expressions = creator.getParameterExpressions();
			}

			TypedQuery<?> jpaQuery = createQuery(criteriaQuery);
			return invokeBinding(getBinder(values, expressions), jpaQuery);
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

		protected <T> CriteriaQuery<T> potentiallyAugment(CriteriaQuery<T> query) {
			return potentiallyAugment(query, QueryMode.FIND);
		}

		private <T> CriteriaQuery<T> potentiallyAugment(CriteriaQuery<T> query, QueryMode mode) {

			QueryAugmentationEngine engine = getAugmentationEngine();

			if (engine != null
					&& engine.augmentationNeeded(JpaCriteriaQueryContext.class, mode, getQueryMethod().getEntityInformation())) {
				JpaCriteriaQueryContext<T, T> context = new JpaCriteriaQueryContext<T, T>(mode, getEntityManager(), query, null);
				return engine.invokeAugmentors(context).getQuery();
			} else {
				return query;
			}
		}

		protected JpaQueryCreator createCreator(ParametersParameterAccessor accessor) {

			EntityManager entityManager = getEntityManager();
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			ParameterMetadataProvider provider = accessor == null ? new ParameterMetadataProvider(builder, parameters)
					: new ParameterMetadataProvider(builder, accessor);

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

			ParameterMetadataProvider provider = accessor == null ? new ParameterMetadataProvider(builder, parameters)
					: new ParameterMetadataProvider(builder, accessor);

			return new JpaCountQueryCreator(tree, domainClass, builder, provider);
		}

		/* 
		 * (non-Javadoc)
		 * @see org.springframework.data.jpa.repository.query.PartTreeJpaQuery.QueryPreparer#potentiallyAugment(javax.persistence.criteria.CriteriaQuery)
		 */
		@Override
		protected <T> CriteriaQuery<T> potentiallyAugment(CriteriaQuery<T> query) {
			return super.potentiallyAugment(query, QueryMode.COUNT_FOR_PAGING);
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
