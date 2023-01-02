/*
 * Copyright 2008-2023 the original author or authors.
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

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.DeleteExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ExistsExecution;
import org.springframework.data.jpa.repository.query.ParameterMetadataProvider.ParameterMetadata;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;

/**
 * A {@link AbstractJpaQuery} implementation based on a {@link PartTree}.
 *
 * @author Oliver Gierke
 * @author Thomas Darimont
 * @author Christoph Strobl
 * @author Jens Schauder
 * @author Mark Paluch
 * @author Сергей Цыпанов
 */
public class PartTreeJpaQuery extends AbstractJpaQuery {

	private final PartTree tree;
	private final JpaParameters parameters;

	private final QueryPreparer query;
	private final QueryPreparer countQuery;
	private final EntityManager em;
	private final EscapeCharacter escape;

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	PartTreeJpaQuery(JpaQueryMethod method, EntityManager em) {
		this(method, em, EscapeCharacter.DEFAULT);
	}

	/**
	 * Creates a new {@link PartTreeJpaQuery}.
	 *
	 * @param method must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 * @param escape character used for escaping characters used as patterns in LIKE-expressions.
	 */
	PartTreeJpaQuery(JpaQueryMethod method, EntityManager em, EscapeCharacter escape) {

		super(method, em);

		this.em = em;
		this.escape = escape;
		Class<?> domainClass = method.getEntityInformation().getJavaType();
		this.parameters = method.getParameters();

		boolean recreationRequired = parameters.hasDynamicProjection() || parameters.potentiallySortsDynamically();

		try {

			this.tree = new PartTree(method.getName(), domainClass);
			validate(tree, parameters, method.toString());
			this.countQuery = new CountQueryPreparer(recreationRequired);
			this.query = tree.isCountProjection() ? countQuery : new QueryPreparer(recreationRequired);

		} catch (Exception o_O) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s; %s", method, o_O.getMessage()), o_O);
		}
	}

	@Override
	public Query doCreateQuery(JpaParametersParameterAccessor accessor) {
		return query.createQuery(accessor);
	}

	@Override
	@SuppressWarnings("unchecked")
	public TypedQuery<Long> doCreateCountQuery(JpaParametersParameterAccessor accessor) {
		return (TypedQuery<Long>) countQuery.createQuery(accessor);
	}

	@Override
	protected JpaQueryExecution getExecution() {

		if (this.tree.isDelete()) {
			return new DeleteExecution(em);
		} else if (this.tree.isExistsProjection()) {
			return new ExistsExecution();
		}

		return super.getExecution();
	}

	private static void validate(PartTree tree, JpaParameters parameters, String methodName) {

		int argCount = 0;

		Iterable<Part> parts = () -> tree.stream().flatMap(Streamable::stream).iterator();

		for (Part part : parts) {

			int numberOfArguments = part.getNumberOfArguments();

			for (int i = 0; i < numberOfArguments; i++) {

				throwExceptionOnArgumentMismatch(methodName, part, parameters, argCount);

				argCount++;
			}
		}
	}

	private static void throwExceptionOnArgumentMismatch(String methodName, Part part, JpaParameters parameters,
			int index) {

		Type type = part.getType();
		String property = part.getProperty().toDotPath();

		if (!parameters.getBindableParameters().hasParameterAt(index)) {
			throw new IllegalStateException(String.format(
					"Method %s expects at least %d arguments but only found %d; This leaves an operator of type %s for property %s unbound",
					methodName, index + 1, index, type.name(), property));
		}

		JpaParameter parameter = parameters.getBindableParameter(index);

		if (expectsCollection(type) && !parameterIsCollectionLike(parameter)) {
			throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "Collection", parameter));
		} else if (!expectsCollection(type) && !parameterIsScalarLike(parameter)) {
			throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "scalar", parameter));
		}
	}

	private static String wrongParameterTypeMessage(String methodName, String property, Type operatorType,
			String expectedArgumentType, JpaParameter parameter) {

		return String.format("Operator %s on %s requires a %s argument, found %s in method %s", operatorType.name(),
				property, expectedArgumentType, parameter.getType(), methodName);
	}

	private static boolean parameterIsCollectionLike(JpaParameter parameter) {
		return Iterable.class.isAssignableFrom(parameter.getType()) || parameter.getType().isArray();
	}

	/**
	 * Arrays are may be treated as collection like or in the case of binary data as scalar
	 */
	private static boolean parameterIsScalarLike(JpaParameter parameter) {
		return !Iterable.class.isAssignableFrom(parameter.getType());
	}

	private static boolean expectsCollection(Type type) {
		return type == Type.IN || type == Type.NOT_IN;
	}

	/**
	 * Query preparer to create {@link CriteriaQuery} instances and potentially cache them.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private class QueryPreparer {

		private final @Nullable CriteriaQuery<?> cachedCriteriaQuery;
		private final @Nullable ParameterBinder cachedParameterBinder;
		private final QueryParameterSetter.QueryMetadataCache metadataCache = new QueryParameterSetter.QueryMetadataCache();

		QueryPreparer(boolean recreateQueries) {

			JpaQueryCreator creator = createCreator(null);

			if (recreateQueries) {
				this.cachedCriteriaQuery = null;
				this.cachedParameterBinder = null;
			} else {
				this.cachedCriteriaQuery = creator.createQuery();
				this.cachedParameterBinder = getBinder(creator.getParameterExpressions());
			}
		}

		/**
		 * Creates a new {@link Query} for the given parameter values.
		 */
		public Query createQuery(JpaParametersParameterAccessor accessor) {

			CriteriaQuery<?> criteriaQuery = cachedCriteriaQuery;
			ParameterBinder parameterBinder = cachedParameterBinder;

			if (cachedCriteriaQuery == null || accessor.hasBindableNullValue()) {
				JpaQueryCreator creator = createCreator(accessor);
				criteriaQuery = creator.createQuery(getDynamicSort(accessor));
				List<ParameterMetadata<?>> expressions = creator.getParameterExpressions();
				parameterBinder = getBinder(expressions);
			}

			if (parameterBinder == null) {
				throw new IllegalStateException("ParameterBinder is null");
			}

			TypedQuery<?> query = createQuery(criteriaQuery);

			return restrictMaxResultsIfNecessary(invokeBinding(parameterBinder, query, accessor, this.metadataCache));
		}

		/**
		 * Restricts the max results of the given {@link Query} if the current {@code tree} marks this {@code query} as
		 * limited.
		 */
		@SuppressWarnings("ConstantConditions")
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

			if (tree.isExistsProjection()) {
				query.setMaxResults(1);
			}

			return query;
		}

		/**
		 * Checks whether we are working with a cached {@link CriteriaQuery} and synchronizes the creation of a
		 * {@link TypedQuery} instance from it. This is due to non-thread-safety in the {@link CriteriaQuery} implementation
		 * of some persistence providers (i.e. Hibernate in this case), see DATAJPA-396.
		 *
		 * @param criteriaQuery must not be {@literal null}.
		 */
		private TypedQuery<?> createQuery(CriteriaQuery<?> criteriaQuery) {

			if (this.cachedCriteriaQuery != null) {
				synchronized (this.cachedCriteriaQuery) {
					return getEntityManager().createQuery(criteriaQuery);
				}
			}

			return getEntityManager().createQuery(criteriaQuery);
		}

		protected JpaQueryCreator createCreator(@Nullable JpaParametersParameterAccessor accessor) {

			EntityManager entityManager = getEntityManager();

			CriteriaBuilder builder = entityManager.getCriteriaBuilder();
			ResultProcessor processor = getQueryMethod().getResultProcessor();

			ParameterMetadataProvider provider;
			ReturnedType returnedType;

			if (accessor != null) {
				provider = new ParameterMetadataProvider(builder, accessor, escape);
				returnedType = processor.withDynamicProjection(accessor).getReturnedType();
			} else {
				provider = new ParameterMetadataProvider(builder, parameters, escape);
				returnedType = processor.getReturnedType();
			}

			return new JpaQueryCreator(tree, returnedType, builder, provider);
		}

		/**
		 * Invokes parameter binding on the given {@link TypedQuery}.
		 */
		protected Query invokeBinding(ParameterBinder binder, TypedQuery<?> query, JpaParametersParameterAccessor accessor,
				QueryParameterSetter.QueryMetadataCache metadataCache) {

			QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata("query", query);

			return binder.bindAndPrepare(query, metadata, accessor);
		}

		private ParameterBinder getBinder(List<ParameterMetadata<?>> expressions) {
			return ParameterBinderFactory.createCriteriaBinder(parameters, expressions);
		}

		private Sort getDynamicSort(JpaParametersParameterAccessor accessor) {

			return parameters.potentiallySortsDynamically() //
					? accessor.getSort() //
					: Sort.unsorted();
		}
	}

	/**
	 * Special {@link QueryPreparer} to create count queries.
	 *
	 * @author Oliver Gierke
	 * @author Thomas Darimont
	 */
	private class CountQueryPreparer extends QueryPreparer {

		CountQueryPreparer(boolean recreateQueries) {
			super(recreateQueries);
		}

		@Override
		protected JpaQueryCreator createCreator(@Nullable JpaParametersParameterAccessor accessor) {

			EntityManager entityManager = getEntityManager();
			CriteriaBuilder builder = entityManager.getCriteriaBuilder();

			ParameterMetadataProvider provider;

			if (accessor != null) {
				provider = new ParameterMetadataProvider(builder, accessor, escape);
			} else {
				provider = new ParameterMetadataProvider(builder, parameters, escape);
			}

			return new JpaCountQueryCreator(tree, getQueryMethod().getResultProcessor().getReturnedType(), builder, provider);
		}

		/**
		 * Customizes binding by skipping the pagination.
		 */
		@Override
		protected Query invokeBinding(ParameterBinder binder, TypedQuery<?> query, JpaParametersParameterAccessor accessor,
				QueryParameterSetter.QueryMetadataCache metadataCache) {

			QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata("countquery", query);

			return binder.bind(query, metadata, accessor);
		}
	}
}
