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
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;

import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.domain.JpaSort;
import org.springframework.data.jpa.repository.query.JpqlQueryBuilder.ParameterPlaceholder;
import org.springframework.data.jpa.repository.query.ParameterBinding.PartTreeParameterBinding;
import org.springframework.data.jpa.repository.support.JpqlQueryTemplates;
import org.springframework.data.mapping.PropertyPath;
import org.springframework.data.mapping.PropertyReferenceException;
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
 */
public class JpaQueryCreator extends AbstractQueryCreator<String, JpqlQueryBuilder.Predicate> implements JpqlQueryCreator {

	private final ReturnedType returnedType;
	private final ParameterMetadataProvider provider;
	private final JpqlQueryTemplates templates;
	private final PartTree tree;
	private final EscapeCharacter escape;
	private final EntityType<?> entityType;
	private final JpqlQueryBuilder.Entity entity;
	private final Metamodel metamodel;

	/**
	 * Create a new {@link JpaQueryCreator}.
	 *
	 * @param tree must not be {@literal null}.
	 * @param type must not be {@literal null}.
	 * @param templates must not be {@literal null}.
	 * @param provider must not be {@literal null}.
	 * @param em must not be {@literal null}.
	 */
	public JpaQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider,
			JpqlQueryTemplates templates, EntityManager em) {

		this(tree, type, provider, templates, em.getMetamodel());
	}

	public JpaQueryCreator(PartTree tree, ReturnedType type, ParameterMetadataProvider provider,
		JpqlQueryTemplates templates, Metamodel metamodel) {

		super(tree);
		this.tree = tree;
		this.returnedType = type;
		this.provider = provider;
		this.templates = templates;
		this.escape = provider.getEscape();
		this.entityType = metamodel.entity(type.getDomainType());
		this.entity = JpqlQueryBuilder.entity(returnedType.getDomainType());
		this.metamodel = metamodel;
	}

	Bindable<?> getFrom() {
		return entityType;
	}

	JpqlQueryBuilder.Entity getEntity() {
		return entity;
	}

	public boolean useTupleQuery() {
		return returnedType.needsCustomConstruction() && returnedType.getReturnedType().isInterface();
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

			List<JpqlQueryBuilder.PathExpression> paths = new ArrayList<>(requiredSelection.size());
			for (String selection : requiredSelection) {
				paths.add(JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
						PropertyPath.from(selection, returnedType.getDomainType()), true));
			}

			if (useTupleQuery()) {

				return selectStep.select(paths);
			} else {
				return selectStep.instantiate(returnedType.getReturnedType(), paths);
			}
		}

		if (tree.isExistsProjection()) {

			if (entityType.hasSingleIdAttribute()) {

				SingularAttribute<?, ?> id = entityType.getId(entityType.getIdType().getJavaType());
				return selectStep.select(JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
						PropertyPath.from(id.getName(), returnedType.getDomainType()), true));

			} else {

				List<JpqlQueryBuilder.PathExpression> paths = entityType.getIdClassAttributes().stream()//
						.map(it -> JpqlUtils.toExpressionRecursively(metamodel, entity, entityType,
								PropertyPath.from(it.getName(), returnedType.getDomainType()), true))
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

	Collection<String> getRequiredSelection(Sort sort, ReturnedType returnedType) {
		return returnedType.getInputProperties();
	}

	JpqlQueryBuilder.Expression placeholder(ParameterBinding binding) {
		return placeholder(binding.getRequiredPosition());
	}

	JpqlQueryBuilder.Expression placeholder(int position) {
		return JpqlQueryBuilder.parameter(ParameterPlaceholder.indexed(position));
	}

	/**
	 * Creates a {@link Predicate} from the given {@link Part}.
	 *
	 * @param part
	 * @return
	 */
	private JpqlQueryBuilder.Predicate toPredicate(Part part) {
		return new PredicateBuilder(part).build();
	}

	/**
	 * Simple builder to contain logic to create JPA {@link Predicate}s from {@link Part}s.
	 *
	 * @author Phil Webb
	 * @author Oliver Gierke
	 */
	private class PredicateBuilder {

		private final Part part;

		/**
		 * Creates a new {@link PredicateBuilder} for the given {@link Part}.
		 *
		 * @param part must not be {@literal null}.
		 */
		public PredicateBuilder(Part part) {

			Assert.notNull(part, "Part must not be null");

			this.part = part;
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
						where = JpqlQueryBuilder.where(entity, property);

						return type.equals(NOT_CONTAINING) ? where.notMemberOf(placeholder(provider.next(part)))
								: where.memberOf(placeholder(provider.next(part)));
					}

				case LIKE:
				case NOT_LIKE:

					PartTreeParameterBinding parameter = provider.next(part, String.class);
					JpqlQueryBuilder.Expression parameterExpression = potentiallyIgnoreCase(part.getProperty(),
							placeholder(parameter));
					// Predicate like = builder.like(propertyExpression, parameterExpression, escape.getEscapeCharacter());
					String escapeChar = Character.toString(escape.getEscapeCharacter());
					return

					type.equals(NOT_LIKE) || type.equals(NOT_CONTAINING)
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

					JpqlQueryBuilder.Expression expression = potentiallyIgnoreCase(property, placeholder(simple));
					return type.equals(SIMPLE_PROPERTY) ? whereIgnoreCase.eq(expression) : whereIgnoreCase.neq(expression);
				case IS_EMPTY:
				case IS_NOT_EMPTY:

					if (!property.getLeafProperty().isCollection()) {
						throw new IllegalArgumentException("IsEmpty / IsNotEmpty can only be used on collection properties");
					}

					where = JpqlQueryBuilder.where(entity, property);
					return type.equals(IS_NOT_EMPTY) ? where.isNotEmpty() : where.isEmpty();

				default:
					throw new IllegalArgumentException("Unsupported keyword " + type);
			}
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 *
		 * @param path must not be {@literal null}.
		 * @return
		 */
		private <T> JpqlQueryBuilder.Expression potentiallyIgnoreCase(JpqlQueryBuilder.Origin source, PropertyPath path) {
			return potentiallyIgnoreCase(path, JpqlQueryBuilder.expression(source, path));
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 *
		 * @param path must not be {@literal null}.
		 * @return
		 */
		private <T> JpqlQueryBuilder.Expression potentiallyIgnoreCase(JpqlQueryBuilder.PathExpression path) {
			return potentiallyIgnoreCase(path.getPropertyPath(), path);
		}

		/**
		 * Applies an {@code UPPERCASE} conversion to the given {@link Expression} in case the underlying {@link Part}
		 * requires ignoring case.
		 *
		 * @return
		 */
		private <T> JpqlQueryBuilder.Expression potentiallyIgnoreCase(PropertyPath path,
				JpqlQueryBuilder.Expression expressionValue) {

			switch (part.shouldIgnoreCase()) {

				case ALWAYS:

					Assert.isTrue(canUpperCase(path), "Unable to ignore case of " + path.getType().getName()
							+ " types, the property '" + part.getProperty().getSegment() + "' must reference a String");
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
