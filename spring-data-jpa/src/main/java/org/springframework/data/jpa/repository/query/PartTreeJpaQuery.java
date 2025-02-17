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
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.Tuple;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaQuery;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.JpaParameters.JpaParameter;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.DeleteExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ExistsExecution;
import org.springframework.data.jpa.repository.query.JpaQueryExecution.ScrollExecution;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.ResultProcessor;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;
import org.springframework.util.Assert;

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

	private static final Logger log = LoggerFactory.getLogger(PartTreeJpaQuery.class);
	private final JpqlQueryTemplates templates = JpqlQueryTemplates.UPPER;

	private final PartTree tree;
	private final JpaParameters parameters;

	private final QueryPreparer queryPreparer;
	private final QueryPreparer countQuery;
	private final EntityManager em;
	private final EscapeCharacter escape;
	private final JpaMetamodelEntityInformation<?, Object> entityInformation;

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
		this.parameters = method.getParameters();

		Class<?> domainClass = method.getEntityInformation().getJavaType();
		PersistenceUnitUtil persistenceUnitUtil = em.getEntityManagerFactory().getPersistenceUnitUtil();
		this.entityInformation = new JpaMetamodelEntityInformation<>(domainClass, em.getMetamodel(), persistenceUnitUtil);

		try {

			this.tree = new PartTree(method.getName(), domainClass);
			validate(tree, parameters, method.toString());
			this.countQuery = new CountQueryPreparer();
			this.queryPreparer = tree.isCountProjection() ? countQuery : new QueryPreparer();

		} catch (Exception o_O) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s; %s", method, o_O.getMessage()), o_O);
		}
	}

	@Override
	public Query doCreateQuery(JpaParametersParameterAccessor accessor) {
		return queryPreparer.createQuery(accessor);
	}

	@Override
	@SuppressWarnings("unchecked")
	public TypedQuery<Long> doCreateCountQuery(JpaParametersParameterAccessor accessor) {
		return (TypedQuery<Long>) countQuery.createQuery(accessor);
	}

	@Override
	protected JpaQueryExecution getExecution() {

		if (this.getQueryMethod().isScrollQuery()) {
			return new ScrollExecution(this.tree.getSort(), new ScrollDelegate<>(entityInformation));
		} else if (this.tree.isDelete()) {
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

		if (expectsCollection(type)) {
			if (!parameterIsCollectionLike(parameter)) {
				throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "Collection", parameter));
			}
		} else {
			if (!part.getProperty().isCollection() && !parameterIsScalarLike(parameter)) {
				throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "scalar", parameter));
			}
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

		private final PartTreeQueryCache cache = new PartTreeQueryCache();

		/**
		 * Creates a new {@link Query} for the given parameter values.
		 */
		public Query createQuery(JpaParametersParameterAccessor accessor) {

			Sort sort = getDynamicSort(accessor);
			JpqlQueryCreator creator = createCreator(sort, accessor);
			String jpql = creator.createQuery(sort);
			Query query;

			if (log.isDebugEnabled()) {
				log.debug(String.format("%s: Derived query for query method [%s]: '%s'", getClass().getSimpleName(),
						getQueryMethod(), jpql));
			}

			try {
				query = creator.useTupleQuery() ? em.createQuery(jpql, Tuple.class) : em.createQuery(jpql);
			} catch (Exception e) {
				throw new BadJpqlGrammarException(e.getMessage(), jpql, e);
			}

			ParameterBinder binder = creator.getBinder();

			ScrollPosition scrollPosition = accessor.getParameters().hasScrollPositionParameter()
					? accessor.getScrollPosition()
					: null;
			return restrictMaxResultsIfNecessary(invokeBinding(binder, query, accessor), scrollPosition);
		}

		/**
		 * Restricts the max results of the given {@link Query} if the current {@code tree} marks this {@code query} as
		 * limited.
		 */
		@SuppressWarnings({ "ConstantConditions", "NullAway" })
		private Query restrictMaxResultsIfNecessary(Query query, @Nullable ScrollPosition scrollPosition) {

			if (scrollPosition instanceof OffsetScrollPosition offset && !offset.isInitial()) {
				query.setFirstResult(Math.toIntExact(offset.getOffset()) + 1);
			}

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

		protected JpqlQueryCreator createCreator(Sort sort, JpaParametersParameterAccessor accessor) {

			JpqlQueryCreator jpqlQueryCreator = cache.get(sort, accessor); // this caching thingy is broken due to IS NULL
																																			// rendering for
			if (jpqlQueryCreator != null) {
				return jpqlQueryCreator;
			}

			EntityManager entityManager = getEntityManager();
			ResultProcessor processor = getQueryMethod().getResultProcessor();

			ParameterMetadataProvider provider = new ParameterMetadataProvider(accessor, escape, templates);
			ReturnedType returnedType = processor.withDynamicProjection(accessor).getReturnedType();

			if (accessor.getScrollPosition() instanceof KeysetScrollPosition keyset) {
				return new JpaKeysetScrollQueryCreator(tree, returnedType, provider, templates, entityInformation, keyset,
						entityManager);
			}

			JpqlQueryCreator creator = new CacheableJpqlQueryCreator(sort,
					new JpaQueryCreator(tree, returnedType, provider, templates, em));

			if (accessor.getParameters().hasDynamicProjection()) {
				return creator;
			}

			cache.put(sort, accessor, creator);

			return creator;
		}

		static class CacheableJpqlQueryCreator implements JpqlQueryCreator {

			private final Sort expectedSort;
			private final String query;
			private final boolean useTupleQuery;
			private final List<ParameterBinding> parameterBindings;
			private final ParameterBinder binder;

			public CacheableJpqlQueryCreator(Sort expectedSort, JpqlQueryCreator delegate) {

				this.expectedSort = expectedSort;
				this.query = delegate.createQuery(expectedSort);
				this.useTupleQuery = delegate.useTupleQuery();
				this.parameterBindings = delegate.getBindings();
				this.binder = delegate.getBinder();
			}

			@Override
			public boolean useTupleQuery() {
				return useTupleQuery;
			}

			@Override
			public String createQuery(Sort sort) {

				Assert.isTrue(sort.equals(expectedSort), "Expected sort does not match");
				return query;
			}

			@Override
			public List<ParameterBinding> getBindings() {
				return parameterBindings;
			}

			@Override
			public ParameterBinder getBinder() {
				return binder;
			}
		}

		/**
		 * Invokes parameter binding on the given {@link TypedQuery}.
		 */
		protected Query invokeBinding(ParameterBinder binder, Query query, JpaParametersParameterAccessor accessor) {
			return binder.bindAndPrepare(query, accessor);
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

		private final PartTreeQueryCache cache = new PartTreeQueryCache();

		@Override
		protected JpqlQueryCreator createCreator(Sort sort, JpaParametersParameterAccessor accessor) {

			JpqlQueryCreator cached = cache.get(Sort.unsorted(), accessor);
			if (cached != null) {
				return cached;
			}

			ParameterMetadataProvider provider = new ParameterMetadataProvider(accessor, escape, templates);
			JpaCountQueryCreator creator = new JpaCountQueryCreator(tree,
					getQueryMethod().getResultProcessor().getReturnedType(), provider, templates, em);

			if (!accessor.getParameters().hasDynamicProjection()) {
				cached = new CacheableJpqlCountQueryCreator(creator);
				cache.put(Sort.unsorted(), accessor, cached);
				return cached;
			}

			return creator;
		}

		/**
		 * Customizes binding by skipping the pagination.
		 */
		@Override
		protected Query invokeBinding(ParameterBinder binder, Query query, JpaParametersParameterAccessor accessor) {
			return binder.bind(query, accessor);
		}

		static class CacheableJpqlCountQueryCreator implements JpqlQueryCreator {

			private final String query;
			private final boolean useTupleQuery;
			private final List<ParameterBinding> parameterBindings;
			private final ParameterBinder binder;

			public CacheableJpqlCountQueryCreator(JpqlQueryCreator delegate) {

				this.query = delegate.createQuery(Sort.unsorted());
				this.useTupleQuery = delegate.useTupleQuery();
				this.parameterBindings = delegate.getBindings();
				this.binder = delegate.getBinder();
			}

			@Override
			public boolean useTupleQuery() {
				return useTupleQuery;
			}

			@Override
			public String createQuery(Sort sort) {
				return query;
			}

			@Override
			public List<ParameterBinding> getBindings() {
				return parameterBindings;
			}

			@Override
			public ParameterBinder getBinder() {
				return binder;
			}
		}
	}
}
