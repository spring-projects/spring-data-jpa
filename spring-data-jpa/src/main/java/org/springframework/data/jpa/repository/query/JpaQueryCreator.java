/*
 * Copyright 2008-present the original author or authors.
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

import static org.springframework.data.repository.query.parser.Part.Type.*;

import jakarta.persistence.EntityManager;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.Bindable;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import jakarta.persistence.metamodel.SingularAttribute;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.jspecify.annotations.Nullable;

import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyPath;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.data.domain.Range;
import org.springframework.data.domain.Score;
import org.springframework.data.domain.ScoringFunction;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.VectorScoringFunctions;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.ParameterPlaceholder;
import org.springframework.data.jpa.repository.query.ParameterBinding.PartTreeParameterBinding;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.repository.query.ReturnedType;
import org.springframework.data.repository.query.parser.AbstractQueryCreator;
import org.springframework.data.repository.query.parser.Part;
import org.springframework.data.repository.query.parser.Part.Type;
import org.springframework.data.repository.query.parser.PartTree;
import org.springframework.util.Assert;

/**
 * Query creator to create a {@link CriteriaQuery} from a {@link PartTree}.
 *
 * @author Oliver Gierke
 * @author Mark Paluch
 * @author Michael Cramer
 * @author Mark Paluch
 * @author Reda.Housni-Alaoui
 * @author Moritz Becker
 * @author Andrey Kovalev
 * @author Greg Turnquist
 * @author Christoph Strobl
 * @author Jinmyeong Kim
 * @author Oualid Bouh
 */
public class JpaQueryCreator extends AbstractQueryCreator<String, JpqlQueryBuilder.Predicate>
		implements JpqlQueryCreator {

	private static final Map<ScoringFunction, DistanceFunction> DISTANCE_FUNCTIONS = Map.of(VectorScoringFunctions.COSINE,
			new DistanceFunction("cosine_distance", Sort.Direction.ASC), //
			VectorScoringFunctions.EUCLIDEAN, new DistanceFunction("euclidean_distance", Sort.Direction.ASC), //
			VectorScoringFunctions.TAXICAB, new DistanceFunction("taxicab_distance", Sort.Direction.ASC), //
			VectorScoringFunctions.HAMMING, new DistanceFunction("hamming_distance", Sort.Direction.ASC), //
			VectorScoringFunctions.DOT_PRODUCT, new DistanceFunction("negative_inner_product", Sort.Direction.ASC));

	record DistanceFunction(String distanceFunction, Sort.Direction direction) {

	}

	private final boolean searchQuery;
	private final ReturnedType returnedType;
	private final ParameterMetadataProvider provider;
	private final JpqlQueryTemplates templates;
	private final PartTree tree;
	private final EscapeCharacter escape;
	private final EntityType<?> entityType;
	private final JpqlQueryBuilder.Entity entity;
	private final Metamodel metamodel;
	private final SimilarityNormalizer similarityNormalizer;
	private final boolean useNamedParameters;

	/**
	 * Create a new {@link JpaQueryCreator}.
	 *
	 * @param tree must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param templates must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	public JpaQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, EntityManager em) {
		this(tree, false, type, provider, templates, em.getMetamodel());
	}

	public JpaQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, Metamodel metamodel) {
		this(tree, false, type, provider, templates, metamodel);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	public JpaQueryCreator(PartTree tree, boolean searchQuery, ReturnedType type, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, Metamodel metamodel) {
		this(tree, searchQuery, type, provider, templates,
				new JpaMetamodelEntityMetadata(metamodel.entity(type.getDomainType())), metamodel);
	}

	public JpaQueryCreator(PartTree tree, boolean searchQuery, ReturnedType type, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, JpaEntityMetadata<?> entityMetadata, Metamodel metamodel) {

		super(tree);

		this.searchQuery = searchQuery;
		this.tree = tree;
		this.returnedType = type;
		this.provider = provider;

		JpaParameters bindableParameters = provider.getParameters().getBindableParameters();

		boolean useNamedParameters = false;
		for (JpaParameters.JpaParameter bindableParameter : bindableParameters) {

			if (bindableParameter.isNamedParameter()) {
				useNamedParameters = true;
			}

			if (useNamedParameters && !bindableParameter.isNamedParameter()) {
				useNamedParameters = false;
				break;
			}
		}

		this.useNamedParameters = useNamedParameters;
		this.templates = templates;
		this.escape = provider.getEscape();
		this.entityType = metamodel.entity(type.getDomainType());
		this.entity = JpqlQueryBuilder.entity(entityMetadata);
		this.metamodel = metamodel;
		this.similarityNormalizer = provider.getSimilarityNormalizer();
	}

	Bindable<?> getFrom() {
		return entityType;
	}

	JpqlQueryBuilder.Entity getEntity() {
		return entity;
	}

	public boolean useTupleQuery() {
		return returnedType.needsCustomConstruction() && returnedType.isInterfaceProjection();
	}

	/**
	 * Returns all {@link jakarta.persistence.criteria.ParameterExpression} created when creating the query.
	 *
	 * @return the parameterExpressions
	 */
	public List<ParameterBinding> getBindings() {
		return provider.getBindings();
	}

	@Override
	public ParameterBinder getBinder() {
		return ParameterBinderFactory.createBinder(provider.getParameters(), getBindings());
	}

	@Override
	protected JpqlQueryBuilder.Predicate create(Part part, Iterator<Object> iterator) {
		return toPredicate(part);
	}

	@Override
	protected JpqlQueryBuilder.Predicate and(Part part, JpqlQueryBuilder.Predicate base, Iterator<Object> iterator) {
		return base.and(toPredicate(part));
	}

	@Override
	protected JpqlQueryBuilder.Predicate or(JpqlQueryBuilder.Predicate base, JpqlQueryBuilder.Predicate predicate) {
		return base.or(predicate);
	}

	/**
	 * Finalizes the given {@link Predicate} and applies the given sort. Delegates to {@link #buildQuery(Sort)} and hands
	 * it the current {@link JpqlQueryBuilder.Predicate}.
	 */
	@Override
	protected final String complete(JpqlQueryBuilder.@Nullable Predicate predicate, Sort sort) {

		JpqlQueryBuilder.AbstractJpqlQuery query = createQuery(predicate, sort);
		return query.render();
	}

	protected JpqlQueryBuilder.AbstractJpqlQuery createQuery(JpqlQueryBuilder.@Nullable Predicate predicate, Sort sort) {

		JpqlQueryBuilder.Select query = buildQuery(sort);

		if (predicate != null) {
			return query.where(predicate);
		}

		return query;
	}

	/**
	 * Template method to build a query stub using the given {@link Sort}.
	 *
	 * @param sort
	 * @return
	 */
	protected JpqlQueryBuilder.Select buildQuery(Sort sort) {

		JpqlQueryBuilder.Select select = doSelect(sort);

		if (tree.isDelete() || tree.isCountProjection()) {
			return select;
		}

		if (sort.isSorted()) {

			for (Sort.Order order : sort) {

				JpqlQueryBuilder.Expression expression;
				QueryUtils.checkSortExpression(order);

				try {
					expression = JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
							PropertyPath.from(order.getProperty(), entityType.getJavaType()));
				} catch (PropertyReferenceException e) {

					if (order instanceof JpaSort.JpaOrder jpaOrder && jpaOrder.isUnsafe()) {
						expression = JpqlQueryBuilder.expression(order.getProperty());
					} else {
						throw e;
					}
				}

				if (order.isIgnoreCase()) {
					expression = JpqlQueryBuilder.function(templates.getIgnoreCaseOperator(), expression);
				}

				select.orderBy(JpqlQueryBuilder.orderBy(expression, order));
			}
		} else {

			if (searchQuery) {

				DistanceFunction distanceFunction = DISTANCE_FUNCTIONS.get(provider.getScoringFunction());
				if (distanceFunction != null) {
					select
							.orderBy(JpqlQueryBuilder.orderBy(JpqlQueryBuilder.expression("distance"), distanceFunction.direction()));
				}
			}
		}

		return select;
	}

	private JpqlQueryBuilder.Select doSelect(Sort sort) {

		JpqlQueryBuilder.SelectStep selectStep = JpqlQueryBuilder.selectFrom(entity);

		if (tree.isDelete()) {
			return selectStep.entity();
		}

		if (tree.isDistinct()) {
			selectStep = selectStep.distinct();
		}

		if (returnedType.needsCustomConstruction()) {

			Collection<String> requiredSelection = null;
			if (returnedType.getReturnedType().getPackageName().startsWith("java.util")
					|| returnedType.getReturnedType().getPackageName().startsWith("jakarta.persistence")) {
				requiredSelection = metamodel.managedType(returnedType.getDomainType()).getAttributes().stream()
						.map(Attribute::getName).collect(Collectors.toList());
			} else {
				requiredSelection = getRequiredSelection(sort, returnedType);
			}

			List<JpqlQueryBuilder.Expression> paths = new ArrayList<>(requiredSelection.size());
			for (String selection : requiredSelection) {
				paths.add(JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
						PropertyPath.from(selection, returnedType.getDomainType()), true).as(selection));
			}

			JpqlQueryBuilder.Expression distance = null;
			if (searchQuery) {
				distance = getDistanceExpression();
			}

			if (useTupleQuery()) {

				if (searchQuery) {
					paths.add((distance != null ? distance : JpqlQueryBuilder.literal(0)).as("distance"));
				}
				return selectStep.select(paths);
			} else {

				JpqlQueryBuilder.ConstructorExpression expression = new JpqlQueryBuilder.ConstructorExpression(
						returnedType.getReturnedType().getName(), new JpqlQueryBuilder.Multiselect(entity, paths));

				List<JpqlQueryBuilder.Expression> selection = new ArrayList<>(2);
				selection.add(expression);

				if (searchQuery) {
					selection.add((distance != null ? distance : JpqlQueryBuilder.literal(0)).as("distance"));
				}

				return selectStep.select(selection);
			}
		}

		if (searchQuery) {

			JpqlQueryBuilder.Expression distance = getDistanceExpression();

			if (distance != null) {
				return selectStep.select(new JpqlQueryBuilder.Multiselect(entity,
						Arrays.asList(new JpqlQueryBuilder.EntitySelection(entity), distance.as("distance"))));
			}
		}

		if (tree.isExistsProjection()) {

			if (entityType.hasSingleIdAttribute()) {

				SingularAttribute<?, ?> id = entityType.getId(entityType.getIdType().getJavaType());
				JpqlQueryBuilder.PathExpression path = JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
						PropertyPath.from(id.getName(), returnedType.getDomainType()), true);
				return selectStep.select(List.of(JpqlQueryBuilder.unaliased(path)));

			} else {

				List<JpqlQueryBuilder.Expression> paths = entityType.getIdClassAttributes().stream()//
						.map(it -> JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
								PropertyPath.from(it.getName(), returnedType.getDomainType()), true))
						.map(JpqlQueryBuilder::unaliased)
						.toList();
				return selectStep.select(paths);
			}
		}

		if (tree.isCountProjection()) {
			return selectStep.count();
		} else {
			return selectStep.entity();
		}
	}

	private JpqlQueryBuilder.@Nullable Expression getDistanceExpression() {

		DistanceFunction distanceFunction = DISTANCE_FUNCTIONS.get(provider.getScoringFunction());

		if (distanceFunction != null) {
			JpqlQueryBuilder.PathExpression pas = JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
					getVectorPath(), true);
			return JpqlQueryBuilder.function(distanceFunction.distanceFunction(), pas,
					placeholder(provider.getVectorBinding()));
		}

		return null;
	}

	PropertyPath getVectorPath() {

		for (PartTree.OrPart parts : tree) {
			for (Part part : parts) {
				if (part.getType() == NEAR || part.getType() == WITHIN) {
					return part.getProperty();
				}
			}
		}

		throw new IllegalStateException("No vector path found");
	}

	Collection<String> getRequiredSelection(Sort sort, ReturnedType returnedType) {
		return returnedType.getInputProperties();
	}

	JpqlQueryBuilder.Expression placeholder(ParameterBinding binding) {

		if (useNamedParameters && binding.hasName()) {
			return JpqlQueryBuilder.parameter(ParameterPlaceholder.named(binding.getRequiredName()));
		}

		return JpqlQueryBuilder.parameter(ParameterPlaceholder.indexed(binding.getRequiredPosition()));
	}

	/**
	 * Creates a {@link Predicate} from the given {@link Part}.
	 *
	 * @param part
	 * @return
	 */
	private JpqlQueryBuilder.Predicate toPredicate(Part part) {
		return new PredicateBuilder(part, similarityNormalizer).build();
	}

	/**
	 * Simple builder to contain logic to create JPA {@link Predicate}s from {@link Part}s.
	 *
	 * @author Phil Webb
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 */
	private class PredicateBuilder {

		private final Part part;
		private final SimilarityNormalizer normalizer;

		/**
		 * Creates a new {@link PredicateBuilder} for the given {@link Part}.
		 *
		 * @param part must not be {@literal null}.
		 * @param normalizer must not be {@literal null}.
		 */
		public PredicateBuilder(Part part, SimilarityNormalizer normalizer) {

			this.part = part;
			this.normalizer = normalizer;
		}

		/**
		 * Builds a JPA {@link Predicate} from the underlying {@link Part}.
		 *
		 * @return
		 */
		public JpqlQueryBuilder.Predicate build() {

			PropertyPath property = part.getProperty();
			Type type = part.getType();

			JpqlQueryBuilder.PathExpression pas = JpqlUtils.toExpressionRecursively(metamodel, entity, entityType, property);
			JpqlQueryBuilder.WhereStep where = JpqlQueryBuilder.where(pas);
			JpqlQueryBuilder.WhereStep whereIgnoreCase = JpqlQueryBuilder.where(potentiallyIgnoreCase(pas));

			switch (type) {
				case BETWEEN:
					PartTreeParameterBinding first = provider.next(part);
					ParameterBinding second = provider.next(part);
					return where.between(placeholder(first), placeholder(second));
				case AFTER:
				case GREATER_THAN:
					return where.gt(placeholder(provider.next(part)));
				case GREATER_THAN_EQUAL:
					return where.gte(placeholder(provider.next(part)));
				case BEFORE:
				case LESS_THAN:
					return where.lt(placeholder(provider.next(part)));
				case LESS_THAN_EQUAL:
					return where.lte(placeholder(provider.next(part)));
				case IS_NULL:
					return where.isNull();
				case IS_NOT_NULL:
					return where.isNotNull();
				case NOT_IN:
					return whereIgnoreCase.notIn(placeholder(provider.next(part, Collection.class)));
				case IN:
					return whereIgnoreCase.in(placeholder(provider.next(part, Collection.class)));
				case STARTING_WITH:
				case ENDING_WITH:
				case CONTAINING:
				case NOT_CONTAINING:

					if (property.getLeafProperty().isCollection()) {

						if (!property.hasNext()) {
							where = JpqlQueryBuilder.where(entity, property);
						}

						return type.equals(NOT_CONTAINING) ? where.notMemberOf(placeholder(provider.next(part)))
								: where.memberOf(placeholder(provider.next(part)));
					}

				case LIKE:
				case NOT_LIKE:

					PartTreeParameterBinding parameter = provider.next(part, String.class);
					JpqlQueryBuilder.Expression parameterExpression = potentiallyIgnoreCase(part.getProperty().getLeafProperty(),
							placeholder(parameter));

					// Predicate like = builder.like(propertyExpression, parameterExpression, escape.getEscapeCharacter());
					String escapeChar = Character.toString(escape.getEscapeCharacter());
					return type.equals(NOT_LIKE) || type.equals(NOT_CONTAINING)
							? whereIgnoreCase.notLike(parameterExpression, escapeChar)
							: whereIgnoreCase.like(parameterExpression, escapeChar);
				case TRUE:
					return where.isTrue();
				case FALSE:
					return where.isFalse();
				case SIMPLE_PROPERTY:
				case NEGATING_SIMPLE_PROPERTY:

					PartTreeParameterBinding simple = provider.next(part);

					if (simple.isIsNullParameter()) {
						return type.equals(SIMPLE_PROPERTY) ? where.isNull() : where.isNotNull();
					}

					JpqlQueryBuilder.Expression expression = potentiallyIgnoreCase(property.getLeafProperty(),
							placeholder(simple));
					return type.equals(SIMPLE_PROPERTY) ? whereIgnoreCase.eq(expression) : whereIgnoreCase.neq(expression);
				case IS_EMPTY:
				case IS_NOT_EMPTY:

					if (!property.getLeafProperty().isCollection()) {
						throw new IllegalArgumentException("IsEmpty / IsNotEmpty can only be used on collection properties");
					}

					if (!property.hasNext()) {
						where = JpqlQueryBuilder.where(entity, property);
					}

					return type.equals(IS_NOT_EMPTY) ? where.isNotEmpty() : where.isEmpty();
				case WITHIN:
				case NEAR:
					PartTreeParameterBinding vector = provider.next(part);
					PartTreeParameterBinding within = provider.next(part);

					if (within.getValue() instanceof Range<?> r) {

						Range<Score> range = (Range<Score>) r;

						if (range.getUpperBound().isBounded() || range.getUpperBound().isBounded()) {

							Range.Bound<Score> lower = range.getLowerBound();
							Range.Bound<Score> upper = range.getUpperBound();

							String distanceFunction = getDistanceFunction(provider.getScoringFunction());
							JpqlQueryBuilder.Expression distance = JpqlQueryBuilder.function(distanceFunction, pas,
									placeholder(vector));

							JpqlQueryBuilder.Predicate lowerPredicate = null;
							JpqlQueryBuilder.Predicate upperPredicate = null;

							// Score is a distance function, you typically want less when you specify a lower boundary,
							// therefore lower and upper predicates are inverted.
							if (lower.isBounded()) {
								JpqlQueryBuilder.Expression distanceValue = placeholder(provider.lower(within, normalizer));
								lowerPredicate = getUpperPredicate(lower.isInclusive(), distance, distanceValue);
							}

							if (upper.isBounded()) {
								JpqlQueryBuilder.Expression distanceValue = placeholder(provider.upper(within, normalizer));
								upperPredicate = getLowerPredicate(upper.isInclusive(), distance, distanceValue);
							}

							if (lowerPredicate != null && upperPredicate != null) {
								return lowerPredicate.and(upperPredicate);
							} else if (lowerPredicate != null) {
								return lowerPredicate;
							} else if (upperPredicate != null) {
								return upperPredicate;
							}
						}
					}

					if (within.getValue() instanceof Score score) {

						String distanceFunction = getDistanceFunction(score.getFunction());
						JpqlQueryBuilder.Expression distanceValue = placeholder(provider.normalize(within, normalizer));
						JpqlQueryBuilder.Expression distance = JpqlQueryBuilder.function(distanceFunction, pas,
								placeholder(vector));

						return getUpperPredicate(true, distance, distanceValue);
					}

					throw new InvalidDataAccessApiUsageException(
							"Near/Within keywords must be used with a Score or Range<Score> type");
				default:
					throw new IllegalArgumentException("Unsupported keyword " + type);
			}
		}

		private JpqlQueryBuilder.Predicate getLowerPredicate(boolean inclusive, JpqlQueryBuilder.Expression lhs,
				JpqlQueryBuilder.Expression distance) {
			return doLower(inclusive, lhs, distance);
		}

		private JpqlQueryBuilder.Predicate getUpperPredicate(boolean inclusive, JpqlQueryBuilder.Expression lhs,
				JpqlQueryBuilder.Expression distance) {
			return doUpper(inclusive, lhs, distance);
		}

		private static JpqlQueryBuilder.Predicate doLower(boolean inclusive, JpqlQueryBuilder.Expression lhs,
				JpqlQueryBuilder.Expression distance) {
			return inclusive ? JpqlQueryBuilder.where(lhs).gte(distance) : JpqlQueryBuilder.where(lhs).gt(distance);
		}

		private static JpqlQueryBuilder.Predicate doUpper(boolean inclusive, JpqlQueryBuilder.Expression lhs,
				JpqlQueryBuilder.Expression distance) {
			return inclusive ? JpqlQueryBuilder.where(lhs).lte(distance) : JpqlQueryBuilder.where(lhs).lt(distance);
		}

		private static String getDistanceFunction(ScoringFunction scoringFunction) {

			DistanceFunction distanceFunction = JpaQueryCreator.DISTANCE_FUNCTIONS.get(scoringFunction);

			if (distanceFunction == null) {
				throw new IllegalArgumentException(
						"Unsupported ScoringFunction: %s. Make sure to declare a supported ScoringFunction when creating Score/Similarity instances."
								.formatted(scoringFunction.getName()));
			}

			return distanceFunction.distanceFunction();
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 *
		 * @param path must not be {@literal null}.
		 * @return
		 */
		private <T> JpqlQueryBuilder.Expression potentiallyIgnoreCase(JpqlQueryBuilder.Origin source, PropertyPath path) {
			return potentiallyIgnoreCase(path.getLeafProperty(), JpqlQueryBuilder.expression(source, path));
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 *
		 * @param path must not be {@literal null}.
		 * @return
		 */
		private <T> JpqlQueryBuilder.Expression potentiallyIgnoreCase(JpqlQueryBuilder.PathExpression path) {
			return potentiallyIgnoreCase(path.getPropertyPath().getLeafProperty(), path);
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 *
		 * @return
		 */
		private JpqlQueryBuilder.Expression potentiallyIgnoreCase(PropertyPath path,
				JpqlQueryBuilder.Expression expressionValue) {

			switch (part.shouldIgnoreCase()) {

				case ALWAYS:

					Assert.isTrue(canUpperCase(path),
							() -> "Unable to ignore case of %s types, the property '%s' must reference a String"
									.formatted(path.getType().getName(), part.getProperty().getSegment()));
					return JpqlQueryBuilder.function(templates.getIgnoreCaseOperator(), expressionValue);

				case WHEN_POSSIBLE:

					if (canUpperCase(path)) {
						return JpqlQueryBuilder.function(templates.getIgnoreCaseOperator(), expressionValue);
					}

				case NEVER:
				default:

					return expressionValue;
			}
		}

		private boolean canUpperCase(PropertyPath path) {
			return String.class.equals(path.getType());
		}
	}

}
