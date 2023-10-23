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

import static org.springframework.data.jpa.repository.query.ExpressionContext.*;
import static org.springframework.data.jpa.repository.query.QueryUtils.*;
import static org.springframework.data.repository.query.parser.Part.Type.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.ManagedType;
import jakarta.persistence.metamodel.PluralAttribute;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.data.domain.KeysetScrollPosition;
import org.springframework.data.domain.OffsetScrollPosition;
import org.springframework.data.domain.ScrollPosition;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.query.ParameterMetadataContextProvider.ParameterImpl;
import org.springframework.data.jpa.repository.query.ParameterMetadataContextProvider.ParameterMetadata;
import org.springframework.data.jpa.repository.support.JpaMetamodelEntityInformation;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.data.util.Streamable;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * An {@link AbstractJpaQueryContext} used to handle custom finders.
 *
 * @author Greg Turnquist
 */
class CustomFinderQueryContext extends AbstractJpaQueryContext {

	private static final Log LOG = LogFactory.getLog(CustomFinderQueryContext.class);

	private final JpaParameters parameters;
	private final PartTree tree;
	private final JpaMetamodelEntityInformation<?, Object> entityInformation;
	private final EscapeCharacter escape;

	private QueryCreator queryCreator;

	CustomFinderQueryContext(JpaQueryMethod method, EntityManager entityManager, EscapeCharacter escape) {

		super(Optional.of(method), entityManager);

		this.escape = escape;
		this.parameters = method.getParameters();
		this.entityInformation = new JpaMetamodelEntityInformation<>(//
				method.getEntityInformation().getJavaType(), //
				entityManager.getMetamodel(), //
				entityManager.getEntityManagerFactory().getPersistenceUnitUtil());

		try {

			this.tree = new PartTree(method.getName(), method.getEntityInformation().getJavaType());

			validate(tree, parameters, method.getName());

		} catch (Exception ex) {
			throw new IllegalArgumentException(
					String.format("Failed to create query for method %s; %s", method, ex.getMessage()), ex);
		}
	}

	public PartTree getTree() {
		return this.tree;
	}

	public JpaMetamodelEntityInformation<?, Object> getEntityInformation() {
		return entityInformation;
	}

	@Override
	protected ContextualQuery createQuery(JpaParametersParameterAccessor accessor) {

		ParameterMetadataContextProvider provider;

		if (accessor != null) {
			provider = new ParameterMetadataContextProvider(accessor, escape);
		} else {
			provider = new ParameterMetadataContextProvider(parameters, escape);
		}

		if (accessor != null && accessor.getScrollPosition() instanceof KeysetScrollPosition keyset) {
			this.queryCreator = new KeysetScrollQueryCreator(accessor, provider, tree.isCountProjection(), keyset);
		} else {
			this.queryCreator = new QueryCreator(accessor, provider, tree.isCountProjection());
		}

		Sort dynamicSort = getDynamicSort(accessor);

		String query = queryCreator.createQuery(dynamicSort);

		LOG.debug(queryMethod().getName() + ": " + query);

		System.out.println(query);

		return ContextualQuery.of(query);
	}

	private Sort getDynamicSort(JpaParametersParameterAccessor accessor) {

		return accessor.getParameters().potentiallySortsDynamically() //
				? accessor.getSort() //
				: Sort.unsorted();
	}

	@Override
	protected Query turnIntoJpaQuery(ContextualQuery query, JpaParametersParameterAccessor accessor) {

		ReturnedType returnedType = queryMethod().getResultProcessor().withDynamicProjection(accessor).getReturnedType();

		Query jpaQuery;

		if (returnedType.needsCustomConstruction()) {

			Class<?> typeToRead = returnedType.getReturnedType();

			if (typeToRead.isInterface()) {
				jpaQuery = getEntityManager().createQuery(query.getQuery(), typeToRead);
			} else {

				String selections = returnedType.getInputProperties().stream() //
						.map(property -> PropertyPath.from(property, returnedType.getDomainType())) //
						.map(path -> alias(returnedType.getDomainType()) + "." + path.toDotPath() + " as " + path.toDotPath())//
						.collect(Collectors.joining(","));

				String classBasedDtoQuery = query.getQuery().replace("select " + alias(returnedType.getDomainType()),
						"select " + selections);

				LOG.debug(queryMethod().getName() + ": Switching to classbased DTO query " + classBasedDtoQuery);
				System.out.println("Switching to " + classBasedDtoQuery);

				jpaQuery = getEntityManager().createQuery(classBasedDtoQuery, typeToRead);
			}

		} else if (tree.isExistsProjection()) {

			EntityType<?> model = getEntityManager().getMetamodel().entity(returnedType.getDomainType());

			if (model.hasSingleIdAttribute()) {

				SingularAttribute<?, ?> id = model.getId(model.getIdType().getJavaType());

				String selections = String.format("%s.%s as %s", alias(returnedType.getDomainType()), id.getName(), id.getName());
				String existsQuery = query.getQuery().replace("select " + alias(returnedType.getDomainType()),
						"select " + selections);

				LOG.debug(queryMethod().getName() + ": Switching to " + existsQuery);
				System.out.println("Switching to " + existsQuery);

				jpaQuery = getEntityManager().createQuery(existsQuery);
			} else {

				String selections = model.getIdClassAttributes().stream() //
						.map(id -> String.format("%s.%s as %s", alias(returnedType.getDomainType()), id.getName(), id.getName())) //
						.collect(Collectors.joining(","));

				String existsQuery = query.getQuery().replace("select " + alias(returnedType.getDomainType()),
						"select " + selections);

				LOG.debug(queryMethod().getName() + ": Switching to " + existsQuery);
				System.out.println("Switching to " + existsQuery);

				jpaQuery = getEntityManager().createQuery(existsQuery);
			}

		} else {
			jpaQuery = getEntityManager().createQuery(query.getQuery());
		}

		return jpaQuery;
	}

	@Override
	protected Class<?> getTypeToRead(ReturnedType type) {
		return tree.isDelete() ? type.getDomainType() : type.getTypeToRead();
	}

	@Override
	public Query createJpaCountQuery(JpaParametersParameterAccessor accessor) {

		QueryCreator countQueryCreator;

		if (accessor != null) {

			ParameterMetadataContextProvider provider = new ParameterMetadataContextProvider(accessor, escape);
			countQueryCreator = new QueryCreator(accessor, provider, true);
		} else {

			ParameterMetadataContextProvider provider = new ParameterMetadataContextProvider(parameters, escape);
			countQueryCreator = new QueryCreator(null, provider, true);
		}

		String countQuery = countQueryCreator.createQuery();

		System.out.println(countQuery);

		TypedQuery<Long> jpaCountQuery = getEntityManager().createQuery(countQuery, Long.class);

		Query boundCountQuery = bindParameters(jpaCountQuery, accessor);

		return boundCountQuery;
	}

	@Override
	protected Query bindParameters(Query query, JpaParametersParameterAccessor accessor) {

		QueryParameterSetter.QueryMetadata metadata = metadataCache.getMetadata("query", query);

		ParameterBinder binder = ParameterBinderFactory.oneFlowBinder((JpaParameters) accessor.getParameters(),
				queryCreator.provider.getExpressions());

		if (binder == null) {
			throw new IllegalStateException("ParameterBinder is null");
		}

		Query boundQuery = binder.bindAndPrepare(query, metadata, accessor);

		ScrollPosition scrollPosition = accessor.getParameters().hasScrollPositionParameter()//
				? accessor.getScrollPosition() //
				: null;

		if (scrollPosition instanceof OffsetScrollPosition offset) {
			boundQuery.setFirstResult(Math.toIntExact(offset.getOffset()));
		}

		if (tree.isLimiting()) {
			if (boundQuery.getMaxResults() != Integer.MAX_VALUE) {
				/*
				 * In order to return the correct results, we have to adjust the first result offset to be returned if:
				 * - a Pageable parameter is present
				 * - AND the requested page number > 0
				 * - AND the requested page size was bigger than the derived result limitation via the First/Top keyword.
				 */
				if (boundQuery.getMaxResults() > tree.getMaxResults() && boundQuery.getFirstResult() > 0) {
					boundQuery.setFirstResult(boundQuery.getFirstResult() - (boundQuery.getMaxResults() - tree.getMaxResults()));
				}
			}

			boundQuery.setMaxResults(tree.getMaxResults());
		}

		if (tree.isExistsProjection()) {
			boundQuery.setMaxResults(1);
		}

		return boundQuery;
	}

	private void validate(PartTree tree, JpaParameters parameters, String methodName) {

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

		Part.Type type = part.getType();
		String property = part.getProperty().toDotPath();

		if (!parameters.getBindableParameters().hasParameterAt(index)) {
			throw new IllegalStateException(String.format(
					"Method %s expects at least %d arguments but only found %d; This leaves an operator of type %s for property %s unbound",
					methodName, index + 1, index, type.name(), property));
		}

		JpaParameters.JpaParameter parameter = parameters.getBindableParameter(index);

		if (expectsCollection(type) && !parameterIsCollectionLike(parameter)) {
			throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "Collection", parameter));
		} else if (!expectsCollection(type) && !parameterIsScalarLike(parameter)) {
			throw new IllegalStateException(wrongParameterTypeMessage(methodName, property, type, "scalar", parameter));
		}
	}

	private static boolean expectsCollection(Part.Type type) {
		return type == Part.Type.IN || type == Part.Type.NOT_IN;
	}

	private static boolean parameterIsCollectionLike(JpaParameters.JpaParameter parameter) {
		return Iterable.class.isAssignableFrom(parameter.getType()) || parameter.getType().isArray();
	}

	private static String wrongParameterTypeMessage(String methodName, String property, Part.Type operatorType,
			String expectedArgumentType, JpaParameters.JpaParameter parameter) {

		return String.format("Operator %s on %s requires a %s argument, found %s in method %s", operatorType.name(),
				property, expectedArgumentType, parameter.getType(), methodName);
	}

	/**
	 * Arrays may be treated as collection-like or in the case of binary data as scalar
	 */
	private static boolean parameterIsScalarLike(JpaParameters.JpaParameter parameter) {
		return !Iterable.class.isAssignableFrom(parameter.getType());
	}

	private class QueryCreator extends AbstractQueryCreator<String, ExpressionContext> {

		private final ParameterMetadataContextProvider provider;
		protected final ReturnedType returnedType;
		private final EntityType<?> model;
		private final boolean countQuery;

		public QueryCreator(JpaParametersParameterAccessor accessor, ParameterMetadataContextProvider provider,
				boolean countQuery) {

			super(tree, accessor);

			this.provider = provider;
			this.returnedType = queryMethod().getResultProcessor().withDynamicProjection(accessor).getReturnedType();
			this.model = getEntityManager().getMetamodel().entity(returnedType.getDomainType());
			this.countQuery = countQuery;
		}

		@Override
		protected ExpressionContext create(Part part, Iterator<Object> iterator) {
			return toExpressionContext(part);
		}

		@Override
		protected ExpressionContext and(Part part, ExpressionContext base, Iterator<Object> iterator) {
			return new AndExpressionContext(List.of(base, toExpressionContext(part)));
		}

		@Override
		protected ExpressionContext or(ExpressionContext base, ExpressionContext predicate) {
			return new OrExpressionContext(List.of(base, predicate));
		}

		@Override
		protected String complete(@Nullable ExpressionContext expression, @Nullable Sort sort) {

			String alias = alias(returnedType.getDomainType());
			String entityName = entityName(returnedType.getDomainType());
			String queryTemplate = !countQuery ? "select %s from %s %s" : "select count(%s) from %s %s";
			String query = String.format(queryTemplate, alias, entityName, alias);

			if (expression != null) {

				String joins = expression.joins(alias);
				query += joins.isEmpty() ? "" : " " + joins;

				String criteria = expression.criteria(alias);
				query += criteria.isEmpty() ? "" : " where " + criteria;
			}

			if (!countQuery && sort != null && sort.isSorted()) {
				query += " order by " + String.join(",", QueryUtils.toOrders(sort, returnedType.getDomainType()));
			}

			LOG.debug(query);

			return query;

		}

		private ExpressionContext toExpressionContext(Part part) {
			return new ExpressionContextBuilder(part, provider, model).build();
		}
	}

	private class KeysetScrollQueryCreator extends QueryCreator { // TODO: Get Keyset-based scrolling working!

		private final KeysetScrollPosition scrollPosition;

		public KeysetScrollQueryCreator(JpaParametersParameterAccessor accessor, ParameterMetadataContextProvider provider,
				boolean countQuery, KeysetScrollPosition scrollPosition) {

			super(accessor, provider, countQuery);
			this.scrollPosition = scrollPosition;
		}

		@Override
		protected String complete(ExpressionContext expression, Sort sort) {

			String queryToUse = super.complete(expression, sort);

			// TODO: Apply additional clause regarding keyset

			return queryToUse;
		}
	}

	private class ExpressionContextBuilder {

		private final Part part;
		private final EntityType<?> model;
		private final ParameterMetadataContextProvider provider;

		public ExpressionContextBuilder(Part part, ParameterMetadataContextProvider provider, EntityType<?> model) {

			Assert.notNull(part, "Part must not be null");
			Assert.notNull(provider, "Provider must not be null");

			this.part = part;
			this.provider = provider;
			this.model = model;
		}

		public ExpressionContext build() {

			PropertyPath property = part.getProperty();
			Part.Type type = part.getType();

			String simpleAlias = alias(property.getOwningType().getType());

			switch (type) {
				case BETWEEN:
					ParameterMetadata<Object> first = provider.next(part);
					ParameterMetadata<Object> second = provider.next(part);
					return between(getComparablePath(part), first.getValue(), second.getValue());
				case AFTER:
				case GREATER_THAN:
					return comparison(getComparablePath(part), ">", provider.next(part, Comparable.class).getValue());
				case GREATER_THAN_EQUAL:
					return comparison(getComparablePath(part), ">=", provider.next(part, Comparable.class).getValue());
				case BEFORE:
				case LESS_THAN:
					return comparison(getComparablePath(part), "<", provider.next(part, Comparable.class).getValue());
				case LESS_THAN_EQUAL:
					return comparison(getComparablePath(part), "<=", provider.next(part, Comparable.class).getValue());
				case IS_NULL:
					return isNull(getTypedPath(part));
				case IS_NOT_NULL:
					return isNotNull(getTypedPath(part));
				case NOT_IN:
					return notIn(upperIfIgnoreCase(getTypedPath(part)), provider.next(part, Collection.class).getValue());
				case IN:
					return in(upperIfIgnoreCase(getTypedPath(part)), provider.next(part, Collection.class).getValue());
				case STARTING_WITH:
				case ENDING_WITH:
				case CONTAINING:
				case NOT_CONTAINING:

					if (property.getLeafProperty().isCollection()) {

						String propertyExpression = traversePath(simpleAlias, property);
						ParameterImpl<Object> parameterExpression = provider.next(part).getExpression();

						return type.equals(NOT_CONTAINING) //
								? isNotMember(parameterExpression, propertyExpression) //
								: isMember(parameterExpression, propertyExpression);
					}

				case LIKE:
				case NOT_LIKE:

					ExpressionContext propertyExpression = upperIfIgnoreCase(getTypedPath(part));
					ExpressionContext parameterExpression = upperIfIgnoreCase(provider.next(part, String.class).getValue());

					return type.equals(NOT_LIKE) || type.equals(NOT_CONTAINING) //
							? notLike(propertyExpression, parameterExpression)
							: like(propertyExpression, parameterExpression);
				case TRUE:
					return isTrue(getTypedPath(part));
				case FALSE:
					return isFalse(getTypedPath(part));
				case SIMPLE_PROPERTY:

					ParameterMetadata<Object> expression = provider.next(part);

					return expression.isIsNullParameter() //
							? isNull(getTypedPath(part)) //
							: equal(upperIfIgnoreCase(getTypedPath(part)), upperIfIgnoreCase(expression.getValue()));

				case NEGATING_SIMPLE_PROPERTY:
					return notEqual(upperIfIgnoreCase(getTypedPath(part)), upperIfIgnoreCase(provider.next(part).getValue()));
				case IS_EMPTY:
				case IS_NOT_EMPTY:

					if (!property.getLeafProperty().isCollection()) {
						throw new IllegalArgumentException("IsEmpty / IsNotEmpty can only be used on collection properties");
					}

					String collectionPath = traversePath("", property);

					return type.equals(IS_NOT_EMPTY) //
							? isNotEmpty(collectionPath) //
							: isEmpty(collectionPath);
				default:
					throw new IllegalArgumentException("Unsupported keyword " + type);
			}
		}

		private ExpressionContext upperIfIgnoreCase(String value) {
			return upperIfIgnoreCase(new ValueBasedExpressionContext(value));
		}

		private ExpressionContext upperIfIgnoreCase(ExpressionContext expression) {

			switch (part.shouldIgnoreCase()) {

				case ALWAYS:

					Assert.state(canUpperCase(part.getProperty()),
							"Unable to ignore case of " + part.getProperty().getType().getName() + " types, the property '"
									+ part.getProperty().getSegment() + "' must reference a String");

					return upper(expression);

				case WHEN_POSSIBLE:

					if (canUpperCase(part.getProperty())) {
						return upper(expression);
					}

				case NEVER:
				default:
					return expression;
			}
		}

		private boolean canUpperCase(PropertyPath property) {
			return String.class.equals(property.getType());
		}

		private ExpressionContext getComparablePath(Part part) {
			return getTypedPath(part);
		}

		private ExpressionContext getTypedPath(Part part) {
			return toExpression(model, new PropertyBasedExpressionContext(part.getProperty()), part.getProperty());
		}

		private String traversePath(String totalPath, PropertyPath path) {

			String result = totalPath.isEmpty() ? path.getSegment() : totalPath + "." + path.getSegment();

			return path.hasNext() //
					? traversePath(result, path.next()) //
					: result;
		}

		private ExpressionContext toExpression(EntityType<?> model, ExpressionContext expression, PropertyPath property) {
			return toExpression(model, expression, property, false, false);
		}

		private ExpressionContext toExpression(EntityType<?> model, ExpressionContext from, PropertyPath property,
				boolean isForSelection, boolean hasRequiredOuterJoin) {

			String segment = property.getSegment();

			boolean isLeafProperty = !property.hasNext();

			boolean requiresOuterJoin = requiresOuterJoin(from, model, property, isForSelection, hasRequiredOuterJoin);

			if (!requiresOuterJoin && isLeafProperty) {
				return from.get(segment);
			}

			ExpressionContext join = requiresOuterJoin //
					? from.join(new OuterJoin(segment)) //
					: from.join(new InnerJoin(segment));

			if (isLeafProperty) {
				return join;
			}

			PropertyPath nextProperty = Objects.requireNonNull(property.next(), "An element of the property path is null");

			return toExpression(model, join, nextProperty, isForSelection, requiresOuterJoin);
		}

		private static boolean requiresOuterJoin(ExpressionContext from, EntityType<?> model, PropertyPath property,
				boolean isForSelection, boolean hasRequiredOuterJoin) {

			String segment = property.getSegment();

			if (isAlreadyInnerJoined(from, segment)) {
				return false;
			}

			ManagedType<?> managedType = model;
			Bindable<?> propertyPathModel = (Bindable<?>) managedType.getAttribute(segment);

			boolean isPluralAttribute = model instanceof PluralAttribute;
			boolean isLeafProperty = !property.hasNext();

			if (propertyPathModel == null && isPluralAttribute) {
				return true;
			}

			if (!(propertyPathModel instanceof Attribute<?, ?> attribute)) {
				return false;
			}

			// not a persistent attribute type association (@OneToOne, @ManyToOne)
			if (!ASSOCIATION_TYPES.containsKey(attribute.getPersistentAttributeType())) {
				return false;
			}

			boolean isCollection = attribute.isCollection();
			// if this path is an optional one to one attribute navigated from the not owning side we also need an
			// explicit outer join to avoid https://hibernate.atlassian.net/browse/HHH-12712
			// and https://github.com/eclipse-ee4j/jpa-api/issues/170
			boolean isInverseOptionalOneToOne = Attribute.PersistentAttributeType.ONE_TO_ONE == attribute
					.getPersistentAttributeType() && !getAnnotationProperty(attribute, "mappedBy", "").isEmpty();

			if (isLeafProperty && !isForSelection && !isCollection && !isInverseOptionalOneToOne && !hasRequiredOuterJoin) {
				return false;
			}

			return hasRequiredOuterJoin || getAnnotationProperty(attribute, "optional", true);
		}

		private static boolean isAlreadyInnerJoined(ExpressionContext from, String segment) {

			return from.joins().stream() //
					.filter(InnerJoin.class::isInstance) //
					.anyMatch(join -> join.join().equals(segment));
		}
	}
}
